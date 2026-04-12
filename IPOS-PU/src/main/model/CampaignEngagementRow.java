package main.model;

// REPRESENTS A SINGLE COUNTER ROW IN THE CUSTOMER CAMPAIGN ENGAGEMENT REPORT

public class CampaignEngagementRow {

    private final String counterId;
    private final String counterDescription;
    private int hitsCount;
    private int purchases;

    public CampaignEngagementRow(String counterId, String counterDescription, int hitsCount, int purchases) {
        
        this.counterId = counterId;
        this.counterDescription = counterDescription;
        this.hitsCount = hitsCount;
        this.purchases = purchases;

    }

    // GETTERS
    public String getCounterId() { return counterId; }
    public String getCounterDescription() { return counterDescription; }
    public int getHitsCount() { return hitsCount; }
    public int getpurchases() { return purchases; }

    public double getConversionRate() {

        if (hitsCount == 0 ) return 0;
        return (double) purchases / hitsCount;

    }

    public String getConversionRateFormatted() {

        if ("Campaign".equalsIgnoreCase(counterId)) {
            return "N/A";
        }

        if (hitsCount == 0) return "N/A";

        return String.format("%d / %d = %.2f (%.1f%%)",
                purchases,
                hitsCount,
                getConversionRate(),
                getConversionRate() * 100);

    }

    @Override
    public String toString() {
        return String.format("%-15s %-42s %10d %12s %20s",
                counterId,
                counterDescription,
                hitsCount,
                "Campaign".equalsIgnoreCase(counterId) ? "N/A" : String.valueOf(purchases),
                getConversionRateFormatted());
    }
}