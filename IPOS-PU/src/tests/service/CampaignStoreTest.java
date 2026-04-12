package tests.service;

import main.db.DatabaseManager;
import main.model.Campaign;
import main.model.CampaignItem;
import main.service.CampaignStore;
import main.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CampaignStoreTest {

    @BeforeEach
    void setUp() {
        clearCampaignStore();
    }

    // Expected: adding a non-null campaign stores it and returns in getAllCampaigns.
    @Test
    void testAddCampaign_ValidCampaign_AddedToStore() {
        Campaign campaign = buildCampaign(
                "CAMP-100",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        CampaignStore.addCampaign(campaign);

        List<Campaign> campaigns = CampaignStore.getAllCampaigns();
        assertEquals(1, campaigns.size());
        assertEquals("CAMP-100", campaigns.get(0).getCampaignId());
    }

    // Expected: adding null campaign does not change store contents.
    @Test
    void testAddCampaign_NullCampaign_Ignored() {
        CampaignStore.addCampaign(null);

        assertTrue(CampaignStore.getAllCampaigns().isEmpty());
    }

    // Expected: getAllCampaigns returns a defensive copy, not the internal list.
    @Test
    void testGetAllCampaigns_ReturnsDefensiveCopy() {
        CampaignStore.addCampaign(buildCampaign(
                "CAMP-101",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));

        List<Campaign> copy = CampaignStore.getAllCampaigns();
        copy.clear();

        assertEquals(1, CampaignStore.getAllCampaigns().size());
    }

    // Expected: getActiveCampaigns returns only campaigns active at current time.
    @Test
    void testGetActiveCampaigns_FiltersByActiveWindowAndCancelledFlag() {
        Campaign active = buildCampaign(
                "CAMP-ACTIVE",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );
        Campaign expired = buildCampaign(
                "CAMP-EXPIRED",
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1)
        );
        Campaign cancelled = buildCampaign(
                "CAMP-CANCELLED",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );
        cancelled.cancel();

        CampaignStore.addCampaign(active);
        CampaignStore.addCampaign(expired);
        CampaignStore.addCampaign(cancelled);

        List<Campaign> activeCampaigns = CampaignStore.getActiveCampaigns();
        assertEquals(1, activeCampaigns.size());
        assertEquals("CAMP-ACTIVE", activeCampaigns.get(0).getCampaignId());
    }

    // Expected: findById matches campaign IDs case-insensitively.
    @Test
    void testFindById_CaseInsensitiveMatch_ReturnsCampaign() {
        CampaignStore.addCampaign(buildCampaign(
                "Camp-Case",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));

        Campaign found = CampaignStore.findById("camp-case");

        assertNotNull(found);
        assertEquals("Camp-Case", found.getCampaignId());
    }

    // Expected: findById returns null when campaign does not exist.
    @Test
    void testFindById_NotFound_ReturnsNull() {
        Campaign found = CampaignStore.findById("MISSING-ID");

        assertNull(found);
    }

    // Expected: removeCampaign removes matching campaign ID case-insensitively.
    @Test
    void testRemoveCampaign_ExistingId_ReturnsTrueAndRemoves() {
        CampaignStore.addCampaign(buildCampaign(
                "Camp-Remove",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));

        boolean removed = CampaignStore.removeCampaign("camp-remove");

        assertTrue(removed);
        assertTrue(CampaignStore.getAllCampaigns().isEmpty());
    }

    // Expected: removeCampaign returns false when campaign ID is not present.
    @Test
    void testRemoveCampaign_NonExistingId_ReturnsFalse() {
        CampaignStore.addCampaign(buildCampaign(
                "Camp-Existing",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));

        boolean removed = CampaignStore.removeCampaign("Camp-Other");

        assertFalse(removed);
        assertEquals(1, CampaignStore.getAllCampaigns().size());
    }

    // Expected: adding duplicate campaign IDs (case-insensitive) keeps only first campaign.
    @Test
    void testAddCampaign_DuplicateId_IgnoresSecond() {
        Campaign first = buildCampaign("Camp-Dupe", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        Campaign duplicate = buildCampaign("camp-dupe", LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(2));

        CampaignStore.addCampaign(first);
        CampaignStore.addCampaign(duplicate);

        List<Campaign> campaigns = CampaignStore.getAllCampaigns();
        assertEquals(1, campaigns.size());
        assertEquals("Camp-Dupe", campaigns.get(0).getCampaignId());
    }

    // Expected: clear removes all campaigns from store.
    @Test
    void testClear_RemovesAllCampaigns() {
        CampaignStore.addCampaign(buildCampaign("CAMP-201", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));
        CampaignStore.addCampaign(buildCampaign("CAMP-202", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));

        CampaignStore.clear();

        assertTrue(CampaignStore.getAllCampaigns().isEmpty());
    }

    // Expected: loadFromDatabase refreshes store with campaigns persisted in DB.
    @Test
    void testLoadFromDatabase_LoadsPersistedCampaigns() throws SQLException {
        DatabaseManager.initialise();
        PromotionService promotionService = new PromotionService();

        String campaignId = "DB-CAMP-" + UUID.randomUUID();
        Campaign dbCampaign = buildCampaign(campaignId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertTrue(promotionService.createCampaign(dbCampaign));

        try {
            CampaignStore.loadFromDatabase(promotionService);
            Campaign loaded = CampaignStore.findById(campaignId);
            assertNotNull(loaded);
            assertEquals(campaignId, loaded.getCampaignId());
        } finally {
            deleteCampaignDirectly(campaignId);
        }
    }

    private Campaign buildCampaign(String id, LocalDateTime start, LocalDateTime end) {
        List<CampaignItem> items = List.of(new CampaignItem("ITEM-001", 10.0));
        return new Campaign(id, start, end, "PERCENTAGE", items, false);
    }

    private void clearCampaignStore() {
        for (Campaign c : CampaignStore.getAllCampaigns()) {
            CampaignStore.removeCampaign(c.getCampaignId());
        }
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
}
