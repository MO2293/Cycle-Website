package com.cyclehaven.controller;

import com.cyclehaven.dto.PageResponse;
import com.cyclehaven.dto.item.ItemRequest;
import com.cyclehaven.dto.item.ItemResponse;
import com.cyclehaven.entity.Category;
import com.cyclehaven.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;

/**
 * Product catalogue.
 *
 * <p>Resource-shaped URLs replace the old action-shaped ones: browsing is
 * {@code GET /api/items} rather than {@code indexFilter.jsp}, and creating is
 * {@code POST /api/items} rather than {@code GET /ItemServlet?...}. The HTTP verb
 * now carries the intent, which also means the safe operations really are safe —
 * the original created products on a GET, so a crawler or a browser prefetch
 * could add inventory.
 *
 * <p>Write endpoints are admin-only; the rules live in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/items")
@Tag(name = "Items", description = "Browse the catalogue; admin product management")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    @Operation(summary = "Browse products with filters, search, sorting and pagination")
    public ResponseEntity<PageResponse<ItemResponse>> browse(
            @RequestParam(required = false) List<Category> category,
            @RequestParam(required = false) List<String> colour,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(PageResponse.from(itemService.search(
                category, colour, search, minPrice, maxPrice, inStockOnly, sort, page, size)));
    }

    @GetMapping("/categories")
    @Operation(summary = "List the category values the catalogue supports")
    public ResponseEntity<Category[]> categories() {
        return ResponseEntity.ok(Category.values());
    }

    @GetMapping("/colours")
    @Operation(summary = "List the distinct colours currently in the catalogue")
    public ResponseEntity<List<String>> colours() {
        return ResponseEntity.ok(itemService.getAvailableColours());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single product")
    public ResponseEntity<ItemResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Serve a product image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        ItemService.ImagePayload payload = itemService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                // Product images rarely change, so let the browser keep them.
                // Without this the catalogue re-downloads every thumbnail on each
                // page view, which is what made the original storefront slow.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(payload.data());
    }

    @PostMapping
    @Operation(summary = "Create a product (admin only)")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product (admin only)")
    public ResponseEntity<ItemResponse> update(
            @PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    /**
     * Image upload is its own endpoint rather than part of the create/update body.
     * That keeps product details as plain JSON — far easier to consume and test —
     * instead of forcing every client to build a multipart request just to change
     * a price.
     */
    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace a product image (admin only)")
    public ResponseEntity<ItemResponse> uploadImage(
            @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(itemService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product (admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        // 204: succeeded, and there is deliberately nothing to return.
        return ResponseEntity.noContent().build();
    }
}
