package main.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

// REPRESENTS A WHOLE CAMPAIGN BLOCK IN THE CAMPAIGN REPORT

public class CampaignReportItem {
    private final String campaignId;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private String discountType;
    private final List<CampaignSoldItem> soldItems;
    private final double totalCampaignSales;

    public CampaignReportItem(String campaignId, LocalDateTime startDateTime, LocalDateTime endDateTime, String discountType, List<CampaignSoldItem> soldItems, double totalCampaignSales) {
        this.campaignId = campaignId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.discountType = discountType;
        this.soldItems = Collections.unmodifiableList(soldItems);
        this.totalCampaignSales = totalCampaignSales;
    }

    // GETTERS
    public String getCampaignId() { return campaignId; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getDiscountType() { return discountType; }
    public List<CampaignSoldItem> getSoldItems() { return soldItems; }
    public double getTotalCampaignSales() { return totalCampaignSales; }

    public int getItemCount() {
        return soldItems.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %-20s %-20s %5d   %s%n", campaignId, startDateTime.toLocalDate(), endDateTime.toLocalDate(), getItemCount(), discountType ));
        sb.append (" Sold\n");
        sb.append(String.format("  %-12s %-20s %-10s %-12s %-10s%n", "ID", "Description", "Discount", "Items Sold", "Total Sales £"));
        for (CampaignSoldItem item : soldItems) {
            sb.append("  ").append(item).append("\n");
        }
        sb.append(String.format("  Total Sales in campaign: £%.2f%n", totalCampaignSales));
        return sb.toString();
    }
}