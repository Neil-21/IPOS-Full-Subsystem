package iposca.model;

import java.math.BigDecimal;

public class OrderItem {
    private int orderItemId;
    private int orderId;
    private String itemId;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;

    public OrderItem() {}

    public OrderItem(int orderId, String itemId, int quantity, BigDecimal unitCost) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.totalCost = unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public int getOrderItemID() { return orderItemId; }
    public void setOrderItemID(int id) { this.orderItemId = id; }

    public int getOrderID() { return orderId; }
    public void setOrderID(int orderId) { this.orderId = orderId; }

    public String getItemID() { return itemId; }
    public void setItemID(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
}