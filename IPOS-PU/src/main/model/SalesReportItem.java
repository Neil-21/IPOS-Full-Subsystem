package main.model;

public class SalesReportItem {

    /* REPRESENTS ONE ROW IN THE SALES REPORT TABLE
       E.g 0012685, Paracetomol, 250 packs sold, £0.10 each, £25.00 total
    */
    private final String itemId; // unique ID of each product
    private final String description; // the product name/description
    private final int quantitySold; // number of packs sold in the report period
    private final double unitPrice; // price per pack in £

    public SalesReportItem(String itemId, String description, int quantitySold, double unitPrice) {
        this.itemId = itemId;
        this.description = description;
        this.quantitySold = quantitySold;
        this.unitPrice = unitPrice;
    }

    // GETTERS 
    
    public String getItemId() { return itemId; }
    public String getDescription() { return description; }
    public int getQualitySold() { return quantitySold; }
    public double getUnitPrice() { return unitPrice; }

    public double getLineTotal() {
        return quantitySold * unitPrice;
    }

    @Override
    public String toString() {
        return String.format("%-12s %-40s %14d %14.2f %12.2f",
                itemId, description, quantitySold, unitPrice, getLineTotal());
//        return String.format("%-12s %-20s %6d   £%6.2f   £%8.2f", itemId, description, quantitySold, unitPrice, getLineTotal());
    }

    public int getQuantitySold() {
        return quantitySold;
    }
}