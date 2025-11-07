package org.example;

import org.example.Amazon.*;
import org.example.Amazon.Cost.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Amazon shopping cart system. These tests use mocks and stubs to isolate
 * individual components.
 */
class AmazonUnitTest {

    private ShoppingCart mockShoppingCart;

    @BeforeEach
    void setUp() {
        mockShoppingCart = mock(ShoppingCart.class);
    }

    // ===== Specification-Based Unit Tests =====

    @Test
    @DisplayName("specification-based")
    void shouldReturnZeroWhenNoRulesApplied() {
        // Given: Amazon with no pricing rules
        Amazon amazon = new Amazon(mockShoppingCart, List.of());
        when(mockShoppingCart.getItems()).thenReturn(List.of());

        // When: calculating price
        double total = amazon.calculate();

        // Then: should return 0
        assertThat(total).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void shouldApplySinglePricingRule() {
        // Given: Amazon with one pricing rule
        PriceRule mockRule = mock(PriceRule.class);
        when(mockRule.priceToAggregate(any())).thenReturn(50.0);

        Amazon amazon = new Amazon(mockShoppingCart, List.of(mockRule));
        when(mockShoppingCart.getItems()).thenReturn(List.of());

        // When: calculating price
        double total = amazon.calculate();

        // Then: should return the rule's price
        assertThat(total).isEqualTo(50.0);
    }

    @Test
    @DisplayName("specification-based")
    void shouldAggregateMultiplePricingRules() {
        // Given: Amazon with multiple pricing rules
        PriceRule rule1 = mock(PriceRule.class);
        PriceRule rule2 = mock(PriceRule.class);
        PriceRule rule3 = mock(PriceRule.class);

        when(rule1.priceToAggregate(any())).thenReturn(100.0);
        when(rule2.priceToAggregate(any())).thenReturn(25.5);
        when(rule3.priceToAggregate(any())).thenReturn(10.0);

        Amazon amazon = new Amazon(mockShoppingCart, List.of(rule1, rule2, rule3));
        when(mockShoppingCart.getItems()).thenReturn(List.of());

        // When: calculating price
        double total = amazon.calculate();

        // Then: should aggregate all rule prices
        assertThat(total).isEqualTo(135.5);
    }

    @Test
    @DisplayName("specification-based")
    void shouldAddItemToCart() {
        // Given: an item and Amazon instance
        Item item = new Item(ItemType.OTHER, "Book", 1, 15.0);
        Amazon amazon = new Amazon(mockShoppingCart, List.of());

        // When: adding item to cart
        amazon.addToCart(item);

        // Then: shopping cart should receive the item
        verify(mockShoppingCart).add(item);
    }

    // ===== Structural-Based Unit Tests =====

    @Test
    @DisplayName("structural-based")
    void shouldCallPriceToAggregateOnAllRules() {
        // Given: Amazon with multiple rules
        PriceRule rule1 = mock(PriceRule.class);
        PriceRule rule2 = mock(PriceRule.class);

        when(rule1.priceToAggregate(any())).thenReturn(20.0);
        when(rule2.priceToAggregate(any())).thenReturn(5.0);

        List<Item> items = List.of(new Item(ItemType.OTHER, "Test", 1, 10.0));
        when(mockShoppingCart.getItems()).thenReturn(items);

        Amazon amazon = new Amazon(mockShoppingCart, List.of(rule1, rule2));

        // When: calculating price
        amazon.calculate();

        // Then: both rules should be called with the items
        verify(rule1).priceToAggregate(items);
        verify(rule2).priceToAggregate(items);
    }

    @Test
    @DisplayName("structural-based")
    void shouldPassItemsFromCartToRules() {
        // Given: shopping cart with items
        List<Item> items = List.of(new Item(ItemType.OTHER, "Item1", 2, 10.0),
                new Item(ItemType.ELECTRONIC, "Item2", 1, 50.0));
        when(mockShoppingCart.getItems()).thenReturn(items);

        PriceRule mockRule = mock(PriceRule.class);
        when(mockRule.priceToAggregate(items)).thenReturn(100.0);

        Amazon amazon = new Amazon(mockShoppingCart, List.of(mockRule));

        // When: calculating price
        amazon.calculate();

        // Then: rule should receive the exact items list
        verify(mockRule).priceToAggregate(items);
    }

    @Test
    @DisplayName("structural-based")
    void shouldDelegateAddOperationToShoppingCart() {
        // Given: Amazon instance
        Amazon amazon = new Amazon(mockShoppingCart, List.of());
        Item item = new Item(ItemType.ELECTRONIC, "Laptop", 1, 1000.0);

        // When: adding item
        amazon.addToCart(item);

        // Then: should delegate to shopping cart's add method
        verify(mockShoppingCart, times(1)).add(item);
    }

    // ===== RegularCost Unit Tests =====

    @Test
    @DisplayName("specification-based")
    void regularCostShouldReturnZeroForEmptyCart() {
        // Given: empty cart
        RegularCost regularCost = new RegularCost();

        // When: calculating price
        double price = regularCost.priceToAggregate(List.of());

        // Then: should return 0
        assertThat(price).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void regularCostShouldCalculateSingleItemPrice() {
        // Given: cart with one item
        Item item = new Item(ItemType.OTHER, "Book", 3, 12.5);
        RegularCost regularCost = new RegularCost();

        // When: calculating price
        double price = regularCost.priceToAggregate(List.of(item));

        // Then: should return quantity * price per unit
        assertThat(price).isEqualTo(37.5); // 3 * 12.5
    }

    @Test
    @DisplayName("specification-based")
    void regularCostShouldSumMultipleItems() {
        // Given: cart with multiple items
        List<Item> items = List.of(new Item(ItemType.OTHER, "Book", 2, 10.0), // 20
                new Item(ItemType.OTHER, "Pen", 5, 2.0), // 10
                new Item(ItemType.ELECTRONIC, "Mouse", 1, 25.0) // 25
        );
        RegularCost regularCost = new RegularCost();

        // When: calculating price
        double price = regularCost.priceToAggregate(items);

        // Then: should sum all items
        assertThat(price).isEqualTo(55.0); // 20 + 10 + 25
    }

    // ===== DeliveryPrice Unit Tests =====

    @Test
    @DisplayName("specification-based")
    void deliveryPriceShouldReturnZeroForEmptyCart() {
        // Given: empty cart
        DeliveryPrice deliveryPrice = new DeliveryPrice();

        // When: calculating delivery
        double price = deliveryPrice.priceToAggregate(List.of());

        // Then: should return 0
        assertThat(price).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void deliveryPriceShouldReturn5ForOneToThreeItems() {
        // Given: carts with 1, 2, and 3 items
        DeliveryPrice deliveryPrice = new DeliveryPrice();

        // When/Then: 1 item
        assertThat(deliveryPrice.priceToAggregate(createItems(1))).isEqualTo(5.0);

        // When/Then: 2 items
        assertThat(deliveryPrice.priceToAggregate(createItems(2))).isEqualTo(5.0);

        // When/Then: 3 items
        assertThat(deliveryPrice.priceToAggregate(createItems(3))).isEqualTo(5.0);
    }

    @Test
    @DisplayName("specification-based")
    void deliveryPriceShouldReturn12Point5ForFourToTenItems() {
        // Given: carts with 4 and 10 items
        DeliveryPrice deliveryPrice = new DeliveryPrice();

        // When/Then: 4 items
        assertThat(deliveryPrice.priceToAggregate(createItems(4))).isEqualTo(12.5);

        // When/Then: 10 items
        assertThat(deliveryPrice.priceToAggregate(createItems(10))).isEqualTo(12.5);
    }

    @Test
    @DisplayName("specification-based")
    void deliveryPriceShouldReturn20ForMoreThanTenItems() {
        // Given: cart with more than 10 items
        DeliveryPrice deliveryPrice = new DeliveryPrice();

        // When/Then: 11 items
        assertThat(deliveryPrice.priceToAggregate(createItems(11))).isEqualTo(20.0);

        // When/Then: 50 items
        assertThat(deliveryPrice.priceToAggregate(createItems(50))).isEqualTo(20.0);
    }

    @Test
    @DisplayName("structural-based")
    void deliveryPriceShouldUseSizeOfItemsList() {
        // Given: delivery price instance
        DeliveryPrice deliveryPrice = new DeliveryPrice();
        List<Item> items = createItems(7);

        // When: calculating delivery
        double price = deliveryPrice.priceToAggregate(items);

        // Then: should check list size (7 items = 12.5)
        assertThat(price).isEqualTo(12.5);
    }

    // ===== ExtraCostForElectronics Unit Tests =====

    @Test
    @DisplayName("specification-based")
    void electronicCostShouldReturnZeroForEmptyCart() {
        // Given: empty cart
        ExtraCostForElectronics electronicCost = new ExtraCostForElectronics();

        // When: calculating cost
        double price = electronicCost.priceToAggregate(List.of());

        // Then: should return 0
        assertThat(price).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void electronicCostShouldReturnZeroForNonElectronicItems() {
        // Given: cart with only non-electronic items
        List<Item> items = List.of(new Item(ItemType.OTHER, "Book", 1, 10.0),
                new Item(ItemType.OTHER, "Pen", 2, 2.0));
        ExtraCostForElectronics electronicCost = new ExtraCostForElectronics();

        // When: calculating cost
        double price = electronicCost.priceToAggregate(items);

        // Then: should return 0
        assertThat(price).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void electronicCostShouldReturn7Point5WhenElectronicItemPresent() {
        // Given: cart with electronic item
        List<Item> items = List.of(new Item(ItemType.ELECTRONIC, "Laptop", 1, 1000.0));
        ExtraCostForElectronics electronicCost = new ExtraCostForElectronics();

        // When: calculating cost
        double price = electronicCost.priceToAggregate(items);

        // Then: should return 7.50
        assertThat(price).isEqualTo(7.50);
    }

    @Test
    @DisplayName("specification-based")
    void electronicCostShouldReturn7Point5EvenWithMultipleElectronicItems() {
        // Given: cart with multiple electronic items
        List<Item> items = List.of(new Item(ItemType.ELECTRONIC, "Laptop", 1, 1000.0),
                new Item(ItemType.ELECTRONIC, "Mouse", 2, 25.0),
                new Item(ItemType.ELECTRONIC, "Keyboard", 1, 75.0));
        ExtraCostForElectronics electronicCost = new ExtraCostForElectronics();

        // When: calculating cost
        double price = electronicCost.priceToAggregate(items);

        // Then: should return 7.50 (flat fee, not per item)
        assertThat(price).isEqualTo(7.50);
    }

    @Test
    @DisplayName("specification-based")
    void electronicCostShouldReturn7Point5ForMixedCart() {
        // Given: cart with both electronic and non-electronic items
        List<Item> items = List.of(new Item(ItemType.OTHER, "Book", 3, 15.0),
                new Item(ItemType.ELECTRONIC, "Tablet", 1, 300.0),
                new Item(ItemType.OTHER, "Pen", 5, 2.0));
        ExtraCostForElectronics electronicCost = new ExtraCostForElectronics();

        // When: calculating cost
        double price = electronicCost.priceToAggregate(items);

        // Then: should return 7.50
        assertThat(price).isEqualTo(7.50);
    }

    @Test
    @DisplayName("structural-based")
    void electronicCostShouldUseStreamToCheckForElectronics() {
        // Given: cart with electronic item
        List<Item> items = List.of(new Item(ItemType.ELECTRONIC, "Phone", 1, 500.0));
        ExtraCostForElectronics electronicCost = new ExtraCostForElectronics();

        // When: calculating cost
        double price = electronicCost.priceToAggregate(items);

        // Then: should detect electronic and return surcharge
        assertThat(price).isEqualTo(7.50);
    }

    // ===== Item Class Unit Tests =====

    @Test
    @DisplayName("specification-based")
    void itemShouldStoreAllProperties() {
        // Given: item properties
        ItemType type = ItemType.ELECTRONIC;
        String name = "Laptop";
        int quantity = 2;
        double pricePerUnit = 999.99;

        // When: creating item
        Item item = new Item(type, name, quantity, pricePerUnit);

        // Then: all properties should be accessible
        assertThat(item.getType()).isEqualTo(type);
        assertThat(item.getName()).isEqualTo(name);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getPricePerUnit()).isEqualTo(pricePerUnit);
    }

    @Test
    @DisplayName("structural-based")
    void itemGettersShouldReturnCorrectValues() {
        // Given: an item
        Item item = new Item(ItemType.OTHER, "Book", 5, 12.50);

        // When/Then: getters should return stored values
        assertThat(item.getType()).isEqualTo(ItemType.OTHER);
        assertThat(item.getName()).isEqualTo("Book");
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getPricePerUnit()).isEqualTo(12.50);
    }

    // ===== Helper Methods =====

    /**
     * Helper method to create a list of items for testing
     */
    private List<Item> createItems(int count) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new Item(ItemType.OTHER, "Item" + i, 1, 10.0));
        }
        return items;
    }
}
