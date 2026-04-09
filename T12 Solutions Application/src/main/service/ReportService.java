package main.service;

import main.model.SalesReport;
import main.model.SalesReportItem;
import main.model.CampaignsReport;
import main.model.CampaignReportItem;
import main.model.CampaignSoldItem;
import main.model.CampaignEngagementReport;
import main.model.CampaignEngagementRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final Connection connection;

    public ReportService(Connection connection) {
        this.connection = connection;
    }

    /* To connect to the database, we have to add these lines to AppLauncher.java:
            Connection conn = DriverManager.getConnection(url, username, password);
            ReportService reportService = new ReportService(conn);

        Once we decide how we're going to access the database, we can add these.
    */

    public SalesReport generateSalesReport(LocalDate startDate, LocalDate endDate) throws SQLException {
        
        validateDateRange(startDate, endDate);

        // Once the database is made, we can change some of the names of the tables and columns here. I just made some myself for easier reading.
        String sql = "SELECT " +
                "p.item_id, " +
                "p.description, " +
                "SUM(oi.quantity) AS quantity_sold, " +
                "p.unit_price " +
                "FROM online_orders o " +
                "JOIN order_items oi ON o.order_id = oi.order_id " +
                "JOIN products p ON oi.item_id = p.item_id " +
                "WHERE o.order_date BETWEEN ? AND ? " +
                "AND o.status = 'COMPLETED' " + // Only counts orders that have been completed
                "GROUP BY p.item_id, p.description, p.unit_price " +
                "ORDER BY p.item_id";
 
        List<SalesReportItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, startDate);
            stmt.setObject(2, endDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String itemId = rs.getString("item_id");
                    String description = rs.getString("description");
                    int quantitySold = rs.getInt("quantity_sold");
                    double unitPrice = rs.getDouble("unit_price");

                    items.add(new SalesReportItem(itemId, description, quantitySold, unitPrice));

                }
            }
        }

        // Calculates the grand totals from the returned items
        int totalUnitsSold = items.stream().mapToInt(SalesReportItem::getQuantitySold).sum();
        double totalRevenue = items.stream().mapToDouble(SalesReportItem::getLineTotal).sum();

        return new SalesReport(startDate, endDate, items, totalUnitsSold, totalRevenue);
    }

    // Generates a Campaigns Report for IPOS-PU over a given date range
    public CampaignsReport generateCampaignsReport(LocalDate startDate, LocalDate endDate) throws SQLException {

        validateDateRange(startDate, endDate);

        /* These can change based on the database when its made.

            This particular query fetches all campaigns active within the given date range
        */
        String campaignSql = "SELECT " +
                "c.campaign_id, " +
                "c.start_datetime, " +
                "c.end_datetime, " +
                "c.discount_type " +
                "FROM campaigns c " +
                "WHERE c.start_datetime <= ? " +
                "AND c.end_datetime >= ? " +
                "ORDER BY c.start_datetime";
        
        // This query fetches all products sold within a specific campaign
        String soldItemsSql = "SELECT " +
                "p.item_id, " +
                "p.description, " +
                "ci.discount_rate, " +
                "SUM(oi.quantity) AS items_sold, " +
                "SUM(oi.quantity * p.unit_price * (1 - ci.discount_rate / 100)) AS total_sales " +
                "FROM campaign_items ci " +
                "JOIN products p ON ci.item_id = p.item_id " +
                "JOIN order_items oi ON oi.item_id = p.item_id " +
                "JOIN online_orders o ON oi.order_id = o.order_id " +
                "WHERE ci.campaign_id = ? " +
                "AND o.status = 'COMPLETED' " +
                "GROUP BY p.item_id, p.description, ci.discount_rate " +
                "ORDER BY p.item_id";

        List<CampaignReportItem> campaigns = new ArrayList<>();

        try (PreparedStatement campaignStmt = connection.prepareStatement(campaignSql)) {

            campaignStmt.setObject(1,endDate);
            campaignStmt.setObject(2, startDate);

            try (ResultSet campaignRs = campaignStmt.executeQuery()) {

                while (campaignRs.next()) {

                    String campaignId = campaignRs.getString("campaign_id");
                    LocalDateTime startDateTime = campaignRs.getObject("start_datetime", LocalDateTime.class);
                    LocalDateTime endDateTime = campaignRs.getObject("end_datetime", LocalDateTime.class);
                    String discountType = campaignRs.getString("discount_type");

                    // For each campaign, fetch its sold items in a nested query
                    List<CampaignSoldItem> soldItems = new ArrayList<>();

                    try (PreparedStatement soldStmt = connection.prepareStatement(soldItemsSql)) {

                        soldStmt.setString(1, campaignId);

                        try (ResultSet soldRs = soldStmt.executeQuery()) {

                            while (soldRs.next()) {

                                String itemId = soldRs.getString("item_id");
                                String description = soldRs.getString("description");
                                double discountRate = soldRs.getDouble("discount_rate");
                                int itemsSold = soldRs.getInt("items_sold");
                                double totalSales = soldRs.getDouble("total_sales");

                                soldItems.add(new CampaignSoldItem(itemId, description, discountRate, itemsSold, totalSales));
                            }
                        }
                    }

                    // Calculates total sales for this campaign across all products
                    double totalCampaignSales = soldItems.stream().mapToDouble(CampaignSoldItem::getTotalSales).sum();
                    campaigns.add(new CampaignReportItem(campaignId, startDateTime, endDateTime, discountType, soldItems, totalCampaignSales));

                }
            }
        }

        // Counts how many campaigns are still active right now
        int activeCampaignCount = (int) campaigns.stream().filter(c -> !c.getEndDateTime().isBefore(LocalDateTime.now())).count();

        return new CampaignsReport(startDate, endDate, campaigns, activeCampaignCount);

    }

    public CampaignEngagementReport generateCampaignEngagementReport(String campaignId) throws SQLException {

        if (campaignId == null || campaignId.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign ID must not be null or empty.");
        }

        String campaignSql = "SELECT " +
                "c.campaign_id, " +
                "c.description, " +
                "c.start_datetime, " +
                "c.end_datetime, " +
                "c.hit_count " +
                "FROM campaigns c " +
                "WHERE c.campaign_id = ?";

        String itemsSql = "SELECT " +
                "p.item_id, " +
                "p.description, " +
                "ci.hit_count, " +
                "ci.purchase_count " +
                "FROM campaign_items ci " +
                "JOIN products p ON ci.item_id = p.item_id " +
                "WHERE ci.campaign_id = ? " +
                "ORDER BY p.item_id";

        String campaignDescription = null;
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        int campaignHits = 0;

        try (PreparedStatement campaignStmt = connection.prepareStatement(campaignSql)) {

            campaignStmt.setString(1, campaignId);

            try (ResultSet rs = campaignStmt.executeQuery()) {

                if (rs.next()) {
                    campaignDescription = rs.getString("description");
                    startDateTime = rs.getObject("start_datetime", LocalDateTime.class);
                    endDateTime = rs.getObject("end_datetime", LocalDateTime.class);
                    campaignHits = rs.getInt("hit_count");
                }
            }
        }

        List<CampaignEngagementRow> rows = new ArrayList<>();

        rows.add(new CampaignEngagementRow(campaignId, "Campaign hits", campaignHits, 0));

        try (PreparedStatement itemsStmt = connection.prepareStatement(itemsSql)) {
            
            itemsStmt.setString(1, campaignId); 
            
            try (ResultSet rs = itemsStmt.executeQuery()) {

                int itemNumber = 1;

                while (rs.next()) {

                    String description = rs.getString("description");
                    int hitCount = rs.getInt("hit_count");
                    int purchaseCount = rs.getInt("purchase_count");
                    String counterId = "Item(" + itemNumber + ") hits";
                    String counterDescription = description + " hits";

                    rows.add(new CampaignEngagementRow(counterId, counterDescription, hitCount, purchaseCount));

                    itemNumber++;
                }
            }
        }

        return new CampaignEngagementReport(campaignId, campaignDescription, startDateTime, endDateTime, rows);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start date and end date must not be null.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date (" + start + ") must not be after end date (" + end + ").");
        }
    }
}
