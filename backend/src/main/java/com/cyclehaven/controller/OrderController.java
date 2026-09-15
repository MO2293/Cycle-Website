package com.cyclehaven.controller;

import com.cyclehaven.dto.order.CheckoutRequest;
import com.cyclehaven.dto.order.GuestCheckoutRequest;
import com.cyclehaven.dto.order.OrderResponse;
import com.cyclehaven.entity.Role;
import com.cyclehaven.security.CustomUserDetails;
import com.cyclehaven.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Checkout and order history")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order from the signed-in user's cart")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(principal.getId(), request));
    }

    @PostMapping("/guest")
    @Operation(summary = "Place an order without an account")
    public ResponseEntity<OrderResponse> guestCheckout(
            @Valid @RequestBody GuestCheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.guestCheckout(request));
    }

    @GetMapping("/me")
    @Operation(summary = "List the signed-in user's orders")
    public ResponseEntity<List<OrderResponse>> myOrders(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.getMyOrders(principal.getId()));
    }

    @GetMapping("/{orderRef}")
    @Operation(summary = "Fetch one order (its owner, or any admin)")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String orderRef) {
        Role role = principal.getUser().getRole();
        return ResponseEntity.ok(orderService.getOrder(orderRef, principal.getId(), role));
    }
}
