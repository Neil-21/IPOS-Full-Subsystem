package iposca.dao;

import iposca.db.DatabaseManager;
import iposca.model.StockItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockDAO {

    public StockItem findByID(String productID) throws SQLException {
        String sql = "SELECT * FROM stock_items WHERE product_id = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, productID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<StockItem> getAll() throws SQLException {
        List<StockItem> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_items WHERE is_active = TRUE ORDER BY product_name";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<StockItem> search(String keyword) throws SQLException {
        List<StockItem> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_items WHERE is_active = TRUE AND " +
                "(product_name LIKE ? OR product_id LIKE ?)";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<StockItem> getLowStockItems() throws SQLException {
        List<StockItem> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_items WHERE is_active = TRUE AND current_stock <= reorder_level";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean insert(StockItem item) throws SQLException {
        String sql = "INSERT INTO stock_items (product_id, product_name, description, unit_type, " +
                "form, pack_size, wholesale_cost, retail_price, current_stock, reorder_level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, item.getProductID());
            stmt.setString(2, item.getProductName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getUnitType());
            stmt.setString(5, item.getForm());
            stmt.setInt(6, item.getPackSize());
            stmt.setBigDecimal(7, item.getWholesaleCost());
            stmt.setBigDecimal(8, item.getRetailPrice());
            stmt.setInt(9, item.getCurrentStock());
            stmt.setInt(10, item.getReorderLevel());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean update(StockItem item) throws SQLException {
        String sql = "UPDATE stock_items SET product_name=?, description=?, unit_type=?, " +
                "form=?, pack_size=?, wholesale_cost=?, retail_price=?, reorder_level=? " +
                "WHERE product_id=?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, item.getProductName());
            stmt.setString(2, item.getDescription());
            stmt.setString(3, item.getUnitType());
            stmt.setString(4, item.getForm());
            stmt.setInt(5, item.getPackSize());
            stmt.setBigDecimal(6, item.getWholesaleCost());
            stmt.setBigDecimal(7, item.getRetailPrice());
            stmt.setInt(8, item.getReorderLevel());
            stmt.setString(9, item.getProductID());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deductStock(String productID, int quantity) throws SQLException {
        // only deducts if enough stock exists
        String sql = "UPDATE stock_items SET current_stock = current_stock - ? " +
                "WHERE product_id = ? AND current_stock >= ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, productID);
            stmt.setInt(3, quantity);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean addStock(String productID, int quantity) throws SQLException {
        String sql = "UPDATE stock_items SET current_stock = current_stock + ? WHERE product_id = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, productID);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean softDelete(String productID) throws SQLException {
        String sql = "UPDATE stock_items SET is_active = FALSE WHERE product_id = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, productID);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean setStockQuantity(String productId, int quantity) throws SQLException {
        String sql = "UPDATE stock_items SET current_stock = ? WHERE product_id = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, productId);
            return stmt.executeUpdate() > 0;
        }
    }

    private StockItem mapRow(ResultSet rs) throws SQLException {
        StockItem item = new StockItem();
        item.setProductID(rs.getString("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setDescription(rs.getString("description"));
        item.setUnitType(rs.getString("unit_type"));
        item.setForm(rs.getString("form"));
        item.setPackSize(rs.getInt("pack_size"));
        item.setWholesaleCost(rs.getBigDecimal("wholesale_cost"));
        item.setRetailPrice(rs.getBigDecimal("retail_price"));
        item.setCurrentStock(rs.getInt("current_stock"));
        item.setReorderLevel(rs.getInt("reorder_level"));
        item.setActive(rs.getBoolean("is_active"));
        return item;
    }
}