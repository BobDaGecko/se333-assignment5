package org.example;

import org.example.Barnes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BarnesAndNobleTest {

    private BookDatabase mockDatabase;
    private BuyBookProcess mockProcess;
    private BarnesAndNoble barnesAndNoble;

    @BeforeEach
    void setUp() {
        mockDatabase = mock(BookDatabase.class);
        mockProcess = mock(BuyBookProcess.class);
        barnesAndNoble = new BarnesAndNoble(mockDatabase, mockProcess);
    }

    // ===== Specification-Based Tests =====

    @Test
    @DisplayName("specification-based")
    void shouldReturnNullWhenOrderIsNull() {
        // Given: null order
        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(null);

        // Then: should return null
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("specification-based")
    void shouldCalculateTotalPriceForSingleBook() {
        // Given: a book with ISBN "123", price 20, quantity 10
        String isbn = "123";
        Book book = new Book(isbn, 20, 10);
        when(mockDatabase.findByISBN(isbn)).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put(isbn, 3); // Order 3 books

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: total price should be 3 * 20 = 60
        assertThat(result.getTotalPrice()).isEqualTo(60);
        assertThat(result.getUnavailable()).isEmpty();
    }

    @Test
    @DisplayName("specification-based")
    void shouldCalculateTotalPriceForMultipleBooks() {
        // Given: multiple books in database
        Book book1 = new Book("ISBN1", 10, 5);
        Book book2 = new Book("ISBN2", 20, 8);
        Book book3 = new Book("ISBN3", 15, 10);

        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(mockDatabase.findByISBN("ISBN2")).thenReturn(book2);
        when(mockDatabase.findByISBN("ISBN3")).thenReturn(book3);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 2); // 2 * 10 = 20
        order.put("ISBN2", 3); // 3 * 20 = 60
        order.put("ISBN3", 1); // 1 * 15 = 15

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: total price should be 20 + 60 + 15 = 95
        assertThat(result.getTotalPrice()).isEqualTo(95);
        assertThat(result.getUnavailable()).isEmpty();
    }

    @Test
    @DisplayName("specification-based")
    void shouldHandleInsufficientQuantity() {
        // Given: a book with limited quantity
        Book book = new Book("ISBN1", 25, 5); // Only 5 available
        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 10); // Request 10 books (5 more than available)

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: should charge for 5 books and mark 5 as unavailable
        assertThat(result.getTotalPrice()).isEqualTo(125); // 5 * 25
        assertThat(result.getUnavailable()).hasSize(1);
        assertThat(result.getUnavailable().get(book)).isEqualTo(5);
    }

    @Test
    @DisplayName("specification-based")
    void shouldHandleEmptyOrder() {
        // Given: empty order
        Map<String, Integer> order = new HashMap<>();

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: total price should be 0
        assertThat(result.getTotalPrice()).isEqualTo(0);
        assertThat(result.getUnavailable()).isEmpty();
    }

    @Test
    @DisplayName("specification-based")
    void shouldHandleZeroQuantityRequest() {
        // Given: a book in database
        Book book = new Book("ISBN1", 15, 10);
        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 0); // Request 0 books

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: total price should be 0
        assertThat(result.getTotalPrice()).isEqualTo(0);
    }

    // ===== Structural-Based Tests =====

    @Test
    @DisplayName("structural-based")
    void shouldCallBuyBookProcessWhenOrderingBooks() {
        // Given: a book in database
        Book book = new Book("ISBN1", 30, 10);
        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 5);

        // When: getting price for cart
        barnesAndNoble.getPriceForCart(order);

        // Then: buyBook should be called with correct arguments
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockProcess).buyBook(bookCaptor.capture(), quantityCaptor.capture());
        assertThat(bookCaptor.getValue()).isEqualTo(book);
        assertThat(quantityCaptor.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("structural-based")
    void shouldQueryDatabaseForEachISBNInOrder() {
        // Given: multiple books
        Book book1 = new Book("ISBN1", 10, 5);
        Book book2 = new Book("ISBN2", 20, 8);

        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(mockDatabase.findByISBN("ISBN2")).thenReturn(book2);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 2);
        order.put("ISBN2", 3);

        // When: getting price for cart
        barnesAndNoble.getPriceForCart(order);

        // Then: database should be queried for each ISBN
        verify(mockDatabase).findByISBN("ISBN1");
        verify(mockDatabase).findByISBN("ISBN2");
        verify(mockDatabase, times(2)).findByISBN(anyString());
    }

    @Test
    @DisplayName("structural-based")
    void shouldCallBuyBookWithReducedQuantityWhenInsufficientStock() {
        // Given: a book with limited quantity
        Book book = new Book("ISBN1", 40, 3); // Only 3 available
        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 7); // Request 7 books

        // When: getting price for cart
        barnesAndNoble.getPriceForCart(order);

        // Then: buyBook should be called with available quantity (3, not 7)
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockProcess).buyBook(any(Book.class), quantityCaptor.capture());
        assertThat(quantityCaptor.getValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("structural-based")
    void shouldAddUnavailableQuantityToPurchaseSummaryWhenStockInsufficient() {
        // Given: a book with limited quantity
        Book book = new Book("ISBN1", 50, 2); // Only 2 available
        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 8); // Request 8 books

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: 6 books should be marked as unavailable (8 - 2)
        assertThat(result.getUnavailable()).containsKey(book);
        assertThat(result.getUnavailable().get(book)).isEqualTo(6);
    }

    @Test
    @DisplayName("structural-based")
    void shouldAccumulatePricesForMultipleItems() {
        // Given: multiple books with different prices
        Book book1 = new Book("ISBN1", 100, 10);
        Book book2 = new Book("ISBN2", 200, 10);

        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(mockDatabase.findByISBN("ISBN2")).thenReturn(book2);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 1); // 100
        order.put("ISBN2", 2); // 400

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: total should be accumulated correctly
        assertThat(result.getTotalPrice()).isEqualTo(500);
    }

    @Test
    @DisplayName("structural-based")
    void shouldHandleMixOfAvailableAndUnavailableBooks() {
        // Given: multiple books, some with sufficient stock, some without
        Book book1 = new Book("ISBN1", 10, 10); // Sufficient
        Book book2 = new Book("ISBN2", 20, 2); // Insufficient
        Book book3 = new Book("ISBN3", 30, 5); // Insufficient

        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(mockDatabase.findByISBN("ISBN2")).thenReturn(book2);
        when(mockDatabase.findByISBN("ISBN3")).thenReturn(book3);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 5); // 5 * 10 = 50 (all available)
        order.put("ISBN2", 5); // 2 * 20 = 40 (3 unavailable)
        order.put("ISBN3", 10); // 5 * 30 = 150 (5 unavailable)

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: should handle both available and unavailable items correctly
        assertThat(result.getTotalPrice()).isEqualTo(240); // 50 + 40 + 150
        assertThat(result.getUnavailable()).hasSize(2);
        assertThat(result.getUnavailable().get(book2)).isEqualTo(3);
        assertThat(result.getUnavailable().get(book3)).isEqualTo(5);
    }

    @Test
    @DisplayName("structural-based")
    void shouldNotAddToUnavailableWhenSufficientStock() {
        // Given: a book with sufficient quantity
        Book book = new Book("ISBN1", 25, 10);
        when(mockDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 5); // Request less than available

        // When: getting price for cart
        PurchaseSummary result = barnesAndNoble.getPriceForCart(order);

        // Then: unavailable map should be empty
        assertThat(result.getUnavailable()).isEmpty();
    }
}
