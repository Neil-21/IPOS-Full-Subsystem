package tests.implementation;

import main.db.DatabaseManager;
import main.implementation.CAOrderStatusAPIImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CAOrderStatusAPIImplTest {

    private CAOrderStatusAPIImpl api;
    private final List<String> usersToDelete = new ArrayList<>();
    private final List<String> ordersToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        api = new CAOrderStatusAPIImpl();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String orderId : ordersToDelete) {
            deleteOrder(orderId);
        }
        for (String email : usersToDelete) {
            deleteUser(email);
        }
        ordersToDelete.clear();
        usersToDelete.clear();
    }

    // Expected: returns status value for an existing order ID.
    @Test
    void testGetOrderStatus_ExistingOrder_ReturnsStatus() throws SQLException {
        String email = uniqueEmail();
        String orderId = uniqueOrderId();
        insertUser(email);
        insertOrder(orderId, email, "Dispatched", LocalDateTime.now().minusMinutes(10));

        String status = api.getOrderStatus(orderId);

        assertEquals("Dispatched", status);
    }

    // Expected: returns not-found message for unknown order.
    @Test
    void testGetOrderStatus_MissingOrder_ReturnsNotFoundMessage() {
        String status = api.getOrderStatus("CA-STATUS-MISSING-" + UUID.randomUUID());
        assertEquals("Order not found.", status);
    }

    // Expected: returns invalid-order-id message for null/blank values.
    @Test
    void testGetOrderStatus_InvalidOrderId_ReturnsValidationMessage() {
        assertEquals("Invalid order ID.", api.getOrderStatus(null));
        assertEquals("Invalid order ID.", api.getOrderStatus(" "));
    }

    // Expected: returns latest order_date after the provided timestamp.
    @Test
    void testListUpdatedOrders_WithNewerOrders_ReturnsLatestTimestamp() throws SQLException {
        String email = uniqueEmail();
        insertUser(email);

        LocalDateTime since = LocalDateTime.now().minusHours(3);
        LocalDateTime t1 = LocalDateTime.now().minusHours(2);
        LocalDateTime t2 = LocalDateTime.now().minusHours(1);

        insertOrder(uniqueOrderId(), email, "Received", t1);
        insertOrder(uniqueOrderId(), email, "Delivered", t2);

        LocalDateTime latest = api.listUpdatedOrders(since);

        assertEquals(t2, latest);
    }

    // Expected: returns original timestamp when no newer order updates exist.
    @Test
    void testListUpdatedOrders_NoNewerOrders_ReturnsSinceValue() throws SQLException {
        String email = uniqueEmail();
        insertUser(email);

        LocalDateTime oldTime = LocalDateTime.now().minusDays(2);
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        insertOrder(uniqueOrderId(), email, "Received", oldTime);

        LocalDateTime latest = api.listUpdatedOrders(since);

        assertEquals(since, latest);
    }

    // Expected: returns null when input timestamp is null.
    @Test
    void testListUpdatedOrders_NullSince_ReturnsNull() {
        assertNull(api.listUpdatedOrders(null));
    }

    private String uniqueEmail() {
        return "ca-status-" + UUID.randomUUID() + "@ipos.com";
    }

    private String uniqueOrderId() {
        return "CA-STATUS-ORD-" + UUID.randomUUID();
    }

    private void insertUser(String email) throws SQLException {
        String sql = """
            INSERT INTO users (email, full_name, password, role, first_login)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, "CA Status User");
            ps.setString(3, "Pass123!");
            ps.setString(4, "CUSTOMER");
            ps.setInt(5, 0);
            ps.executeUpdate();
        }
        usersToDelete.add(email);
    }

    private void insertOrder(String orderId, String email, String status, LocalDateTime orderDate) throws SQLException {
        String sql = """
            INSERT INTO orders (order_id, user_email, order_date, item_count, status, total_amount)
            VALUES (?, ?, ?, 1, ?, 10.0)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, email);
            ps.setString(3, orderDate.toString());
            ps.setString(4, status);
            ps.executeUpdate();
        }
        ordersToDelete.add(orderId);
    }

    private void deleteOrder(String orderId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM payments WHERE order_id = ?")) {
                ps.setString(1, orderId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                ps.setString(1, orderId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {
                ps.setString(1, orderId);
                ps.executeUpdate();
            }
        }
    }

    private void deleteUser(String email) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }
}
