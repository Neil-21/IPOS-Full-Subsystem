package main.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

// REPRESENTS THE COMPLETE CAMPAIGN ENGAGEMENT REPORT FOR A SPECIFIC CAMPAIGN

public class CampaignEngagementReport {

    private final String campaignId;
    private final String campaignDescription;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final List<CampaignEngagementRow> rows;

    public CampaignEngagementReport(String campaignId, String campaignDescription, LocalDateTime startDateTime, LocalDateTime endDateTime, List<CampaignEngagementRow> rows) {
        
        this.campaignId = campaignId;
        this.campaignDescription = campaignDescription;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.rows = Collections.unmodifiableList(rows);

    }

    // GETTERS
    public String getCampaignId() { return campaignId; }
    public String getCampaignDescription() { return campaignDescription; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public List<CampaignEngagementRow> getRows() { return rows; }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer Campaign Engagement Report\n");
        sb.append("Campaign ID: ").append(campaignId).append("\n");
        sb.append("Campaign Description: ").append(campaignDescription).append("\n");
        sb.append("Start Period: ").append(startDateTime.toLocalDate()).append("\n");
        sb.append("End Period: ").append(endDateTime.toLocalDate()).append("\n");
        sb.append("-".repeat(105)).append("\n");
//        sb.append(String.format("%-15s %-25s %8s   %8s   %s%n", "Counter ID", "Counter Description", "Hits", "Purchases", "Conversion Rate"));
        sb.append(String.format("%-15s %-42s %10s %12s %20s%n",
                "Counter ID", "Counter Description", "Hits", "Purchases", "Conversion Rate"));
        sb.append("-".repeat(105)).append("\n");
        for (CampaignEngagementRow row : rows) {
            sb.append(row).append("\n");
        }

        return sb.toString();
    }
}