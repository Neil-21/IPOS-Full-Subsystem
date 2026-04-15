package iposca.service;

import iposca.dao.StockDAO;
import iposca.model.StockItem;
import java.sql.SQLException;
import java.util.List;

public class StockService {
    private static final StockDAO stockDAO = new StockDAO();

    public static List<StockItem> getAllStock() throws SQLException {
        return stockDAO.getAll();
    }

    public static List<StockItem> searchStock(String keyword) throws SQLException {
        return stockDAO.search(keyword);
    }

    public static List<StockItem> getLowStockItems() throws SQLException {
        return stockDAO.getLowStockItems();
    }

    public static boolean addNewStockItem(StockItem item) throws SQLException {
        return stockDAO.insert(item);
    }

    public static boolean updateStockItem(StockItem item) throws SQLException {
        return stockDAO.update(item);
    }

    public static boolean removeStockItem(String productID) throws SQLException {
        return stockDAO.softDelete(productID);
    }

    public static boolean confirmDelivery(String productID, int quantity) throws SQLException {
        return stockDAO.addStock(productID, quantity);
    }

    public static boolean hasLowStock() throws SQLException {
        return !stockDAO.getLowStockItems().isEmpty();
    }

    public static boolean updateStockQuantity(String productId, int newQty) throws SQLException {
        return stockDAO.setStockQuantity(productId, newQty);
    }
}