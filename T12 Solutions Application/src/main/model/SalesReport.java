package main.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SalesReport {

    /* THE COMPLETE SALES REPORT.
       HOLDS THE FULL LIST OF SalesReportItem 
    */
   
    private final LocalDate startDate; // first day of the report period (inclusive)
    private final LocalDate endDate; // last day of the report period (inclusive)
    private final List<SalesReportItem> items; // the product rows making up the report body
    private final int totalUnitsSold; // grand total of all packs sold across all products
    private final double totalRevenue; // grand total revenue (£) across all products

    public SalesReport(LocalDate startDate, LocalDate endDate, List<SalesReportItem> items, int totalUnitsSold, double totalRevenue) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.items = Collections.unmodifiableList(items);
        this.totalUnitsSold = totalUnitsSold;
        this.totalRevenue = totalRevenue;

    }

    // GETTERS 

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<SalesReportItem> getItems() { return items; }
    public int getTotalUnitsSold() { return totalUnitsSold; }
    public double getTotalRevenue() { return totalRevenue; }

    // Returns true if the report contains no sales
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override 
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IPOS-PU Sales Report\n");
        sb.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        sb.append(String.format("%-12s %-20s %13s   %-12s %-10s%n", "Item ID", "Description", "Sold (packs)", "Unit Price £", "Total £"));
        sb.append("-".repeat(75)).append("\n");
        for (SalesReportItem item : items) {
            sb.append(item).append("\n");
        }
        sb.append("-".repeat(75)).append("\n");
        sb.append(String.format("Total online sales for period: %d packs   £%.2f%n", totalUnitsSold, totalRevenue));
        return sb.toString();
    }
}
