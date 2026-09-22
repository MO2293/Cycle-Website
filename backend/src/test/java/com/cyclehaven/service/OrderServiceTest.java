package com.cyclehaven.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cyclehaven.dto.order.CardDetails;
import com.cyclehaven.dto.order.CheckoutRequest;
import com.cyclehaven.dto.order.OrderResponse;
import com.cyclehaven.entity.CartItem;
import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import com.cyclehaven.entity.Order;
import com.cyclehaven.entity.OrderStatus;
import com.cyclehaven.entity.User;
import com.cyclehaven.exception.BadRequestException;
import com.cyclehaven.exception.ConflictException;
import com.cyclehaven.repository.CartItemRepository;
import com.cyclehaven.repository.ItemRepository;
import com.cyclehaven.repository.OrderRepository;
import com.cyclehaven.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Checkout rules — the highest-stakes logic in the application.
 *
 * <p>Each test here pins down one of the defects in the original
 * {@code OrderServlet}, so a future change cannot quietly reintroduce them.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long ITEM_ID = 42L;

    @Mock private OrderRepository orderRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks private OrderService orderService;

    private User user;
    private Item item;
    private CheckoutRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setName("Test Customer");
        user.setEmail("customer@example.com");

        item = new Item();
        item.setId(ITEM_ID);
        item.setName("Velocity RS Carbon");
        item.setCategory(Category.ROAD);
        item.setDescription("Carbon race frame");
        item.setModel("RS-Carbon");
        item.setColour("Red");
        item.setPrice(new BigDecimal("1000.00"));
        item.setQuantity(5);

        request = new CheckoutRequest(
                "123 Bloor St W, Toronto",
                new CardDetails("4242424242424242", "123", 12, 2030));
    }

    private void cartContains(int quantity) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserIdWithItems(USER_ID))
                .thenReturn(List.of(new CartItem(user, item, quantity)));
        when(itemRepository.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.of(item));
    }

    private void paymentApproved() {
        when(paymentService.authorise(any(), any()))
                .thenReturn(PaymentService.PaymentResult.approved("SIM-TEST"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Order total is computed from database prices, not supplied by the client")
    void computesTotalFromDatabasePrices() {
        cartContains(3);
        paymentApproved();

        OrderResponse response = orderService.checkout(USER_ID, request);

        // 1000.00 * 3. Note the request carries no price field at all — the
        // original read totalAmount straight off the request.
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));

        ArgumentCaptor<BigDecimal> charged = ArgumentCaptor.forClass(BigDecimal.class);
        verify(paymentService).authorise(any(), charged.capture());
        assertThat(charged.getValue()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Stock is decremented by the quantity ordered")
    void decrementsStock() {
        cartContains(2);
        paymentApproved();

        orderService.checkout(USER_ID, request);

        assertThat(item.getQuantity()).isEqualTo(3); // 5 - 2
        verify(itemRepository).saveAll(any());
    }

    @Test
    @DisplayName("The cart is emptied only after the order succeeds")
    void clearsCartAfterSuccess() {
        cartContains(1);
        paymentApproved();

        orderService.checkout(USER_ID, request);

        verify(cartItemRepository).deleteByUserId(USER_ID);
    }

    @Test
    @DisplayName("The line price is captured at purchase time")
    void capturesPriceAtPurchase() {
        cartContains(1);
        paymentApproved();

        OrderResponse response = orderService.checkout(USER_ID, request);

        // Recorded on the order line, so a later price change cannot rewrite
        // this order's history the way the original sales report did.
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).priceAtPurchase())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Only the card's last four digits and brand are retained")
    void storesOnlyMaskedCardDetails() {
        cartContains(1);
        paymentApproved();

        OrderResponse response = orderService.checkout(USER_ID, request);

        assertThat(response.cardLast4()).isEqualTo("4242");
        assertThat(response.cardBrand()).isEqualTo("Visa");
        assertThat(response.status()).isEqualTo(OrderStatus.PAID);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getCardLast4()).hasSize(4);
        // Nothing anywhere on the persisted order holds the full number.
        assertThat(saved.getValue().getCardLast4()).isNotEqualTo("4242424242424242");
    }

    @Test
    @DisplayName("Ordering more than is in stock is refused")
    void refusesWhenStockInsufficient() {
        item.setQuantity(1);
        cartContains(5);

        assertThatThrownBy(() -> orderService.checkout(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only 1");

        // Stock must be verified before any payment is attempted — charging for
        // something that cannot ship is worse than declining early.
        verify(paymentService, never()).authorise(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("A declined card places no order and leaves the cart intact")
    void declinedPaymentPlacesNoOrder() {
        cartContains(2);
        when(paymentService.authorise(any(), any()))
                .thenReturn(PaymentService.PaymentResult.declined("Card was declined by the issuer"));

        assertThatThrownBy(() -> orderService.checkout(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("declined");

        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(any());
        // The in-memory decrement is rolled back by the transaction in production;
        // what matters here is that nothing was persisted.
        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Checking out an empty cart is rejected")
    void rejectsEmptyCart() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserIdWithItems(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");

        verify(paymentService, never()).authorise(any(), any());
    }
}
