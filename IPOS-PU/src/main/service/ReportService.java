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
        String sql = """
            SELECT
                oi.product_id AS item_id,
                oi.product_name AS description,
                SUM(oi.quantity) AS quantity_sold,
                oi.unit_price AS unit_price
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE date(o.order_date) BETWEEN ? AND ?
            AND o.status = 'Received'
            GROUP BY oi.product_id, oi.product_name, oi.unit_price
            ORDER BY oi.product_id
        """;

        List<SalesReportItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());

//            stmt.setObject(1, startDate);
//            stmt.setObject(2, endDate);

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

        String campaignSql = """
        SELECT
            c.campaign_id,
            c.start_date,
            c.end_date,
            c.discount_type,
            c.cancelled
        FROM campaigns c
        WHERE date(c.start_date) <= date(?)
          AND date(c.end_date) >= date(?)
        ORDER BY c.start_date
    """;

        String soldItemsSql = """
            SELECT
                ci.item_id,
                COALESCE(MAX(oi.product_name), ci.item_id) AS description,
                ci.discount_rate,
                COALESCE(SUM(oi.quantity), 0) AS items_sold,
                COALESCE(SUM(oi.line_total), 0) AS total_sales
            FROM campaign_items ci
            LEFT JOIN order_items oi
                ON oi.product_id = ci.item_id
               AND oi.campaign_id = ci.campaign_id
            WHERE ci.campaign_id = ?
            GROUP BY ci.item_id, ci.discount_rate
            ORDER BY ci.item_id
    """;


        List<CampaignReportItem> campaigns = new ArrayList<>();

        try (PreparedStatement campaignStmt = connection.prepareStatement(campaignSql)) {

            campaignStmt.setString(1, endDate.toString());
            campaignStmt.setString(2, startDate.toString());

            try (ResultSet campaignRs = campaignStmt.executeQuery()) {

                while (campaignRs.next()) {

                    String campaignId = campaignRs.getString("campaign_id");
                    LocalDateTime startDateTime = parseDateTimeFlexible(campaignRs.getString("start_date"), false);
                    LocalDateTime endDateTime = parseDateTimeFlexible(campaignRs.getString("end_date"), true);
//                    LocalDateTime startDateTime = LocalDate.parse(campaignRs.getString("start_date")).atStartOfDay();
//                    LocalDateTime endDateTime = LocalDate.parse(campaignRs.getString("end_date")).atTime(23, 59);
                    String discountType = campaignRs.getString("discount_type");

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

                                soldItems.add(new CampaignSoldItem(
                                        itemId,
                                        description,
                                        discountRate,
                                        itemsSold,
                                        totalSales
                                ));
                            }
                        }
                    }

                    double totalCampaignSales = soldItems.stream()
                            .mapToDouble(CampaignSoldItem::getTotalSales)
                            .sum();

                    campaigns.add(new CampaignReportItem(
                            campaignId,
                            startDateTime,
                            endDateTime,
                            discountType,
                            soldItems,
                            totalCampaignSales
                    ));
                }
            }
        }

        int activeCampaignCount = (int) campaigns.stream()
                .filter(c -> !c.getEndDateTime().isBefore(LocalDateTime.now()))
                .count();

        return new CampaignsReport(startDate, endDate, campaigns, activeCampaignCount);
    }

    public CampaignEngagementReport generateCampaignEngagementReport(String campaignId) throws SQLException {

        if (campaignId == null || campaignId.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign ID must not be null or empty.");
        }

        String campaignSql = """
            SELECT
                c.campaign_id,
                c.discount_type,
                c.start_date,
                c.end_date
            FROM campaigns c
            WHERE c.campaign_id = ?
        """;

        String campaignHitsSql = """
            SELECT campaign_hits
            FROM campaign_metrics
            WHERE campaign_id = ?
        """;

        String itemsSql = """
            SELECT
                ci.item_id,
                ci.item_id AS description,
                COALESCE(cim.item_hits, 0) AS hit_count,
                COALESCE(cim.item_purchases, 0) AS purchase_count
            FROM campaign_items ci
            LEFT JOIN campaign_item_metrics cim
                ON ci.campaign_id = cim.campaign_id
                AND ci.item_id = cim.item_id
            WHERE ci.campaign_id = ?
            ORDER BY ci.item_id
        """;

        String campaignDescription = null;
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        try (PreparedStatement campaignStmt = connection.prepareStatement(campaignSql)) {

            campaignStmt.setString(1, campaignId);

            try (ResultSet rs = campaignStmt.executeQuery()) {
                if (rs.next()) {
                    campaignDescription = rs.getString("discount_type");
                    startDateTime = parseDateTimeFlexible(rs.getString("start_date"), false);
                    endDateTime = parseDateTimeFlexible(rs.getString("end_date"), true);
                } else {
                    throw new IllegalArgumentException("Campaign not found: " + campaignId);
                }
            }
        }

        int campaignHits = 0;

        try (PreparedStatement hitsStmt = connection.prepareStatement(campaignHitsSql)) {
            hitsStmt.setString(1, campaignId);

            try (ResultSet rs = hitsStmt.executeQuery()) {
                if (rs.next()) {
                    campaignHits = rs.getInt("campaign_hits");
                }
            }
        }

        List<CampaignEngagementRow> rows = new ArrayList<>();

        try (PreparedStatement itemsStmt = connection.prepareStatement(itemsSql)) {

            itemsStmt.setString(1, campaignId);

            try (ResultSet rs = itemsStmt.executeQuery()) {

                rows.add(new CampaignEngagementRow(
                        "Campaign",
                        "Campaign link clicks",
                        campaignHits,
                        0
                ));

                int itemNumber = 1;

                while (rs.next()) {
                    String description = rs.getString("description");
                    int hitCount = rs.getInt("hit_count");
                    int purchaseCount = rs.getInt("purchase_count");

                    String counterId = "Item(" + itemNumber + ")";
                    String counterDescription = description + " activity";

                    rows.add(new CampaignEngagementRow(
                            counterId,
                            counterDescription,
                            hitCount,
                            purchaseCount
                    ));

                    itemNumber++;
                }
            }
        }

        return new CampaignEngagementReport(
                campaignId,
                campaignDescription,
                startDateTime,
                endDateTime,
                rows
        );
    }

    private LocalDateTime parseDateTimeFlexible(String value, boolean endOfDay) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Date value must not be null or empty.");
        }

        value = value.trim();

        if (value.length() == 10) { // yyyy-MM-dd
            LocalDate date = LocalDate.parse(value);
            return endOfDay ? date.atTime(23, 59) : date.atStartOfDay();
        }

        return LocalDateTime.parse(value);
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
