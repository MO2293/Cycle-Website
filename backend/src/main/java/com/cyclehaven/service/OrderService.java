package com.cyclehaven.service;

import com.cyclehaven.dto.cart.AddToCartRequest;
import com.cyclehaven.dto.order.CardDetails;
import com.cyclehaven.dto.order.CheckoutRequest;
import com.cyclehaven.dto.order.GuestCheckoutRequest;
import com.cyclehaven.dto.order.OrderResponse;
import com.cyclehaven.entity.CartItem;
import com.cyclehaven.entity.Item;
import com.cyclehaven.entity.Order;
import com.cyclehaven.entity.OrderItem;
import com.cyclehaven.entity.OrderStatus;
import com.cyclehaven.entity.Role;
import com.cyclehaven.entity.User;
import com.cyclehaven.exception.BadRequestException;
import com.cyclehaven.exception.ConflictException;
import com.cyclehaven.exception.NotFoundException;
import com.cyclehaven.repository.CartItemRepository;
import com.cyclehaven.repository.ItemRepository;
import com.cyclehaven.repository.OrderRepository;
import com.cyclehaven.repository.UserRepository;
import com.cyclehaven.util.CardUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout and order history.
 *
 * <p>Checkout is the most safety-critical operation in the application, and the
 * original got three things wrong that this class fixes:
 *
 * <p><b>1. The client decided the price.</b> {@code OrderServlet} read
 * {@code totalAmount} from a request parameter. Every total here is summed from
 * prices read out of the database during the transaction.
 *
 * <p><b>2. Nothing was atomic.</b> The original looped over cart lines inserting
 * an order row and then updating stock, with a {@code break} on the first
 * failure — so a partial failure left some orders placed, some stock decremented,
 * and the cart untouched. Everything here happens in one transaction: either the
 * whole order commits or none of it does.
 *
 * <p><b>3. Card data was persisted.</b> Full card numbers and CVVs were written
 * to the database in plaintext. Only a masked last-4 and brand survive now.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public OrderService(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    /**
     * Checkout for a signed-in customer, using their server-side cart.
     */
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("User", userId));

        List<CartItem> cartItems = cartItemRepository.findByUserIdWithItems(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItem cartItem : cartItems) {
            quantities.merge(cartItem.getItem().getId(), cartItem.getQuantity(), Integer::sum);
        }

        Order order = placeOrder(user, null, request.shippingAddress(), quantities, request.card());

        // The cart only empties once the order has actually succeeded.
        cartItemRepository.deleteByUserId(userId);

        return OrderResponse.from(order);
    }

    /**
     * Checkout for a visitor with no account. The lines come from the request
     * rather than a stored cart, but prices and stock are still resolved here.
     */
    @Transactional
    public OrderResponse guestCheckout(GuestCheckoutRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (AddToCartRequest line : request.items()) {
            quantities.merge(line.itemId(), line.quantity(), Integer::sum);
        }

        Order order = placeOrder(
                null, request.email().trim().toLowerCase(),
                request.shippingAddress(), quantities, request.card());

        return OrderResponse.from(order);
    }

    /**
     * The shared checkout path.
     *
     * <p>Order of operations is deliberate: verify stock, compute the true total,
     * then attempt payment, then commit the stock change. Taking payment before
     * confirming stock would mean charging for something that cannot be shipped.
     */
    private Order placeOrder(
            User user,
            String guestEmail,
            String shippingAddress,
            Map<Long, Integer> quantities,
            CardDetails card) {

        if (quantities.isEmpty()) {
            throw new BadRequestException("An order must contain at least one item");
        }

        List<OrderItem> lines = new ArrayList<>();
        List<Item> toDecrement = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long itemId = entry.getKey();
            int quantity = entry.getValue();

            if (quantity < 1) {
                throw new BadRequestException("Quantity must be at least 1");
            }

            // Row-locked read: prevents two concurrent checkouts from both
            // claiming the last unit in stock.
            Item item = itemRepository.findByIdForUpdate(itemId)
                    .orElseThrow(() -> NotFoundException.of("Item", itemId));

            if (item.getQuantity() < quantity) {
                throw new ConflictException(String.format(
                        "Only %d of %s left in stock", item.getQuantity(), item.getName()));
            }

            // The price comes from the database row, inside this transaction —
            // never from anything the client sent.
            BigDecimal unitPrice = item.getPrice();
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));

            lines.add(new OrderItem(item, quantity, unitPrice));
            item.setQuantity(item.getQuantity() - quantity);
            toDecrement.add(item);
        }

        PaymentService.PaymentResult payment = paymentService.authorise(card, total);
        if (!payment.approved()) {
            // Throwing rolls the transaction back, so the stock decrements above
            // are undone automatically. Nothing partial survives a declined card.
            throw new BadRequestException("Payment failed: " + payment.declineReason());
        }

        itemRepository.saveAll(toDecrement);

        Order order = new Order();
        order.setOrderRef(UUID.randomUUID().toString());
        order.setUser(user);
        order.setGuestEmail(guestEmail);
        order.setShippingAddress(shippingAddress.trim());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PAID);
        // Everything that survives of the card.
        order.setCardBrand(CardUtils.detectBrand(card.number()));
        order.setCardLast4(CardUtils.lastFour(card.number()));
        lines.forEach(order::addItem);

        Order saved = orderRepository.save(order);
        log.info("Order {} placed, total={}, lines={}",
                saved.getOrderRef(), saved.getTotalAmount(), lines.size());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdWithItems(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * Fetches one order. A customer may only read their own; an admin may read any.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderRef, Long requesterId, Role requesterRole) {
        Order order = orderRepository.findByOrderRefWithItems(orderRef)
                .orElseThrow(() -> new NotFoundException("Order " + orderRef + " not found"));

        boolean isOwner = order.getUser() != null
                && order.getUser().getId().equals(requesterId);

        if (requesterRole != Role.ADMIN && !isOwner) {
            // Deliberately "not found" rather than "forbidden": confirming that an
            // order reference exists would let someone probe for valid references.
            throw new NotFoundException("Order " + orderRef + " not found");
        }

        return OrderResponse.from(order);
    }

    /** Admin sales view: every order, optionally filtered by customer email. */
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrders(String email, int page, int size) {
        String normalised = (email == null || email.isBlank())
                ? null
                : email.trim().toLowerCase();

        return orderRepository
                .searchByCustomerEmail(
                        normalised,
                        PageRequest.of(
                                Math.max(page, 0),
                                Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public SalesSummary getSalesSummary() {
        return new SalesSummary(
                orderRepository.totalRevenue(),
                orderRepository.countByStatus(OrderStatus.PAID));
    }

    /** Headline numbers for the admin dashboard. */
    public record SalesSummary(BigDecimal totalRevenue, long paidOrderCount) {
    }
}
