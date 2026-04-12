package main.implementation;

import main.api.CAOrderStatusAPI;
import main.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CAOrderStatusAPIImpl implements CAOrderStatusAPI {

    @Override
    public String getOrderStatus(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return "Invalid order ID.";
        }

        String sql = """
            SELECT status
            FROM orders
            WHERE order_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, orderId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
                return "Order not found.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Failed to retrieve order status.";
        }
    }

    @Override
    public LocalDateTime listUpdatedOrders(LocalDateTime since) {

        if (since == null) {
            return null;
        }

        String sql = """
            SELECT MAX(order_date) AS latest_update
            FROM orders
            WHERE order_date > ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, since.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String latest = rs.getString("latest_update");

                    if (latest != null) {
                        return LocalDateTime.parse(latest);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return since;
    }
}