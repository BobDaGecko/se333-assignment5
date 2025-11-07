package org.example;

import org.example.Amazon.*;
import org.example.Amazon.Cost.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Amazon shopping cart system. These tests use real database connections
 * and test how multiple components work together.
 */
class AmazonIntegrationTest {

    private Database database;
    private ShoppingCartAdaptor shoppingCart;
    private Amazon amazon;

    @BeforeEach
    void setUp() {
        // Initialize real database connection
        database = new Database();
        database.resetDatabase(); // Reset database before each test

        // Create real shopping cart adapter with database
        shoppingCart = new ShoppingCartAdaptor(database);

        // Create Amazon instance with real pricing rules
        List<PriceRule> rules =
                List.of(new RegularCost(), new DeliveryPrice(), new ExtraCostForElectronics());
        amazon = new Amazon(shoppingCart, rules);
    }

    @AfterEach
    void tearDown() {
        // Clean up database after each test
        database.close();
    }

    // ===== Specification-Based Integration Tests =====

    @Test
    @DisplayName("specification-based")
    void shouldCalculateTotalPriceWithNoItems() {
        // Given: empty shopping cart
        // When: calculating total
        double total = amazon.calculate();

        // Then: should return 0
        assertThat(total).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void shouldAddItemAndCalculateTotalPrice() {
        // Given: an item is added to cart
        Item item = new Item(ItemType.OTHER, "Book", 2, 15.0);
        amazon.addToCart(item);

        // When: calculating total
        double total = amazon.calculate();

        // Then: should calculate item cost + delivery (1-3 items = $5)
        // Regular cost: 2 * 15 = 30
        // Delivery: 5 (for 1-3 items)
        // Electronic: 0
        assertThat(total).isEqualTo(35.0);
    }

    @Test
    @DisplayName("specification-based")
    void shouldAddMultipleItemsAndPersistToDatabase() {
        // Given: multiple items added
        Item item1 = new Item(ItemType.OTHER, "Book", 1, 20.0);
        Item item2 = new Item(ItemType.OTHER, "Magazine", 3, 5.0);
        Item item3 = new Item(ItemType.ELECTRONIC, "Laptop", 1, 1000.0);

        amazon.addToCart(item1);
        amazon.addToCart(item2);
        amazon.addToCart(item3);

        // When: retrieving items from database
        List<Item> items = shoppingCart.getItems();

        // Then: all items should be persisted
        assertThat(items).hasSize(3);
        assertThat(items).extracting(Item::getName).containsExactlyInAnyOrder("Book", "Magazine",
                "Laptop");
    }

    @Test
    @DisplayName("specification-based")
    void shouldCalculatePriceWithElectronicItem() {
        // Given: shopping cart with electronic item
        Item laptop = new Item(ItemType.ELECTRONIC, "Laptop", 1, 800.0);
        amazon.addToCart(laptop);

        // When: calculating total
        double total = amazon.calculate();

        // Then: should include electronic surcharge
        // Regular cost: 1 * 800 = 800
        // Delivery: 5 (for 1-3 items)
        // Electronic surcharge: 7.50
        assertThat(total).isEqualTo(812.5);
    }

    @Test
    @DisplayName("specification-based")
    void shouldApplyCorrectDeliveryPriceForFourToTenItems() {
        // Given: 5 items in cart
        for (int i = 0; i < 5; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item" + i, 1, 10.0));
        }

        // When: calculating total
        double total = amazon.calculate();

        // Then: delivery should be $12.5 for 4-10 items
        // Regular cost: 5 * 10 = 50
        // Delivery: 12.5
        assertThat(total).isEqualTo(62.5);
    }

    @Test
    @DisplayName("specification-based")
    void shouldApplyCorrectDeliveryPriceForMoreThanTenItems() {
        // Given: 12 items in cart
        for (int i = 0; i < 12; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item" + i, 1, 5.0));
        }

        // When: calculating total
        double total = amazon.calculate();

        // Then: delivery should be $20 for more than 10 items
        // Regular cost: 12 * 5 = 60
        // Delivery: 20
        assertThat(total).isEqualTo(80.0);
    }

    // ===== Structural-Based Integration Tests =====

    @Test
    @DisplayName("structural-based")
    void shouldPersistItemToDatabase() {
        // Given: an item
        Item item = new Item(ItemType.OTHER, "Notebook", 5, 3.5);

        // When: adding to cart
        amazon.addToCart(item);

        // Then: item should be in database
        List<Item> items = shoppingCart.getItems();
        assertThat(items).hasSize(1);
        Item retrieved = items.get(0);
        assertThat(retrieved.getName()).isEqualTo("Notebook");
        assertThat(retrieved.getQuantity()).isEqualTo(5);
        assertThat(retrieved.getPricePerUnit()).isEqualTo(3.5);
        assertThat(retrieved.getType()).isEqualTo(ItemType.OTHER);
    }

    @Test
    @DisplayName("structural-based")
    void shouldApplyAllPricingRulesInSequence() {
        // Given: cart with multiple items including electronics
        Item phone = new Item(ItemType.ELECTRONIC, "Phone", 2, 500.0);
        Item charger = new Item(ItemType.ELECTRONIC, "Charger", 1, 25.0);

        amazon.addToCart(phone);
        amazon.addToCart(charger);

        // When: calculating total
        double total = amazon.calculate();

        // Then: all three pricing rules should be applied
        // RegularCost: (2 * 500) + (1 * 25) = 1025
        // DeliveryPrice: 5 (2 items)
        // ExtraCostForElectronics: 7.50 (has electronics)
        assertThat(total).isEqualTo(1037.5);
    }

    @Test
    @DisplayName("structural-based")
    void shouldHandleDatabaseResetBetweenTests() {
        // Given: items added in a previous operation
        amazon.addToCart(new Item(ItemType.OTHER, "Test", 1, 10.0));

        // When: database is reset and new instance created
        database.resetDatabase();

        // Then: cart should be empty
        List<Item> items = shoppingCart.getItems();
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("structural-based")
    void shouldCalculateCorrectPriceWithMixedItemTypes() {
        // Given: mix of electronic and other items
        Item book = new Item(ItemType.OTHER, "Book", 3, 15.0);
        Item tablet = new Item(ItemType.ELECTRONIC, "Tablet", 1, 300.0);
        Item pen = new Item(ItemType.OTHER, "Pen", 10, 2.0);

        amazon.addToCart(book);
        amazon.addToCart(tablet);
        amazon.addToCart(pen);

        // When: calculating total
        double total = amazon.calculate();

        // Then: should correctly sum all components
        // RegularCost: (3*15) + (1*300) + (10*2) = 45 + 300 + 20 = 365
        // DeliveryPrice: 5 (3 items)
        // ExtraCostForElectronics: 7.50
        assertThat(total).isEqualTo(377.5);
    }

    @Test
    @DisplayName("structural-based")
    void shouldHandleHighQuantityItems() {
        // Given: items with high quantity
        Item bulk = new Item(ItemType.OTHER, "Bulk Item", 100, 0.5);

        amazon.addToCart(bulk);

        // When: calculating total
        double total = amazon.calculate();

        // Then: should handle large quantity calculation
        // RegularCost: 100 * 0.5 = 50
        // DeliveryPrice: 5 (1 item)
        assertThat(total).isEqualTo(55.0);
    }

    @Test
    @DisplayName("structural-based")
    void shouldNotChargeElectronicSurchargeForNonElectronicItems() {
        // Given: only non-electronic items
        Item book1 = new Item(ItemType.OTHER, "Fiction Book", 1, 12.0);
        Item book2 = new Item(ItemType.OTHER, "Science Book", 1, 18.0);

        amazon.addToCart(book1);
        amazon.addToCart(book2);

        // When: calculating total
        double total = amazon.calculate();

        // Then: should not include electronic surcharge
        // RegularCost: 12 + 18 = 30
        // DeliveryPrice: 5 (2 items)
        // ExtraCostForElectronics: 0
        assertThat(total).isEqualTo(35.0);
    }

    @Test
    @DisplayName("structural-based")
    void shouldHandleBoundaryOfDeliveryPricing() {
        // Given: exactly 3 items (upper bound of first tier)
        for (int i = 0; i < 3; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item" + i, 1, 10.0));
        }

        // When: calculating total
        double total = amazon.calculate();

        // Then: should use $5 delivery for 1-3 items
        assertThat(total).isEqualTo(35.0); // 30 + 5

        // Given: add one more item to reach 4 (lower bound of second tier)
        database.resetDatabase();
        shoppingCart = new ShoppingCartAdaptor(database);
        amazon = new Amazon(shoppingCart, List.of(new RegularCost(), new DeliveryPrice()));

        for (int i = 0; i < 4; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item" + i, 1, 10.0));
        }

        // When: calculating total
        total = amazon.calculate();

        // Then: should use $12.5 delivery for 4-10 items
        assertThat(total).isEqualTo(52.5); // 40 + 12.5
    }

    @Test
    @DisplayName("structural-based")
    void shouldIntegrateAllComponentsCorrectly() {
        // Given: complex scenario with all features
        Item laptop = new Item(ItemType.ELECTRONIC, "Gaming Laptop", 1, 1500.0);
        Item mouse = new Item(ItemType.ELECTRONIC, "Mouse", 2, 25.0);
        Item keyboard = new Item(ItemType.ELECTRONIC, "Keyboard", 1, 75.0);
        Item book = new Item(ItemType.OTHER, "Programming Book", 3, 40.0);

        amazon.addToCart(laptop);
        amazon.addToCart(mouse);
        amazon.addToCart(keyboard);
        amazon.addToCart(book);

        // When: calculating total and retrieving items
        double total = amazon.calculate();
        List<Item> items = shoppingCart.getItems();

        // Then: all components should work together correctly
        assertThat(items).hasSize(4);
        // RegularCost: 1500 + (2*25) + 75 + (3*40) = 1500 + 50 + 75 + 120 = 1745
        // DeliveryPrice: 5 (4 items -> actually 12.5 for 4-10 items)
        // ExtraCostForElectronics: 7.50
        assertThat(total).isEqualTo(1765.0); // 1745 + 12.5 + 7.5
    }
}
