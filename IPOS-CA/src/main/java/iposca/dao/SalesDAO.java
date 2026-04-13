package iposca.dao;

import iposca.db.DatabaseManager;
import iposca.model.Sale;
import iposca.model.SaleItem;

import java.sql.SQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalesDAO {

    public int insertSaleWithItems(Sale sale, List<SaleItem> items) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            String saleSql = "INSERT INTO sales (sale_reference, customer_type, account_id, " +
                    "subtotal, discount_amount, vat_amount, total_amount, " +
                    "payment_method, payment_status, served_by, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING sale_id";
            int saleId;
            try (PreparedStatement stmt = conn.prepareStatement(saleSql)) {
                stmt.setString(1, sale.getSaleReference());
                stmt.setString(2, sale.getCustomerType());
                stmt.setString(3, sale.getAccountID());
                stmt.setBigDecimal(4, sale.getSubtotal());
                stmt.setBigDecimal(5, sale.getDiscountAmount());
                stmt.setBigDecimal(6, sale.getVatAmount());
                stmt.setBigDecimal(7, sale.getTotalAmount());
                stmt.setString(8, sale.getPaymentMethod());
                stmt.setString(9, sale.getPaymentStatus());
                stmt.setInt(10, sale.getServedBy());
                stmt.setString(11, sale.getNotes());
                ResultSet rs = stmt.executeQuery(); // executeQuery not executeUpdate for RETURNING
                rs.next();
                saleId = rs.getInt(1);
            }

            String itemSql = "INSERT INTO sale_items " +
                    "(sale_id, product_id, quantity, unit_price, line_total) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                for (SaleItem item : items) {
                    stmt.setInt(1, saleId);
                    stmt.setString(2, item.getProductID());
                    stmt.setInt(3, item.getQuantity());
                    stmt.setBigDecimal(4, item.getUnitPrice());
                    stmt.setBigDecimal(5, item.getLineTotal());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();
            return saleId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<SaleItem> getItemsForSale(int saleID) throws SQLException {
        List<SaleItem> list = new ArrayList<>();
        String sql = "SELECT * FROM sale_items WHERE sale_id = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, saleID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SaleItem item = new SaleItem();
                item.setItemID(rs.getInt("item_id"));
                item.setSaleID(rs.getInt("sale_id"));
                item.setProductID(rs.getString("product_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getBigDecimal("unit_price"));
                item.setLineTotal(rs.getBigDecimal("line_total"));
                list.add(item);
            }
        }
        return list;
    }
    public List<Sale> getAll() throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY sale_date DESC";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Sale> getSalesToday() throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE DATE(sale_date) = CURRENT_DATE";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Sale> getRecent(int limit) throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY sale_date DESC LIMIT ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Sale mapRow(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSaleID(rs.getInt("sale_id"));
        sale.setSaleReference(rs.getString("sale_reference"));
        sale.setSaleDate(rs.getTimestamp("sale_date").toLocalDateTime());
        sale.setCustomerType(rs.getString("customer_type"));
        sale.setAccountID(rs.getString("account_id"));
        sale.setSubtotal(rs.getBigDecimal("subtotal"));
        sale.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        sale.setVatAmount(rs.getBigDecimal("vat_amount"));
        sale.setTotalAmount(rs.getBigDecimal("total_amount"));
        sale.setPaymentMethod(rs.getString("payment_method"));
        sale.setPaymentStatus(rs.getString("payment_status"));
        sale.setServedBy(rs.getInt("served_by"));
        sale.setNotes(rs.getString("notes"));
        return sale;
    }

}
