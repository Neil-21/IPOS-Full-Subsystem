package main.implementation;

import main.api.CAMerchantStockAPI;
import main.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CAMerchantStockAPIImpl implements CAMerchantStockAPI {

    @Override
    public int checkStock(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return -1;
        }

        String sql = """
            SELECT stock
            FROM products
            WHERE product_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, productId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public boolean deductStock(String productId, int quantity) {
        if (productId == null || productId.trim().isEmpty() || quantity <= 0) {
            return false;
        }

        String sql = """
            UPDATE products
            SET stock = stock - ?
            WHERE product_id = ?
              AND stock >= ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setString(2, productId.trim());
            ps.setInt(3, quantity);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String listAvailableStock(String keyword) {
        StringBuilder sb = new StringBuilder();

        String sql;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if (hasKeyword) {
            sql = """
                SELECT product_id, product_name, category, price, stock
                FROM products
                WHERE lower(product_name) LIKE lower(?)
                   OR lower(category) LIKE lower(?)
                ORDER BY product_id
            """;
        } else {
            sql = """
                SELECT product_id, product_name, category, price, stock
                FROM products
                ORDER BY product_id
            """;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasKeyword) {
                String pattern = "%" + keyword.trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString("product_id"))
                            .append(" | ")
                            .append(rs.getString("product_name"))
                            .append(" | ")
                            .append(rs.getString("category"))
                            .append(" | £")
                            .append(String.format("%.2f", rs.getDouble("price")))
                            .append(" | Stock: ")
                            .append(rs.getInt("stock"))
                            .append("\n");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error retrieving stock.";
        }

        if (sb.isEmpty()) {
            return "No products found.";
        }

        return sb.toString();
    }

    @Override
    public String submitPaidOrder(String orderId, String items) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return "Invalid order ID.";
        }
        if (items == null || items.trim().isEmpty()) {
            return "No items supplied.";
        }

        return "Paid order submitted to CA successfully. Order ID: " + orderId;
    }
}