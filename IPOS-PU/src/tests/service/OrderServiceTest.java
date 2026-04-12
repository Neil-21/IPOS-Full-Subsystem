package tests.service;

import main.api.PUCommsAPI;
import main.db.DatabaseManager;
import main.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {

    private OrderService orderService;
    private TestCommsApi commsApi;
    private final List<String> usersToDelete = new ArrayList<>();
    private final List<String> ordersToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseManager.initialise();
        cleanupResidualOrderTestData();
        commsApi = new TestCommsApi();
        orderService = new OrderService(commsApi);
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String orderId : ordersToDelete) {
            deleteOrderById(orderId);
        }
        for (String email : usersToDelete) {
            deleteUser(email);
        }
        cleanupResidualOrderTestData();
        ordersToDelete.clear();
        usersToDelete.clear();
    }

    // Expected: successful checkout persists order, items, and completed payment, and sends confirmation email.
    @Test
    void testCheckoutOrder_ValidInput_PersistsAndReturnsSuccess() throws SQLException {
        String email = uniqueEmail();
        insertUser(email);

        List<OrderService.OrderLine> items = List.of(
                new OrderService.OrderLine("PARA001", "Paracetamol", 2, 2.50, null),
                new OrderService.OrderLine("IBU002", "Ibuprofen", 1, 4.00, "Camp 05")
        );

        OrderService.CheckoutResult result = orderService.checkoutOrder(
                email, items, "1 Test Street", "Flat 2", "4242 4242 4242 4242", false
        );

        assertTrue(result.success());
        assertNotNull(result.orderId());
        assertEquals(9.0, result.finalTotal(), 0.0001);
        assertFalse(result.discountApplied());
        assertTrue(result.propagatedToMerchant());
        assertEquals(1, commsApi.sentEmailCount);

        ordersToDelete.add(result.orderId());
        assertEquals("Received", orderService.getOrderStatus(email, result.orderId()));
        assertEquals(2, orderService.getOrderItems(result.orderId()).size());
        assertEquals("COMPLETED", getPaymentStatus(result.orderId()));
    }

    // Expected: failed payment still records order/payment state and returns failed checkout result.
    @Test
    void testCheckoutOrder_PaymentDeclined_ReturnsFailureWithPersistedFailedPayment() throws SQLException {
        String email = uniqueEmail();
        insertUser(email);
        commsApi.authorisePaymentResult = false;

        List<OrderService.OrderLine> items = List.of(
                new OrderService.OrderLine("PARA001", "Paracetamol", 1, 3.00, null)
        );

        OrderService.CheckoutResult result = orderService.checkoutOrder(
                email, items, "2 Test Street", "", "4000 0000 0000 0002", false
        );

        assertFalse(result.success());
        assertNotNull(result.orderId());
        assertEquals("Payment was declined.", result.message());
        assertEquals(0, commsApi.sentEmailCount, "No confirmation email should be sent for failed payment.");

        ordersToDelete.add(result.orderId());
        assertEquals("Payment Failed", orderService.getOrderStatus(email, result.orderId()));
        assertEquals("FAILED", getPaymentStatus(result.orderId()));
    }

    // Expected: checkout validation rejects missing required fields/items.
    @Test
    void testCheckoutOrder_InvalidInput_ReturnsFailure() {
        List<OrderService.OrderLine> validItems = List.of(
                new OrderService.OrderLine("PARA001", "Paracetamol", 1, 2.50, null)
        );

        assertFalse(orderService.checkoutOrder(null, validItems, "1 Test Street", "", "4242", false).success());
        assertFalse(orderService.checkoutOrder("user@ipos.com", List.of(), "1 Test Street", "", "4242", false).success());
        assertFalse(orderService.checkoutOrder("user@ipos.com", validItems, " ", "", "4242", false).success());
        assertFalse(orderService.checkoutOrder(
                "user@ipos.com",
                List.of(new OrderService.OrderLine(" ", "Paracetamol", 1, 2.50, null)),
                "1 Test Street",
                "",
                "4242",
                false
        ).success());
    }

    // Expected: updateOrderStatus updates existing order and getOrderStatus reflects the new value.
    @Test
    void testUpdateAndGetOrderStatus_ValidOrder_UpdatesAndReadsStatus() throws SQLException {
        String email = uniqueEmail();
        insertUser(email);

        OrderService.CheckoutResult result = orderService.checkoutOrder(
                email,
                List.of(new OrderService.OrderLine("PARA001", "Paracetamol", 1, 1.50, null)),
                "3 Test Street",
                "",
                "4242 4242 4242 4242",
                false
        );
        assertTrue(result.success());
        ordersToDelete.add(result.orderId());

        boolean updated = orderService.updateOrderStatus(result.orderId(), "Dispatched");
        assertTrue(updated);
        assertEquals("Dispatched", orderService.getOrderStatus(email, result.orderId()));
    }

    // Expected: qualifiesForTenthOrderDiscount is true only when user has exactly 9 completed orders.
    @Test
    void testQualifiesForTenthOrderDiscount_ReturnsExpectedResults() throws SQLException {
        String email = uniqueEmail();
        insertUser(email);

        assertFalse(orderService.qualifiesForTenthOrderDiscount(email));

        for (int i = 0; i < 9; i++) {
            String orderId = "ORD-TEST-" + UUID.randomUUID();
            insertOrderDirectly(orderId, email, "Received");
            ordersToDelete.add(orderId);
        }

        assertTrue(orderService.qualifiesForTenthOrderDiscount(email));
    }

    // Expected: getOrdersForUser returns only matching user's orders sorted by date.
    @Test
    void testGetOrdersForUser_ReturnsOnlyUserOrders() throws SQLException {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        insertUser(emailA);
        insertUser(emailB);

        String orderA1 = "ORD-TEST-" + UUID.randomUUID();
        String orderA2 = "ORD-TEST-" + UUID.randomUUID();
        String orderB1 = "ORD-TEST-" + UUID.randomUUID();
        insertOrderDirectly(orderA1, emailA, "Received");
        insertOrderDirectly(orderA2, emailA, "Dispatched");
        insertOrderDirectly(orderB1, emailB, "Received");
        ordersToDelete.add(orderA1);
        ordersToDelete.add(orderA2);
        ordersToDelete.add(orderB1);

        List<OrderService.OrderSummary> summaries = orderService.getOrdersForUser(emailA);
        assertEquals(2, summaries.size());
        assertTrue(summaries.stream().allMatch(s -> emailA.equalsIgnoreCase(getUserEmailForOrder(s.orderId()))));
    }

    private String uniqueEmail() {
        return "ordertest-" + UUID.randomUUID() + "@ipos.com";
    }

    private void insertUser(String email) throws SQLException {
        String sql = """
            INSERT INTO users (email, full_name, password, role, first_login)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            ps.setString(2, "Order Test User");
            ps.setString(3, "Pass123!");
            ps.setString(4, "CUSTOMER");
            ps.setInt(5, 0);
            ps.executeUpdate();
        }
        usersToDelete.add(email.toLowerCase());
    }

    private void insertOrderDirectly(String orderId, String email, String status) throws SQLException {
        String sql = """
            INSERT INTO orders (order_id, user_email, order_date, item_count, status, total_amount)
            VALUES (?, ?, datetime('now'), 1, ?, 10.0)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, email.toLowerCase());
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }

    private String getPaymentStatus(String orderId) throws SQLException {
        String sql = "SELECT payment_status FROM payments WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Expected payment row for order: " + orderId);
                return rs.getString("payment_status");
            }
        }
    }

    private String getUserEmailForOrder(String orderId) {
        String sql = "SELECT user_email FROM orders WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("user_email");
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private void deleteOrderById(String orderId) throws SQLException {
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
            ps.setString(1, email.toLowerCase());
            ps.executeUpdate();
        }
    }

    private void cleanupResidualOrderTestData() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM payments WHERE user_email LIKE 'ordertest-%@ipos.com'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    DELETE FROM order_items
                    WHERE order_id IN (
                        SELECT order_id FROM orders WHERE user_email LIKE 'ordertest-%@ipos.com'
                    )
                    """)) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE user_email LIKE 'ordertest-%@ipos.com'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE email LIKE 'ordertest-%@ipos.com'")) {
                ps.executeUpdate();
            }
        }
    }

    private static class TestCommsApi implements PUCommsAPI {
        boolean authorisePaymentResult = true;
        int sentEmailCount = 0;

        @Override
        public boolean sendEmail(String to, String subject, String body) {
            sentEmailCount++;
            return true;
        }

        @Override
        public boolean authorisePayment(String orderId, double amount) {
            return authorisePaymentResult;
        }

        @Override
        public void recordTransaction(String refId, String type, String outcome, String timestamp) {
            // No-op for tests.
        }
    }
}
