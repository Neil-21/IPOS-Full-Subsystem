package tests.service;

import main.db.DatabaseManager;
import main.model.Product;
import main.service.CatalogueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogueServiceTest {

    private CatalogueService catalogueService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        catalogueService = new CatalogueService();
    }

    // Expected: returns all seeded catalogue products.
    @Test
    void testGetAllProducts_ReturnsExpectedNumberOfProducts() {
        List<Product> products = catalogueService.getAllProducts();

        assertNotNull(products);
        assertEquals(5, products.size(), "Catalogue should return 5 products.");
    }

    // Expected: returned catalogue includes the known seeded product IDs.
    @Test
    void testGetAllProducts_ContainsKnownProductIds() {
        List<Product> products = catalogueService.getAllProducts();
        Set<String> ids = products.stream().map(Product::getId).collect(Collectors.toSet());

        assertTrue(ids.contains("PARA001"));
        assertTrue(ids.contains("IBU002"));
        assertTrue(ids.contains("VIT003"));
        assertTrue(ids.contains("ALL004"));
        assertTrue(ids.contains("BAND005"));
    }

    // Expected: each product has valid basic data (id/name/category/price/stock).
    @Test
    void testGetAllProducts_ProductFieldsAreValid() {
        List<Product> products = catalogueService.getAllProducts();

        for (Product product : products) {
            assertNotNull(product.getId());
            assertFalse(product.getId().trim().isEmpty());
            assertNotNull(product.getName());
            assertFalse(product.getName().trim().isEmpty());
            assertNotNull(product.getCategory());
            assertFalse(product.getCategory().trim().isEmpty());
            assertTrue(product.getPrice() >= 0, "Price should not be negative.");
            assertTrue(product.getStock() >= 0, "Stock should not be negative.");
        }
    }

    // Expected: each call returns an independent list instance.
    @Test
    void testGetAllProducts_ReturnsIndependentListEachCall() {
        List<Product> firstCall = catalogueService.getAllProducts();
        List<Product> secondCall = catalogueService.getAllProducts();

        firstCall.clear();

        assertEquals(5, secondCall.size(), "Second call should not be affected by first call mutations.");
    }

    // Expected: reduceStock returns true and decreases stock by requested quantity.
    @Test
    void testReduceStock_ValidQuantity_ReturnsTrueAndUpdatesStock() throws SQLException {
        setStock("PARA001", 120);
        int before = getStock("PARA001");

        boolean reduced = catalogueService.reduceStock("PARA001", 3);
        int after = getStock("PARA001");

        assertTrue(reduced);
        assertEquals(before - 3, after);
    }

    // Expected: reduceStock returns false and leaves stock unchanged when quantity exceeds stock.
    @Test
    void testReduceStock_InsufficientStock_ReturnsFalseAndLeavesStock() throws SQLException {
        setStock("PARA001", 2);
        int before = getStock("PARA001");

        boolean reduced = catalogueService.reduceStock("PARA001", 3);
        int after = getStock("PARA001");

        assertFalse(reduced);
        assertEquals(before, after);
    }

    // Expected: reduceStock returns false for a missing product ID.
    @Test
    void testReduceStock_UnknownProduct_ReturnsFalse() {
        boolean reduced = catalogueService.reduceStock("UNKNOWN-PRODUCT", 1);
        assertFalse(reduced);
    }

    // Expected: zero-quantity reduction succeeds for an existing product and keeps stock unchanged.
    @Test
    void testReduceStock_ZeroQuantity_ReturnsTrueAndNoStockChange() throws SQLException {
        setStock("PARA001", 120);
        int before = getStock("PARA001");

        boolean reduced = catalogueService.reduceStock("PARA001", 0);
        int after = getStock("PARA001");

        assertTrue(reduced);
        assertEquals(before, after);
    }

    private int getStock(String productId) throws SQLException {
        String sql = "SELECT stock FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Expected product to exist: " + productId);
                return rs.getInt("stock");
            }
        }
    }

    private void setStock(String productId, int stock) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stock);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }
}
