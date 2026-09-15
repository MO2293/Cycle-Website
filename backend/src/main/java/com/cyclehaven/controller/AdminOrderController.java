package com.cyclehaven.controller;

import com.cyclehaven.dto.PageResponse;
import com.cyclehaven.dto.order.OrderResponse;
import com.cyclehaven.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The admin sales view — the replacement for {@code adminSale.jsp}.
 *
 * <p>Everything under {@code /api/admin/**} requires the ADMIN role, enforced in
 * {@code SecurityConfig}. The original's admin pages were ordinary files under the
 * webapp root, reachable by anyone who knew the URL.
 */
@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin — Sales", description = "Sales reporting (admin only)")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "All orders, optionally filtered by customer email")
    public ResponseEntity<PageResponse<OrderResponse>> searchOrders(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                PageResponse.from(orderService.searchOrders(email, page, size)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Total revenue and paid order count")
    public ResponseEntity<OrderService.SalesSummary> summary() {
        return ResponseEntity.ok(orderService.getSalesSummary());
    }
}
