package iposca.dao;

import iposca.db.DatabaseManager;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SAIntegrationDAO {
    
    private static final int COSYMED_SA_ACCOUNT_ID = 9;


    public List<String[]> getAllCatalogue() throws SQLException {
        List<String[]> items = new ArrayList<>();
        String sql = "SELECT item_id, description, package_type, unit, " +
                "units_in_pack, package_cost, availability " +
                "FROM public.catalogue_items ORDER BY description";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                items.add(new String[]{
                        rs.getString("item_id"),
                        rs.getString("description"),
                        rs.getString("package_type"),
                        rs.getString("unit"),
                        String.valueOf(rs.getInt("units_in_pack")),
                        rs.getBigDecimal("package_cost").toPlainString(),
                        String.valueOf(rs.getInt("availability"))
                });
            }
        }
        return items;
    }

    public List<String[]> searchCatalogue(String keyword) throws SQLException {
        List<String[]> items = new ArrayList<>();
        String sql = "SELECT item_id, description, package_type, unit, " +
                "units_in_pack, package_cost, availability " +
                "FROM public.catalogue_items " +
                "WHERE LOWER(description) LIKE LOWER(?) " +
                "ORDER BY description";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(new String[]{
                        rs.getString("item_id"),
                        rs.getString("description"),
                        rs.getString("package_type"),
                        rs.getString("unit"),
                        String.valueOf(rs.getInt("units_in_pack")),
                        rs.getBigDecimal("package_cost").toPlainString(),
                        String.valueOf(rs.getInt("availability"))
                });
            }
        }
        return items;
    }


    public String submitOrderToSA(List<String[]> items, BigDecimal totalValue,
                                  String orderId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            String orderSql = "INSERT INTO public.orders " +
                    "(order_id, account_id, order_date, total_value, status, " +
                    "dispatched_by, dispatch_date, courier, courier_ref, " +
                    "expected_delivery, delivery_date, discount_applied, payment_status) " +
                    "VALUES (?, ?, NOW(), ?, 'ACCEPTED', " +
                    "NULL, NULL, NULL, NULL, NULL, NULL, 0.00, 'PENDING')";
            try (PreparedStatement stmt = conn.prepareStatement(orderSql)) {
                stmt.setString(1, orderId);
                stmt.setInt(2, COSYMED_SA_ACCOUNT_ID);
                stmt.setBigDecimal(3, totalValue);
                stmt.executeUpdate();
            }

            String itemSql = "INSERT INTO public.order_items " +
                    "(order_id, item_id, quantity, unit_cost, total_cost) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                for (String[] item : items) {
                    int qty = Integer.parseInt(item[1]);
                    BigDecimal unitCost = new BigDecimal(item[2]);
                    BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(qty));
                    stmt.setString(1, orderId);
                    stmt.setString(2, item[0]);
                    stmt.setInt(3, qty);
                    stmt.setBigDecimal(4, unitCost);
                    stmt.setBigDecimal(5, totalCost);
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

    public List<String[]> getCosymedOrders() throws SQLException {
        List<String[]> orders = new ArrayList<>();
        String sql = "SELECT order_id, order_date, total_value, status, " +
                "dispatch_date, delivery_date, courier, courier_ref, " +
                "payment_status " +
                "FROM public.orders " +
                "WHERE account_id = ? " +
                "ORDER BY order_date DESC";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, COSYMED_SA_ACCOUNT_ID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(new String[]{
                        rs.getString("order_id"),
                        rs.getString("order_date"),
                        rs.getString("total_value"),
                        rs.getString("status"),
                        rs.getString("dispatch_date"),
                        rs.getString("delivery_date"),
                        rs.getString("courier"),
                        rs.getString("courier_ref"),
                        rs.getString("payment_status")
                });
            }
        }
        return orders;
    }

    // Track a specific order by ID
    public String[] getOrderStatus(String orderId) throws SQLException {
        String sql = "SELECT order_id, status, dispatch_date, delivery_date, " +
                "courier, courier_ref, expected_delivery " +
                "FROM public.orders WHERE order_id = ? AND account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, orderId);
            stmt.setInt(2, COSYMED_SA_ACCOUNT_ID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new String[]{
                        rs.getString("order_id"),
                        rs.getString("status"),
                        rs.getString("dispatch_date"),
                        rs.getString("delivery_date"),
                        rs.getString("courier"),
                        rs.getString("courier_ref"),
                        rs.getString("expected_delivery")
                };
            }
        }
        return null;
    }

    public double getCosymedBalance() throws SQLException {
        String sql = "SELECT balance FROM public.user_accounts " +
                "WHERE username = 'cosymed'";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getDouble("balance");
        }
        return 0.0;
    }

    public String getCosymedAccountStatus() throws SQLException {
        String sql = "SELECT account_status FROM public.user_accounts " +
                "WHERE username = 'cosymed'";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getString("account_status");
        }
        return "UNKNOWN";
    }
}