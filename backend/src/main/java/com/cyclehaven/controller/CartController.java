package com.cyclehaven.controller;

import com.cyclehaven.dto.cart.AddToCartRequest;
import com.cyclehaven.dto.cart.CartResponse;
import com.cyclehaven.dto.cart.MergeCartRequest;
import com.cyclehaven.dto.cart.UpdateCartItemRequest;
import com.cyclehaven.security.CustomUserDetails;
import com.cyclehaven.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's cart.
 *
 * <p>Every endpoint here identifies the cart's owner from the authenticated
 * token, never from a parameter. The original took the cart owner straight off
 * the query string — {@code ./cartServlet?email=someone@example.com&id=5} — so
 * anyone could read or modify anyone else's cart by editing the URL. Guests hit
 * the same problem from the other side: they all shared one
 * {@code guest@cyclehaven.com} cart.
 *
 * <p>Guests now keep a cart in their own browser and POST it to
 * {@code /api/cart/merge} when they sign in.
 */
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "The signed-in user's shopping cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get the current user's cart")
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a quantity of a product to the cart")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getId(), request));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Set a cart line to an exact quantity")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(
                cartService.updateQuantity(principal.getId(), itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a line from the cart")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(principal.getId(), itemId));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<CartResponse> clear(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.clear(principal.getId()));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge a guest cart into the account cart after signing in")
    public ResponseEntity<CartResponse> merge(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody MergeCartRequest request) {
        return ResponseEntity.ok(cartService.merge(principal.getId(), request.lines()));
    }
}
