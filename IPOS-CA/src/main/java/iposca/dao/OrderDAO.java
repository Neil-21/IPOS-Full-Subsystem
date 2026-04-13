package iposca.dao;

import iposca.db.DatabaseManager;
import iposca.model.Order;
import iposca.model.OrderItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public int insertOrderWithItems(Order order,
                                    List<OrderItem> items) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            String orderSql = "INSERT INTO orders_to_infopharma " +
                    "(order_reference, order_status, total_amount, " +
                    "placed_by, notes) " +
                    "VALUES (?, 'Submitted', ?, ?, ?)";
            int orderId;
            String orderSqlReturning = orderSql + " RETURNING order_id";
            try (PreparedStatement stmt =
                         conn.prepareStatement(orderSqlReturning)) {
                stmt.setString(1, order.getOrderReference());
                stmt.setBigDecimal(2, order.getTotalAmount());
                stmt.setInt(3, order.getPlacedBy());
                stmt.setString(4, order.getNotes());
                ResultSet rs = stmt.executeQuery();
                rs.next();
                orderId = rs.getInt(1);
            }

            String itemSql = "INSERT INTO order_items " +
                    "(order_id, item_id, quantity, unit_cost, total_cost) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                for (OrderItem item : items) {
                    stmt.setInt(1, orderId);
                    stmt.setString(2, item.getItemID());
                    stmt.setInt(3, item.getQuantity());
                    stmt.setBigDecimal(4, item.getUnitCost());
                    stmt.setBigDecimal(5, item.getTotalCost());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();
            return orderId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Order> getAll() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders_to_infopharma ORDER BY order_date DESC";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean updateStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders_to_infopharma SET order_status = ? " +
                "WHERE order_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean confirmDelivery(int orderId, String courier,
                                   String trackingNumber) throws SQLException {
        String sql = "UPDATE orders_to_infopharma " +
                "SET order_status = 'Delivered', " +
                "delivery_date = CURRENT_DATE, " +
                "courier = ?, tracking_number = ? " +
                "WHERE order_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, courier);
            stmt.setString(2, trackingNumber);
            stmt.setInt(3, orderId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<OrderItem> getItemsForOrder(int orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setOrderItemID(rs.getInt("order_item_id"));
                item.setOrderID(rs.getInt("order_id"));
                item.setItemID(rs.getString("item_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitCost(rs.getBigDecimal("unit_cost"));
                item.setTotalCost(rs.getBigDecimal("total_cost"));
                list.add(item);
            }
        }
        return list;
    }

    public int getThisMonthCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders_to_infopharma " +
                "WHERE EXTRACT(MONTH FROM order_date) = EXTRACT(MONTH FROM NOW()) " +
                "AND EXTRACT(YEAR FROM order_date) = EXTRACT(YEAR FROM NOW())";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderID(rs.getInt("order_id"));
        order.setOrderReference(rs.getString("order_reference"));
        order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
        order.setOrderStatus(rs.getString("order_status"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setPlacedBy(rs.getInt("placed_by"));
        order.setCourier(rs.getString("courier"));
        order.setTrackingNumber(rs.getString("tracking_number"));
        order.setNotes(rs.getString("notes"));
        Date d1 = rs.getDate("dispatch_date");
        Date d2 = rs.getDate("delivery_date");
        if (d1 != null) order.setDispatchDate(d1.toLocalDate());
        if (d2 != null) order.setDeliveryDate(d2.toLocalDate());
        return order;
    }
}