package tests.model;

import main.model.Campaign;
import main.model.CampaignItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CampaignTest {

    // Expected: constructor stores campaign fields and item count correctly.
    @Test
    void testConstructor_StoresBasicFields() {
        Campaign campaign = new Campaign(
                "CAMP-900",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                "PERCENTAGE",
                List.of(new CampaignItem("ITEM-1", 10.0), new CampaignItem("ITEM-2", 15.0)),
                false
        );

        assertEquals("CAMP-900", campaign.getCampaignId());
        assertEquals("PERCENTAGE", campaign.getDiscountType());
        assertEquals(2, campaign.getItemCount());
        assertFalse(campaign.isCancelled());
    }

    // Expected: isActive returns true when now is within start/end and not cancelled.
    @Test
    void testIsActive_WithinDateWindow_ReturnsTrue() {
        Campaign campaign = new Campaign(
                "CAMP-901",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                "PERCENTAGE",
                List.of(new CampaignItem("ITEM-1", 10.0)),
                false
        );

        assertTrue(campaign.isActive());
    }

    // Expected: isActive returns false when campaign is outside date window.
    @Test
    void testIsActive_OutsideDateWindow_ReturnsFalse() {
        Campaign campaign = new Campaign(
                "CAMP-902",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1),
                "PERCENTAGE",
                List.of(new CampaignItem("ITEM-1", 10.0)),
                false
        );

        assertFalse(campaign.isActive());
    }

    // Expected: cancel sets cancelled=true and campaign is no longer active.
    @Test
    void testCancel_SetsCancelledAndInactive() {
        Campaign campaign = new Campaign(
                "CAMP-903",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                "PERCENTAGE",
                List.of(new CampaignItem("ITEM-1", 10.0)),
                false
        );

        campaign.cancel();

        assertTrue(campaign.isCancelled());
        assertFalse(campaign.isActive());
    }

    // Expected: constructor honours cancelled flag when set to true.
    @Test
    void testConstructor_CancelledFlagTrue_IsCancelledTrue() {
        Campaign campaign = new Campaign(
                "CAMP-904",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                "PERCENTAGE",
                List.of(new CampaignItem("ITEM-1", 10.0)),
                true
        );

        assertTrue(campaign.isCancelled());
        assertFalse(campaign.isActive());
    }
}
