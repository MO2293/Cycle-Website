package com.cyclehaven.repository;

import com.cyclehaven.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Loads a user's cart with items eagerly joined, so rendering the cart is a
     * single query rather than one extra query per line (N+1).
     */
    @Query("SELECT c FROM CartItem c JOIN FETCH c.item WHERE c.user.id = :userId")
    List<CartItem> findByUserIdWithItems(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndItemId(Long userId, Long itemId);

    @Transactional
    void deleteByUserIdAndItemId(Long userId, Long itemId);

    @Transactional
    void deleteByUserId(Long userId);
}
