package main.service;

import main.db.DatabaseManager;
import main.model.Campaign;
import main.model.CampaignItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PromotionService {

//    private static final DateTimeFormatter FORMATTER =
//            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // To create campaigns

    public boolean createCampaign(Campaign campaign) {
        if (campaign == null) return false;
        if (!validateCampaign(campaign)) return false;

        String insertCampaign = """
            INSERT INTO campaigns (campaign_id, start_date, end_date, discount_type, cancelled)
            VALUES (?, ?, ?, ?, 0)
        """;
        String insertItem = """
            INSERT INTO campaign_items (campaign_id, item_id, discount_rate)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertCampaign)) {
                ps.setString(1, campaign.getCampaignId());
                ps.setString(2, campaign.getStartDateTime().format(FORMATTER));
                ps.setString(3, campaign.getEndDateTime().format(FORMATTER));
                ps.setString(4, campaign.getDiscountType());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (CampaignItem item : campaign.getItems()) {
                    ps.setString(1, campaign.getCampaignId());
                    ps.setString(2, item.getItemId());
                    ps.setDouble(3, item.getDiscountRate());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            String insertCampaignMetrics = """
    INSERT INTO campaign_metrics (campaign_id, campaign_hits)
    VALUES (?, 0)
""";

            try (PreparedStatement ps = conn.prepareStatement(insertCampaignMetrics)) {
                ps.setString(1, campaign.getCampaignId());
                ps.executeUpdate();
            }

            String insertItemMetrics = """
                INSERT INTO campaign_item_metrics (campaign_id, item_id, item_hits, item_purchases)
                VALUES (?, ?, 0, 0)
            """;

            try (PreparedStatement ps = conn.prepareStatement(insertItemMetrics)) {
                for (CampaignItem item : campaign.getItems()) {
                    ps.setString(1, campaign.getCampaignId());
                    ps.setString(2, item.getItemId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // To read campaigns

    public Campaign getCampaignById(String campaignId) {
        if (campaignId == null || campaignId.trim().isEmpty()) return null;

        String sql = "SELECT * FROM campaigns WHERE campaign_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, campaignId.trim());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            return mapCampaign(conn, rs);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Campaign> getAllCampaigns() {
        String sql = "SELECT * FROM campaigns ORDER BY start_date DESC";
        List<Campaign> campaigns = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                campaigns.add(mapCampaign(conn, rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return campaigns;
    }

    public List<Campaign> getActiveCampaigns() {
        String sql = """
            SELECT * FROM campaigns
            WHERE cancelled = 0
              AND start_date <= ?
              AND end_date   >= ?
            ORDER BY start_date DESC
        """;
        List<Campaign> campaigns = new ArrayList<>();
        String now = LocalDateTime.now().format(FORMATTER);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, now);
            ps.setString(2, now);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                campaigns.add(mapCampaign(conn, rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return campaigns;
    }

    // To update campaigns

    public boolean updateCampaign(String campaignId, LocalDateTime newStart,
                                  LocalDateTime newEnd, String newDiscountType) {
        if (campaignId == null) return false;
        if (newStart != null && newEnd != null && !newEnd.isAfter(newStart)) return false;

        String sql = """
            UPDATE campaigns
            SET start_date    = COALESCE(?, start_date),
                end_date      = COALESCE(?, end_date),
                discount_type = COALESCE(?, discount_type)
            WHERE campaign_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStart != null ? newStart.format(FORMATTER) : null);
            ps.setString(2, newEnd   != null ? newEnd.format(FORMATTER)   : null);
            ps.setString(3, newDiscountType);
            ps.setString(4, campaignId.trim());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // To cancel campaigns

    public boolean cancelCampaign(String campaignId) {
        if (campaignId == null || campaignId.trim().isEmpty()) return false;

        String sql = "UPDATE campaigns SET cancelled = 1 WHERE campaign_id = ? AND cancelled = 0";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, campaignId.trim());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // To delete campaigns

    public boolean deleteCampaign(String campaignId) {
        if (campaignId == null || campaignId.trim().isEmpty()) return false;

//        String deleteItems = "DELETE FROM campaign_items WHERE campaign_id = ?";
//        String deleteCampaign = "DELETE FROM campaigns WHERE campaign_id = ?";

        String deleteItemMetrics = "DELETE FROM campaign_item_metrics WHERE campaign_id = ?";
        String deleteCampaignMetrics = "DELETE FROM campaign_metrics WHERE campaign_id = ?";
        String deleteItems = "DELETE FROM campaign_items WHERE campaign_id = ?";
        String deleteCampaign = "DELETE FROM campaigns WHERE campaign_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(deleteItemMetrics)) {
                ps.setString(1, campaignId.trim());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(deleteCampaignMetrics)) {
                ps.setString(1, campaignId.trim());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(deleteItems)) {
                ps.setString(1, campaignId.trim());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteCampaign)) {
                ps.setString(1, campaignId.trim());
                int rows = ps.executeUpdate();
                conn.commit();
                return rows > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helpers

    private LocalDateTime parseDateTimeFlexible(String value, boolean endOfDay) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Date value must not be null or empty.");
        }

        value = value.trim();

        if (value.length() == 10) {
            return endOfDay
                    ? LocalDateTime.parse(value + "T23:59:00", FORMATTER)
                    : LocalDateTime.parse(value + "T00:00:00", FORMATTER);
        }

        if (value.length() == 16) { // for yyyy-MM-ddTHH:mm
            value = value + ":00";
        }

        return LocalDateTime.parse(value, FORMATTER);
    }

    private boolean validateCampaign(Campaign campaign) {
        if (campaign.getCampaignId() == null || campaign.getCampaignId().trim().isEmpty()) return false;
        if (campaign.getStartDateTime() == null || campaign.getEndDateTime() == null) return false;
        if (!campaign.getEndDateTime().isAfter(campaign.getStartDateTime())) return false;
        if (campaign.getDiscountType() == null || campaign.getDiscountType().trim().isEmpty()) return false;
        if (campaign.getItems() == null || campaign.getItems().isEmpty()) return false;

        for (CampaignItem item : campaign.getItems()) {
            if (item.getItemId() == null || item.getItemId().trim().isEmpty()) return false;
            if (item.getDiscountRate() <= 0 || item.getDiscountRate() > 100) return false;
        }
        return true;
    }

    private Campaign mapCampaign(Connection conn, ResultSet rs) throws SQLException {
        String id = rs.getString("campaign_id");
        List<CampaignItem> items = getItemsForCampaign(conn, id);

        return new Campaign(
                id,
                parseDateTimeFlexible(rs.getString("start_date"), false),
                parseDateTimeFlexible(rs.getString("end_date"), true),
                rs.getString("discount_type"),
                items,
                rs.getInt("cancelled") == 1
        );
    }

    private List<CampaignItem> getItemsForCampaign(Connection conn, String campaignId)
            throws SQLException {
        String sql = "SELECT * FROM campaign_items WHERE campaign_id = ?";
        List<CampaignItem> items = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, campaignId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new CampaignItem(
                        rs.getString("item_id"),
                        rs.getDouble("discount_rate")
                ));
            }
        }
        return items;
    }

    public void incrementCampaignHits(String campaignId) {
        String sql = """
            UPDATE campaign_metrics
            SET campaign_hits = campaign_hits + 1
            WHERE campaign_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, campaignId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementItemHits(String campaignId, String itemId, int quantity) {
        String sql = """
            UPDATE campaign_item_metrics
            SET item_hits = item_hits + ?
            WHERE campaign_id = ? AND item_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setString(2, campaignId);
            ps.setString(3, itemId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementItemPurchases(String campaignId, String itemId, int quantity) {
        String sql = """
            UPDATE campaign_item_metrics
            SET item_purchases = item_purchases + ?
            WHERE campaign_id = ? AND item_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setString(2, campaignId);
            ps.setString(3, itemId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ensureMetricsExistForAllCampaigns() {
        List<Campaign> campaigns = getAllCampaigns();

        String insertCampaignMetrics = """
            INSERT OR IGNORE INTO campaign_metrics (campaign_id, campaign_hits)
            VALUES (?, 0)
        """;

        String insertItemMetrics = """
            INSERT OR IGNORE INTO campaign_item_metrics (campaign_id, item_id, item_hits, item_purchases)
            VALUES (?, ?, 0, 0)
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement campaignPs = conn.prepareStatement(insertCampaignMetrics);
                 PreparedStatement itemPs = conn.prepareStatement(insertItemMetrics)) {

                for (Campaign campaign : campaigns) {
                    campaignPs.setString(1, campaign.getCampaignId());
                    campaignPs.executeUpdate();

                    for (CampaignItem item : campaign.getItems()) {
                        itemPs.setString(1, campaign.getCampaignId());
                        itemPs.setString(2, item.getItemId());
                        itemPs.addBatch();
                    }
                }

                itemPs.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
