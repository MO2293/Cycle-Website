package com.cyclehaven.repository;

import com.cyclehaven.entity.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Catalogue queries.
 *
 * <p>Filtering is composed with {@link ItemSpecifications} rather than one
 * @Query with nullable parameters: a JPQL {@code IN} clause cannot take a null
 * collection, and Specifications let each filter be omitted entirely from the
 * generated SQL when it isn't supplied.
 */
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Distinct colours present in the catalogue, for building the filter sidebar. */
    @Query("SELECT DISTINCT i.colour FROM Item i ORDER BY i.colour")
    List<String> findDistinctColours();
}
