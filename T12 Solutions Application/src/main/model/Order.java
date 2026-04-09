package main.model;

import java.util.List;

public class Order {
  private String orderId;
    private List<OrderItem> items;
    private double total;

    public Order(String orderId, List<OrderItem> items) {
        this.orderId = orderId;
        this.items = items;
        calculateTotal();
    }

    private void calculateTotal() {
        total = 0;
        for (OrderItem item : items) {
            total += item.getPrice();
        }
    }

    public String getOrderId() {
        return orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }
}
