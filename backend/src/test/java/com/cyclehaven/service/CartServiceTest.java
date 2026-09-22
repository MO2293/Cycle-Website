package com.cyclehaven.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cyclehaven.dto.cart.AddToCartRequest;
import com.cyclehaven.dto.cart.CartResponse;
import com.cyclehaven.entity.CartItem;
import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import com.cyclehaven.entity.User;
import com.cyclehaven.repository.CartItemRepository;
import com.cyclehaven.repository.ItemRepository;
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
 * Cart rules.
 *
 * <p>These are unit tests: the repositories are mocked, so no database and no
 * Spring context is involved and they run in milliseconds. That is only possible
 * because the logic lives in a service rather than inside a servlet's
 * {@code doPost} — the original's cart logic could not be tested at all without
 * faking an {@code HttpServletRequest}.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long ITEM_ID = 42L;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Item item;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setName("Test Customer");
        user.setEmail("customer@example.com");

        item = new Item();
        item.setId(ITEM_ID);
        item.setName("Summit Trail 29");
        item.setCategory(Category.MOUNTAIN);
        item.setDescription("Trail hardtail");
        item.setModel("ST-29");
        item.setColour("Green");
        item.setPrice(new BigDecimal("1849.50"));
        item.setQuantity(3);
    }

    @Test
    @DisplayName("Adding to an empty cart creates a line with the requested quantity")
    void addsNewLine() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserIdWithItems(USER_ID))
                .thenReturn(List.of(new CartItem(user, item, 2)));

        CartResponse response = cartService.addItem(USER_ID, new AddToCartRequest(ITEM_ID, 2));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);

        assertThat(response.lines()).hasSize(1);
        assertThat(response.totalUnits()).isEqualTo(2);
        assertThat(response.notices()).isEmpty();
    }

    @Test
    @DisplayName("Adding the same item again increments rather than creating a second line")
    void incrementsExistingLine() {
        CartItem existing = new CartItem(user, item, 1);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.findByUserIdWithItems(USER_ID)).thenReturn(List.of(existing));

        cartService.addItem(USER_ID, new AddToCartRequest(ITEM_ID, 2));

        // 1 already there + 2 more = 3, not a duplicate line of 2.
        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Quantity is clamped to available stock, and the clamp is reported")
    void clampsToAvailableStock() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserIdWithItems(USER_ID))
                .thenReturn(List.of(new CartItem(user, item, 3)));

        // Only 3 in stock; ask for 99.
        CartResponse response = cartService.addItem(USER_ID, new AddToCartRequest(ITEM_ID, 99));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualTo(3);

        // The original clamped silently. The user must be told.
        assertThat(response.notices())
                .anyMatch(notice -> notice.contains("Only 3") && notice.contains("Summit Trail 29"));
    }

    @Test
    @DisplayName("An out-of-stock item is not added at all")
    void refusesOutOfStockItem() {
        item.setQuantity(0);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdWithItems(USER_ID)).thenReturn(List.of());

        CartResponse response = cartService.addItem(USER_ID, new AddToCartRequest(ITEM_ID, 1));

        verify(cartItemRepository, never()).save(any());
        assertThat(response.lines()).isEmpty();
        assertThat(response.notices()).anyMatch(notice -> notice.contains("out of stock"));
    }

    @Test
    @DisplayName("Subtotal is computed from database prices, not from client input")
    void computesSubtotalFromStoredPrices() {
        when(cartItemRepository.findByUserIdWithItems(USER_ID))
                .thenReturn(List.of(new CartItem(user, item, 2)));

        CartResponse response = cartService.getCart(USER_ID);

        // 1849.50 * 2, taken from the Item row.
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("3699.00"));
        assertThat(response.lines().get(0).unitPrice())
                .isEqualByComparingTo(new BigDecimal("1849.50"));
    }
}
