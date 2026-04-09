package main.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

// REPRESENTS THE COMPLETE CAMPAIGNS REPORT OBJECT

public class CampaignsReport {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<CampaignReportItem> campaigns; 
    private final int activeCampaignCount;

    public CampaignsReport(LocalDate startDate, LocalDate endDate, List<CampaignReportItem> campaigns, int activeCampaignCount) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.campaigns = Collections.unmodifiableList(campaigns);
        this.activeCampaignCount = activeCampaignCount;
    }

    // GETTERS
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<CampaignReportItem> getCampaigns() { return campaigns; }
    public int getActiveCampaignCount()  { return activeCampaignCount; }

    public boolean isEmpty() {
        return campaigns.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IPOS-PU Campaigns Report\n");
        sb.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        sb.append("Active campaigns: ").append(activeCampaignCount).append("\n");
        sb.append("-".repeat(75)).append("\n");
        for (CampaignReportItem campaign : campaigns) {
            sb.append(campaign).append("\n");
        }
        return sb.toString();
    }
}