package main.api;

public interface CAMerchantStockAPI {

    int checkStock(String productId);

    boolean deductStock(String productId, int quantity);

    String listAvailableStock(String keyword);

    String submitPaidOrder(String orderId, String items);
}