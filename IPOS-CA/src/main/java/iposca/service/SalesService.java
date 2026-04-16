package iposca.service;

import iposca.dao.AccountHolderDAO;
import iposca.dao.DiscountPlanDAO;
import iposca.dao.SalesDAO;
import iposca.dao.StockDAO;
import iposca.model.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SalesService {
    private static final SalesDAO salesDAO = new SalesDAO();
    private static final StockDAO stockDAO = new StockDAO();
    private static final AccountHolderDAO accountDAO = new AccountHolderDAO();
    private static final DiscountPlanDAO discountPlanDAO = new DiscountPlanDAO();

    public static String recordSale(String customerType, String accountID,
                                    List<SaleItem> items, String paymentMethod,
                                    String cardType, String cardFirst4, String cardLast4,
                                    int cardExpiryMonth, int cardExpiryYear) throws SQLException {

        // calculates subtotal
        BigDecimal subtotal = items.stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // calculates discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (accountID != null) {
            AccountHolder ah = accountDAO.findByID(accountID);
            if (ah != null && ah.getDiscountPlanID() != null) {
                DiscountPlan plan = discountPlanDAO.findByID(ah.getDiscountPlanID());
                if (plan != null) {
                    discountAmount = plan.calculateDiscount(subtotal);
                }
            }
        }

        // calculates VAT (currently 0% per sample data, but configurable)
        BigDecimal vatRate = BigDecimal.ZERO; // fetch from merchant_config in production
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);
        BigDecimal vatAmount = afterDiscount.multiply(vatRate);
        BigDecimal totalAmount = afterDiscount.add(vatAmount);

        // builds sale object
        Sale sale = new Sale();
        sale.setSaleReference(generateReference());
        sale.setSaleDate(LocalDateTime.now());
        sale.setCustomerType(customerType);
        sale.setAccountID(accountID);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmount);
        sale.setVatAmount(vatAmount);
        sale.setTotalAmount(totalAmount);
        sale.setPaymentMethod(paymentMethod);
        sale.setPaymentStatus("Paid");
        sale.setServedBy(AuthService.getCurrentUser().getUserID());

        // saves sale and items (single transaction in DAO)
        int saleID = salesDAO.insertSaleWithItems(sale, items);

        // deducts stock for each item
        for (SaleItem item : items) {
            stockDAO.deductStock(item.getProductID(), item.getQuantity());
        }

        // if account holder on credit, increase their balance
        if ("Credit".equals(paymentMethod) && accountID != null) {
            accountDAO.addToBalance(accountID, totalAmount);
        }

        return sale.getSaleReference();
    }

    public static String generateInvoiceText(String saleReference) throws SQLException {
        Sale sale = salesDAO.findByReference(saleReference);
        if (sale == null) return "Invoice not found.";

        List<SaleItem> items = salesDAO.getItemsForSale(sale.getSaleID());
        StringBuilder sb = new StringBuilder();
        sb.append("COSYMED LTD\n");
        sb.append("25, Bond Street, London WC1V 8LS\n");
        sb.append("Phone: 0207 321 8001\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("RETAIL INVOICE\n");
        sb.append("Reference: ").append(sale.getSaleReference()).append("\n");
        sb.append("Date: ").append(sale.getSaleDate().toLocalDate()).append("\n");
        if (sale.getAccountID() != null)
            sb.append("Account: ").append(sale.getAccountID()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append(String.format("%-25s %-5s %-10s%n", "Item", "Qty", "Total"));
        sb.append("─".repeat(40)).append("\n");

        for (SaleItem item : items) {
            sb.append(String.format("%-25s %-5d £%-10.2f%n",
                    item.getProductID(),
                    item.getQuantity(),
                    item.getLineTotal()));
        }

        sb.append("─".repeat(40)).append("\n");
        sb.append(String.format("%-30s £%.2f%n", "Subtotal:", sale.getSubtotal()));
        if (sale.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
            sb.append(String.format("%-30s £%.2f%n",
                    "Discount:", sale.getDiscountAmount()));
        sb.append(String.format("%-30s £%.2f%n", "VAT:", sale.getVatAmount()));
        sb.append(String.format("%-30s £%.2f%n", "TOTAL DUE:", sale.getTotalAmount()));
        sb.append("\nThank you for your custom.");
        return sb.toString();
    }

    private static String generateReference() {
        return "SALE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    public static List<Sale> getSalesToday() throws SQLException {
        return salesDAO.getSalesToday();
    }

    public static List<Sale> getRecentSales(int limit) throws SQLException {
        return salesDAO.getRecent(limit);
    }
}