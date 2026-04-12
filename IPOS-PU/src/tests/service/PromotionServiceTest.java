package tests.service;

import main.db.DatabaseManager;
import main.model.Campaign;
import main.model.CampaignItem;
import main.service.PromotionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PromotionServiceTest {

    private PromotionService promotionService;
    private final List<String> campaignIdsToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseManager.initialise();
        cleanupResidualTestCampaignRows();
        promotionService = new PromotionService();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String campaignId : campaignIdsToCleanup) {
            deleteCampaignDirectly(campaignId);
        }
        cleanupResidualTestCampaignRows();
        campaignIdsToCleanup.clear();
    }

    // Expected: createCampaign returns true and campaign can be read back with items.
    @Test
    void testCreateCampaign_ValidCampaign_ReturnsTrueAndPersists() {
        String campaignId = uniqueCampaignId();
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                List.of(new CampaignItem("ITEM-101", 10.0), new CampaignItem("ITEM-102", 15.0))
        );

        boolean created = promotionService.createCampaign(campaign);
        assertTrue(created);
        campaignIdsToCleanup.add(campaignId);

        Campaign saved = promotionService.getCampaignById(campaignId);
        assertNotNull(saved);
        assertEquals(campaignId, saved.getCampaignId());
        assertEquals(2, saved.getItems().size());
    }

    // Expected: createCampaign returns false when campaign is invalid.
    @Test
    void testCreateCampaign_InvalidCampaign_ReturnsFalse() {
        Campaign invalidCampaign = buildCampaign(
                uniqueCampaignId(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                List.of(new CampaignItem("ITEM-201", 10.0))
        );

        boolean created = promotionService.createCampaign(invalidCampaign);
        assertFalse(created);
    }

    // Expected: getCampaignById returns null for missing campaign ID.
    @Test
    void testGetCampaignById_MissingId_ReturnsNull() {
        Campaign missing = promotionService.getCampaignById("NO-SUCH-CAMPAIGN-" + UUID.randomUUID());
        assertNull(missing);
    }

    // Expected: getActiveCampaigns includes active campaign and excludes expired campaign.
    @Test
    void testGetActiveCampaigns_FiltersByDate() {
        String activeId = uniqueCampaignId();
        String expiredId = uniqueCampaignId();

        Campaign active = buildCampaign(
                activeId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of(new CampaignItem("ITEM-301", 5.0))
        );
        Campaign expired = buildCampaign(
                expiredId,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),
                List.of(new CampaignItem("ITEM-302", 5.0))
        );

        assertTrue(promotionService.createCampaign(active));
        assertTrue(promotionService.createCampaign(expired));
        campaignIdsToCleanup.add(activeId);
        campaignIdsToCleanup.add(expiredId);

        List<Campaign> activeCampaigns = promotionService.getActiveCampaigns();
        boolean hasActive = activeCampaigns.stream().anyMatch(c -> c.getCampaignId().equals(activeId));
        boolean hasExpired = activeCampaigns.stream().anyMatch(c -> c.getCampaignId().equals(expiredId));

        assertTrue(hasActive);
        assertFalse(hasExpired);
    }

    // Expected: updateCampaign updates selected fields and returns true.
    @Test
    void testUpdateCampaign_ValidUpdate_ReturnsTrueAndUpdatesValues() {
        String campaignId = uniqueCampaignId();
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(3),
                List.of(new CampaignItem("ITEM-401", 10.0))
        );

        assertTrue(promotionService.createCampaign(campaign));
        campaignIdsToCleanup.add(campaignId);

        LocalDateTime newStart = LocalDateTime.now().minusHours(2);
        LocalDateTime newEnd = LocalDateTime.now().plusDays(10);
        boolean updated = promotionService.updateCampaign(campaignId, newStart, newEnd, "FIXED");

        assertTrue(updated);
        Campaign afterUpdate = promotionService.getCampaignById(campaignId);
        assertNotNull(afterUpdate);
        assertEquals("FIXED", afterUpdate.getDiscountType());
    }

    // Expected: cancelCampaign marks campaign as cancelled and removes it from active list.
    @Test
    void testCancelCampaign_ExistingCampaign_ReturnsTrueAndBecomesInactive() {
        String campaignId = uniqueCampaignId();
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of(new CampaignItem("ITEM-501", 10.0))
        );

        assertTrue(promotionService.createCampaign(campaign));
        campaignIdsToCleanup.add(campaignId);

        boolean cancelled = promotionService.cancelCampaign(campaignId);
        assertTrue(cancelled);

        Campaign cancelledCampaign = promotionService.getCampaignById(campaignId);
        assertNotNull(cancelledCampaign);
        assertTrue(cancelledCampaign.isCancelled());
    }

    // Expected: deleteCampaign removes campaign and related items from database.
    @Test
    void testDeleteCampaign_ExistingCampaign_ReturnsTrueAndDeletes() {
        String campaignId = uniqueCampaignId();
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of(new CampaignItem("ITEM-601", 10.0), new CampaignItem("ITEM-602", 20.0))
        );

        assertTrue(promotionService.createCampaign(campaign));

        boolean deleted = promotionService.deleteCampaign(campaignId);
        assertTrue(deleted);
        assertNull(promotionService.getCampaignById(campaignId));
    }

    // Expected: deleteCampaign returns false when campaign ID does not exist.
    @Test
    void testDeleteCampaign_MissingCampaign_ReturnsFalse() {
        boolean deleted = promotionService.deleteCampaign("MISSING-CAMPAIGN-" + UUID.randomUUID());
        assertFalse(deleted);
    }

    // Expected: getCampaignById returns null for null/blank IDs.
    @Test
    void testGetCampaignById_NullOrBlankId_ReturnsNull() {
        assertNull(promotionService.getCampaignById(null));
        assertNull(promotionService.getCampaignById(" "));
    }

    // Expected: updateCampaign returns false for invalid date range or null ID.
    @Test
    void testUpdateCampaign_InvalidInputs_ReturnsFalse() {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        assertFalse(promotionService.updateCampaign(null, start, end, "FIXED"));
        assertFalse(promotionService.updateCampaign("CAMP-ID", start, end, "FIXED"));
    }

    // Expected: updateCampaign supports partial update when dates are null.
    @Test
    void testUpdateCampaign_DiscountOnlyUpdate_ReturnsTrue() {
        String campaignId = uniqueCampaignId();
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(3),
                List.of(new CampaignItem("ITEM-701", 10.0))
        );
        assertTrue(promotionService.createCampaign(campaign));
        campaignIdsToCleanup.add(campaignId);

        boolean updated = promotionService.updateCampaign(campaignId, null, null, "FIXED");
        assertTrue(updated);

        Campaign afterUpdate = promotionService.getCampaignById(campaignId);
        assertNotNull(afterUpdate);
        assertEquals("FIXED", afterUpdate.getDiscountType());
    }

    // Expected: cancelCampaign returns false for blank or missing campaign IDs.
    @Test
    void testCancelCampaign_InvalidOrMissingId_ReturnsFalse() {
        assertFalse(promotionService.cancelCampaign(" "));
        assertFalse(promotionService.cancelCampaign("NO-SUCH-CAMPAIGN-" + UUID.randomUUID()));
    }

    // Expected: metric increment methods update persisted metric values.
    @Test
    void testMetricIncrements_UpdateCampaignAndItemMetrics() throws SQLException {
        String campaignId = uniqueCampaignId();
        String itemId = "ITEM-801";
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(3),
                List.of(new CampaignItem(itemId, 10.0))
        );
        assertTrue(promotionService.createCampaign(campaign));
        campaignIdsToCleanup.add(campaignId);

        promotionService.incrementCampaignHits(campaignId);
        promotionService.incrementCampaignHits(campaignId);
        promotionService.incrementItemHits(campaignId, itemId, 4);
        promotionService.incrementItemPurchases(campaignId, itemId, 2);

        assertEquals(2, getCampaignHits(campaignId));
        assertEquals(4, getItemHits(campaignId, itemId));
        assertEquals(2, getItemPurchases(campaignId, itemId));
    }

    // Expected: ensureMetricsExistForAllCampaigns recreates missing metric rows.
    @Test
    void testEnsureMetricsExistForAllCampaigns_RecreatesMissingMetrics() throws SQLException {
        String campaignId = uniqueCampaignId();
        String itemId = "ITEM-901";
        Campaign campaign = buildCampaign(
                campaignId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(3),
                List.of(new CampaignItem(itemId, 8.0))
        );
        assertTrue(promotionService.createCampaign(campaign));
        campaignIdsToCleanup.add(campaignId);

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement deleteCampaignMetrics = conn.prepareStatement(
                    "DELETE FROM campaign_metrics WHERE campaign_id = ?")) {
                deleteCampaignMetrics.setString(1, campaignId);
                deleteCampaignMetrics.executeUpdate();
            }
            try (PreparedStatement deleteItemMetrics = conn.prepareStatement(
                    "DELETE FROM campaign_item_metrics WHERE campaign_id = ?")) {
                deleteItemMetrics.setString(1, campaignId);
                deleteItemMetrics.executeUpdate();
            }
        }

        promotionService.ensureMetricsExistForAllCampaigns();

        assertTrue(campaignMetricExists(campaignId));
        assertTrue(itemMetricExists(campaignId, itemId));
    }

    private Campaign buildCampaign(String id, LocalDateTime start, LocalDateTime end, List<CampaignItem> items) {
        return new Campaign(id, start, end, "PERCENTAGE", items, false);
    }

    private String uniqueCampaignId() {
        return "TEST-CAMP-" + UUID.randomUUID();
    }

    private void deleteCampaignDirectly(String campaignId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement deleteItemMetrics = conn.prepareStatement(
                    "DELETE FROM campaign_item_metrics WHERE campaign_id = ?")) {
                deleteItemMetrics.setString(1, campaignId);
                deleteItemMetrics.executeUpdate();
            }

            try (PreparedStatement deleteCampaignMetrics = conn.prepareStatement(
                    "DELETE FROM campaign_metrics WHERE campaign_id = ?")) {
                deleteCampaignMetrics.setString(1, campaignId);
                deleteCampaignMetrics.executeUpdate();
            }

            try (PreparedStatement deleteItems = conn.prepareStatement(
                    "DELETE FROM campaign_items WHERE campaign_id = ?")) {
                deleteItems.setString(1, campaignId);
                deleteItems.executeUpdate();
            }

            try (PreparedStatement deleteCampaign = conn.prepareStatement(
                    "DELETE FROM campaigns WHERE campaign_id = ?")) {
                deleteCampaign.setString(1, campaignId);
                deleteCampaign.executeUpdate();
            }
        }
    }

    private void cleanupResidualTestCampaignRows() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM campaign_item_metrics WHERE campaign_id LIKE 'TEST-CAMP-%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM campaign_metrics WHERE campaign_id LIKE 'TEST-CAMP-%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM campaign_items WHERE campaign_id LIKE 'TEST-CAMP-%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM campaigns WHERE campaign_id LIKE 'TEST-CAMP-%'")) {
                ps.executeUpdate();
            }
        }
    }

    private int getCampaignHits(String campaignId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT campaign_hits FROM campaign_metrics WHERE campaign_id = ?")) {
            ps.setString(1, campaignId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt("campaign_hits");
            }
        }
    }

    private int getItemHits(String campaignId, String itemId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT item_hits FROM campaign_item_metrics WHERE campaign_id = ? AND item_id = ?")) {
            ps.setString(1, campaignId);
            ps.setString(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt("item_hits");
            }
        }
    }

    private int getItemPurchases(String campaignId, String itemId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT item_purchases FROM campaign_item_metrics WHERE campaign_id = ? AND item_id = ?")) {
            ps.setString(1, campaignId);
            ps.setString(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt("item_purchases");
            }
        }
    }

    private boolean campaignMetricExists(String campaignId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM campaign_metrics WHERE campaign_id = ?")) {
            ps.setString(1, campaignId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean itemMetricExists(String campaignId, String itemId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM campaign_item_metrics WHERE campaign_id = ? AND item_id = ?")) {
            ps.setString(1, campaignId);
            ps.setString(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
