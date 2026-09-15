package com.cyclehaven.service;

import com.cyclehaven.dto.cart.AddToCartRequest;
import com.cyclehaven.dto.cart.CartLineResponse;
import com.cyclehaven.dto.cart.CartResponse;
import com.cyclehaven.entity.CartItem;
import com.cyclehaven.entity.Item;
import com.cyclehaven.entity.User;
import com.cyclehaven.exception.NotFoundException;
import com.cyclehaven.repository.CartItemRepository;
import com.cyclehaven.repository.ItemRepository;
import com.cyclehaven.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signed-in user's shopping cart.
 *
 * <p>Two rules drive everything here, and both were broken in the original:
 *
 * <p><b>A cart line may never exceed available stock.</b> The original clamped
 * quantities silently, so asking for 10 of something with 3 left gave you 3 with
 * no explanation. Clamping still happens — refusing the whole request would be
 * worse — but the cart now reports what it changed, so the UI can say so.
 *
 * <p><b>Adding and removing are different operations.</b> The original
 * {@code cartServlet} handled both in one method with this branch:
 * <pre>
 *   q1 += c1;
 *   if (c1 == 0)       addProductToCart(...);
 *   else if (q1 == c1) removeCartItem(...);   // fires when quantity == 0
 * </pre>
 * Because {@code q1 += c1} runs first, passing quantity 0 makes {@code q1}
 * equal {@code c1}, and "add zero" silently deletes the line. Splitting add,
 * update and remove into separate endpoints makes that impossible.
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public CartService(
            CartItemRepository cartItemRepository,
            ItemRepository itemRepository,
            UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return buildResponse(cartItemRepository.findByUserIdWithItems(userId), List.of());
    }

    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        List<String> notices = new ArrayList<>();
        addOrIncrement(userId, request.itemId(), request.quantity(), notices);
        return buildResponse(cartItemRepository.findByUserIdWithItems(userId), notices);
    }

    /**
     * Sets a line to an exact quantity, as a cart page's quantity box would.
     */
    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, int quantity) {
        List<String> notices = new ArrayList<>();

        CartItem cartItem = cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new NotFoundException("That item is not in your cart"));

        int allowed = clamp(cartItem.getItem(), quantity, notices);
        if (allowed == 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(allowed);
            cartItemRepository.save(cartItem);
        }

        return buildResponse(cartItemRepository.findByUserIdWithItems(userId), notices);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        cartItemRepository.deleteByUserIdAndItemId(userId, itemId);
        return buildResponse(cartItemRepository.findByUserIdWithItems(userId), List.of());
    }

    @Transactional
    public CartResponse clear(Long userId) {
        cartItemRepository.deleteByUserId(userId);
        return buildResponse(List.of(), List.of());
    }

    /**
     * Folds a guest's browser-held cart into their account cart at login.
     *
     * <p>Quantities are added to whatever is already saved, which is what users
     * expect: adding a bike as a guest and then signing in should not discard
     * either cart.
     */
    @Transactional
    public CartResponse merge(Long userId, List<AddToCartRequest> lines) {
        List<String> notices = new ArrayList<>();
        for (AddToCartRequest line : lines) {
            try {
                addOrIncrement(userId, line.itemId(), line.quantity(), notices);
            } catch (NotFoundException ex) {
                // A guest cart can reference a product an admin deleted in the
                // meantime. Skip it rather than failing the whole login.
                notices.add("An item in your guest cart is no longer available and was skipped");
            }
        }
        log.info("Merged {} guest cart line(s) for user id={}", lines.size(), userId);
        return buildResponse(cartItemRepository.findByUserIdWithItems(userId), notices);
    }

    private void addOrIncrement(Long userId, Long itemId, int quantity, List<String> notices) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> NotFoundException.of("Item", itemId));

        CartItem existing = cartItemRepository.findByUserIdAndItemId(userId, itemId).orElse(null);
        int current = existing != null ? existing.getQuantity() : 0;
        int allowed = clamp(item, current + quantity, notices);

        if (allowed == 0) {
            if (existing != null) {
                cartItemRepository.delete(existing);
            }
            return;
        }

        if (existing != null) {
            existing.setQuantity(allowed);
            cartItemRepository.save(existing);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> NotFoundException.of("User", userId));
            cartItemRepository.save(new CartItem(user, item, allowed));
        }
    }

    /**
     * Reduces a requested quantity to what is actually in stock, recording a
     * message whenever it had to.
     */
    private int clamp(Item item, int requested, List<String> notices) {
        if (item.getQuantity() <= 0) {
            notices.add(item.getName() + " is out of stock and was not added");
            return 0;
        }
        if (requested > item.getQuantity()) {
            notices.add(String.format(
                    "Only %d of %s %s in stock — quantity set to %d",
                    item.getQuantity(),
                    item.getName(),
                    item.getQuantity() == 1 ? "is" : "are",
                    item.getQuantity()));
            return item.getQuantity();
        }
        return Math.max(requested, 0);
    }

    private CartResponse buildResponse(List<CartItem> cartItems, List<String> notices) {
        List<CartLineResponse> lines = cartItems.stream()
                .sorted(Comparator.comparing(c -> c.getItem().getName()))
                .map(CartLineResponse::from)
                .toList();

        // Totals are computed here, from prices read out of the database — never
        // from anything the client sent.
        BigDecimal subtotal = lines.stream()
                .map(CartLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalUnits = lines.stream().mapToInt(CartLineResponse::quantity).sum();

        return new CartResponse(lines, lines.size(), totalUnits, subtotal, notices);
    }
}
