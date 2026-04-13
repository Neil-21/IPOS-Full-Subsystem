package iposca.service;

import iposca.dao.OrderDAO;
import iposca.dao.SAIntegrationDAO;
import iposca.dao.StockDAO;
import iposca.model.Order;
import iposca.model.OrderItem;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderService {
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final StockDAO stockDAO = new StockDAO();
    private static final SAIntegrationDAO saDAO = new SAIntegrationDAO();

    public static String placeOrder(List<OrderItem> items) throws SQLException {
        Order order = new Order();
        order.setOrderReference(generateReference());
        order.setOrderDate(java.time.LocalDateTime.now());
        order.setPlacedBy(AuthService.getCurrentUser().getUserID());
        order.setOrderStatus("Submitted");
        order.setTotalAmount(items.stream()
                .map(OrderItem::getTotalCost)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        orderDAO.insertOrderWithItems(order, items);

        try {
            List<String[]> saItems = new ArrayList<>();
            for (OrderItem item : items) {
                saItems.add(new String[]{
                        item.getItemID(),
                        String.valueOf(item.getQuantity()),
                        item.getUnitCost().toPlainString()
                });
            }
            saDAO.submitOrderToSA(saItems, order.getTotalAmount());
        } catch (Exception e) {
            System.err.println("Warning: Could not submit to SA database: "
                    + e.getMessage());
        }

        return order.getOrderReference();
    }

    // called when delivery physically arrives at pharmacy
    public static boolean confirmDelivery(int orderID, String courier,
                                          String trackingNumber) throws SQLException {
        // update order status to Delivered
        boolean updated = orderDAO.confirmDelivery(orderID, courier, trackingNumber);
        if (!updated) return false;

        // increase stock for each item in the order
        List<OrderItem> items = orderDAO.getItemsForOrder(orderID);
        for (OrderItem item : items) {
            stockDAO.addStock(item.getItemID(), item.getQuantity());
        }
        return true;
    }

    public static List<Order> getAllOrders() throws SQLException {
        return orderDAO.getAll();
    }

    private static String generateReference() {
        return "CA-ORD-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    public static int getPendingOrderCount() throws SQLException {
        List<Order> orders = orderDAO.getAll();
        return (int) orders.stream()
                .filter(o -> !o.getOrderStatus().equals("Delivered")
                        && !o.getOrderStatus().equals("Cancelled"))
                .count();
    }

    public static int getThisMonthOrderCount() throws SQLException {
        return orderDAO.getThisMonthCount();
    }
}