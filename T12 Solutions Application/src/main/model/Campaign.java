package main.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import main.model.CampaignItem;
// REPRESENTS A PROMOTION/CAMPAIGN RUN VIA THE IPOS-PU PORTAL

public class Campaign {
    private final String campaignId;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final String discountType;
    private final List<CampaignItem> items;
    private boolean cancelled;

    public Campaign(String campaignId, LocalDateTime startDateTime, LocalDateTime endDateTime, String discountType, List<CampaignItem> items, boolean cancelled) {
        this.campaignId = campaignId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.discountType = discountType;
        this.items = Collections.unmodifiableList(items);
        this.cancelled = false;
    }
    
    // GETTERS
    public String getCampaignId() { return campaignId; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getDiscountType() { return discountType; }
    public List<CampaignItem> getItems() { return items; }
    public boolean isCancelled() { return cancelled; }

    public boolean isActive() {
        if (cancelled) return false;
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDateTime) && !now.isAfter(endDateTime);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public int getItemCount() {
        return items.size();
    }

    @Override 
    public String toString() {
        return String.format("Campaign[id=%s, start=%s, end=%s, type=%s, items=%d, active=%s]", campaignId, startDateTime, endDateTime, discountType, getItemCount(), isActive());
    }
}


