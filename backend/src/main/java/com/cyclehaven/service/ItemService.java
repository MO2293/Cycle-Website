package com.cyclehaven.service;

import com.cyclehaven.dto.item.ItemRequest;
import com.cyclehaven.dto.item.ItemResponse;
import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import com.cyclehaven.exception.BadRequestException;
import com.cyclehaven.exception.NotFoundException;
import com.cyclehaven.repository.ItemRepository;
import com.cyclehaven.repository.ItemSpecifications;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Catalogue browsing and admin product management.
 */
@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    /** Caps how much a single request can ask for, so no client can demand 1,000,000 rows. */
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * The storefront's single browse endpoint: filters, search, sorting and
     * pagination all resolved in one SQL query.
     *
     * <p>The original loaded the entire {@code items} table on every page view and
     * then filtered and sorted it in Java inside the JSP.
     */
    @Transactional(readOnly = true)
    public Page<ItemResponse> search(
            Collection<Category> categories,
            Collection<String> colours,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            String sort,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                resolveSort(sort));

        return itemRepository
                .findAll(ItemSpecifications.build(
                        categories, colours, search, inStockOnly, minPrice, maxPrice), pageable)
                .map(ItemResponse::from);
    }

    /**
     * Maps a small set of known sort keys to actual sort orders.
     *
     * <p>Deliberately an allow-list rather than passing the client's string
     * straight to {@code Sort.by(...)}: an arbitrary value would let a caller sort
     * by any field on the entity, including ones that shouldn't be exposed, and an
     * invalid property name would throw a 500 at query time.
     */
    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        return switch (sort.toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> throw new BadRequestException(
                    "Unknown sort '" + sort + "'. Valid values: name_asc, name_desc, "
                            + "price_asc, price_desc, newest");
        };
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(Long id) {
        return ItemResponse.from(findItem(id));
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableColours() {
        return itemRepository.findDistinctColours();
    }

    /**
     * Returns the raw image bytes.
     *
     * <p>Must run inside a transaction: {@code imageData} is a LAZY column, so
     * touching it after the session closed would throw LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public ImagePayload getImage(Long id) {
        Item item = findItem(id);
        if (item.getImageData() == null || item.getImageData().length == 0) {
            throw new NotFoundException("Item " + id + " has no image");
        }
        String contentType = item.getImageContentType() != null
                ? item.getImageContentType()
                : "application/octet-stream";
        return new ImagePayload(item.getImageData(), contentType);
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        Item item = new Item();
        apply(item, request);
        Item saved = itemRepository.save(item);
        log.info("Admin created item id={} name={}", saved.getId(), saved.getName());
        return ItemResponse.from(saved);
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {
        Item item = findItem(id);
        apply(item, request);
        log.info("Admin updated item id={}", id);
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        Item item = findItem(id);
        itemRepository.delete(item);
        log.info("Admin deleted item id={}", id);
    }

    /**
     * Stores an uploaded product image.
     *
     * <p>The content type is checked against an allow-list. The original accepted
     * whatever bytes arrived and later streamed them back with no content type at
     * all, which means an uploaded HTML or SVG file could be served from the
     * application's own origin — a stored-XSS vector.
     */
    @Transactional
    public ItemResponse uploadImage(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported image type. Allowed: PNG, JPEG, WebP, GIF");
        }

        Item item = findItem(id);
        try {
            item.setImageData(file.getBytes());
            item.setImageContentType(contentType.toLowerCase());
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded file");
        }
        return ItemResponse.from(itemRepository.save(item));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Item", id));
    }

    private void apply(Item item, ItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(request.category());
        item.setDescription(request.description().trim());
        item.setModel(request.model().trim());
        item.setPrice(request.price());
        item.setColour(request.colour().trim());
        item.setQuantity(request.quantity());
    }

    /** Image bytes plus the content type to serve them with. */
    public record ImagePayload(byte[] data, String contentType) {
    }
}
