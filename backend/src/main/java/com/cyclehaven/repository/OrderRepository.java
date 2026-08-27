package com.cyclehaven.repository;

import com.cyclehaven.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.item
            WHERE o.user.id = :userId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.item
            LEFT JOIN FETCH o.user
            WHERE o.orderRef = :orderRef
            """)
    Optional<Order> findByOrderRefWithItems(@Param("orderRef") String orderRef);

    Optional<Order> findByOrderRef(String orderRef);

    /**
     * Admin sales view: every order, optionally narrowed by customer email.
     *
     * <p>The join to {@code o.user} must be an explicit LEFT JOIN — an implicit
     * join through {@code o.user.email} would generate an inner join and silently
     * hide every guest order, which is exactly the kind of bug that makes a sales
     * report quietly wrong.
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.user u
            WHERE :email IS NULL
               OR LOWER(COALESCE(u.email, o.guestEmail)) LIKE CONCAT('%', :email, '%')
            """)
    Page<Order> searchByCustomerEmail(@Param("email") String email, Pageable pageable);

    /** Total revenue across all paid orders, for the admin dashboard. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = com.cyclehaven.entity.OrderStatus.PAID")
    java.math.BigDecimal totalRevenue();

    long countByStatus(com.cyclehaven.entity.OrderStatus status);
}
