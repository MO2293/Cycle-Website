package com.cyclehaven.repository;

import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable catalogue filters.
 *
 * <p>The original storefront fetched every product on every page load and then
 * filtered and sorted in Java inside the JSP — which also duplicated any product
 * matching two selected filters. Each filter here becomes a SQL predicate, and
 * is skipped entirely when the caller doesn't supply it.
 */
public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    /** Matches any of the given categories. No-op when the collection is null/empty. */
    public static Specification<Item> inCategories(Collection<Category> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("category").in(categories);
        };
    }

    /** Matches any of the given colours, case-insensitively. */
    public static Specification<Item> inColours(Collection<String> colours) {
        return (root, query, cb) -> {
            if (colours == null || colours.isEmpty()) {
                return cb.conjunction();
            }
            List<String> normalised = colours.stream()
                    .filter(c -> c != null && !c.isBlank())
                    .map(c -> c.toLowerCase(Locale.ROOT))
                    .toList();
            if (normalised.isEmpty()) {
                return cb.conjunction();
            }
            return cb.lower(root.get("colour")).in(normalised);
        };
    }

    /** Free-text search across name, description and model. */
    public static Specification<Item> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate byDescription = cb.like(cb.lower(root.get("description")), pattern);
            Predicate byModel = cb.like(cb.lower(root.get("model")), pattern);
            return cb.or(byName, byDescription, byModel);
        };
    }

    public static Specification<Item> inStockOnly(boolean inStockOnly) {
        return (root, query, cb) -> inStockOnly
                ? cb.greaterThan(root.get("quantity"), 0)
                : cb.conjunction();
    }

    public static Specification<Item> priceAtLeast(java.math.BigDecimal min) {
        return (root, query, cb) -> min == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Item> priceAtMost(java.math.BigDecimal max) {
        return (root, query, cb) -> max == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("price"), max);
    }

    /** Combines every supplied filter with AND. */
    public static Specification<Item> build(
            Collection<Category> categories,
            Collection<String> colours,
            String search,
            boolean inStockOnly,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice) {

        List<Specification<Item>> specs = new ArrayList<>();
        specs.add(inCategories(categories));
        specs.add(inColours(colours));
        specs.add(matchesSearch(search));
        specs.add(inStockOnly(inStockOnly));
        specs.add(priceAtLeast(minPrice));
        specs.add(priceAtMost(maxPrice));

        Specification<Item> combined = (root, query, cb) -> cb.conjunction();
        for (Specification<Item> spec : specs) {
            combined = combined.and(spec);
        }
        return combined;
    }
}
