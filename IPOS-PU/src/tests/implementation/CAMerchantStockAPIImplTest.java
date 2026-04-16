package tests.implementation;

import main.db.DatabaseManager;
import main.implementation.CAMerchantStockAPIImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class CAMerchantStockAPIImplTest {

    private CAMerchantStockAPIImpl api;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        api = new CAMerchantStockAPIImpl();
    }

    // Expected: checkStock returns stock for existing product and -1 for invalid/missing IDs.
    @Test
    void testCheckStock_ReturnsExpectedValues() {
        assertTrue(api.checkStock("PARA001") >= 0);
        assertEquals(-1, api.checkStock("NO-SUCH-PRODUCT"));
        assertEquals(-1, api.checkStock(null));
        assertEquals(-1, api.checkStock(" "));
    }

    // Expected: deductStock decreases stock when quantity is valid and available.
    @Test
    void testDeductStock_ValidQuantity_DecreasesStock() throws SQLException {
        int original = getStock("PARA001");
        setStock("PARA001", 30);
        try {
            boolean deducted = api.deductStock("PARA001", 5);
            int after = getStock("PARA001");

            assertTrue(deducted);
            assertEquals(25, after);
        } finally {
            setStock("PARA001", original);
        }
    }

    // Expected: deductStock returns false for invalid inputs or insufficient stock.
    @Test
    void testDeductStock_InvalidOrInsufficient_ReturnsFalse() throws SQLException {
        int original = getStock("PARA001");
        setStock("PARA001", 2);
        try {
            assertFalse(api.deductStock("PARA001", 3));
            assertFalse(api.deductStock("PARA001", 0));
            assertFalse(api.deductStock(" ", 1));
            assertFalse(api.deductStock(null, 1));
            assertEquals(2, getStock("PARA001"));
        } finally {
            setStock("PARA001", original);
        }
    }

    // Expected: listAvailableStock returns product listing and supports keyword filtering.
    @Test
    void testListAvailableStock_FilteringAndNoResults() {
        String allProducts = api.listAvailableStock(null);
        assertTrue(allProducts.contains("PARA001"));
        assertTrue(allProducts.contains("Stock:"));

        String filtered = api.listAvailableStock("pain relief");
        assertTrue(filtered.toLowerCase().contains("pain relief"));

        String notFound = api.listAvailableStock("no-such-keyword-xyz");
        assertEquals("No products found.", notFound);
    }

    // Expected: submitPaidOrder validates inputs and returns success message for valid values.
    @Test
    void testSubmitPaidOrder_ReturnsExpectedMessages() {
        assertEquals("Invalid order ID.", api.submitPaidOrder(null, "PARA001:2"));
        assertEquals("Invalid order ID.", api.submitPaidOrder(" ", "PARA001:2"));
        assertEquals("No items supplied.", api.submitPaidOrder("ORD-1", null));
        assertEquals("No items supplied.", api.submitPaidOrder("ORD-1", " "));

        String result = api.submitPaidOrder("ORD-12345", "PARA001:2,IBU002:1");
        assertTrue(result.contains("Paid order submitted to CA successfully."));
        assertTrue(result.contains("ORD-12345"));
    }

    private int getStock(String productId) throws SQLException {
        String sql = "SELECT stock FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt("stock");
            }
        }
    }

    private void setStock(String productId, int value) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }
}
