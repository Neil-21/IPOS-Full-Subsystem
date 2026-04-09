package main.model;

// REPRESENTS ONE PRODUCT ROW WITHIN A CAMPAIGN IN THE CAMPAIGN REPORT

public class CampaignSoldItem {
    private final String itemId;
    private final String description;
    private final double discountRate;
    private int itemsSold;
    private final double totalSales;

    public CampaignSoldItem(String itemId, String description, double discountRate, int itemsSold, double totalSales) {
        this.itemId = itemId;
        this.description = description; 
        this.discountRate = discountRate;
        this.itemsSold = itemsSold;
        this.totalSales = totalSales;
    }

    // GETTERS
    public String getItemId() { return itemId; }
    public String getDescription() { return description; }
    public double getDiscountRate() { return discountRate; }
    public int getItemsSold() { return itemsSold; }
    public double getTotalSales() { return totalSales; }

    @Override 
    public String toString() {
        return String.format("%-12s %-20s %6.0f%%   %6d   £%8.2f", itemId, description, discountRate, itemsSold, totalSales);
    }
}