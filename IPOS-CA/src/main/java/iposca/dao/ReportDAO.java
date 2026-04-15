package iposca.dao;

import iposca.db.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;

public class ReportDAO {

    public String getTurnoverReport(LocalDate from, LocalDate to) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("TURNOVER REPORT\n");
        sb.append("Period: ").append(from).append(" to ").append(to).append("\n");
        sb.append("─".repeat(50)).append("\n\n");

        String sql = "SELECT COUNT(*) as total_sales, " +
                "SUM(total_amount) as total_value, " +
                "SUM(discount_amount) as total_discounts " +
                "FROM ca.sales " +
                "WHERE DATE(sale_date) BETWEEN ? AND ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                sb.append("Total Sales: ").append(rs.getInt("total_sales")).append("\n");
                sb.append("Total Revenue: £").append(String.format("%.2f", rs.getDouble("total_value"))).append("\n");
                sb.append("Total Discounts Given: £").append(String.format("%.2f", rs.getDouble("total_discounts"))).append("\n");
            }
        }

        sb.append("\nORDERS PLACED WITH INFOPHARMA\n");
        sb.append("─".repeat(50)).append("\n");

        String orderSql = "SELECT COUNT(*) as order_count, " +
                "SUM(total_amount) as order_value " +
                "FROM ca.orders_to_infopharma " +
                "WHERE DATE(order_date) BETWEEN ? AND ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(orderSql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                sb.append("Orders Placed: ").append(rs.getInt("order_count")).append("\n");
                sb.append("Total Order Value: £").append(String.format("%.2f", rs.getDouble("order_value"))).append("\n");
            }
        }
        return sb.toString();
    }

    public String getStockAvailabilityReport() throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("STOCK AVAILABILITY REPORT\n");
        sb.append("Generated: ").append(LocalDate.now()).append("\n");
        sb.append("─".repeat(60)).append("\n\n");
        sb.append(String.format("%-30s %-10s %-10s %-10s %-10s%n",
                "Product", "Stock", "Threshold", "Value(£)", "Status"));
        sb.append("─".repeat(60)).append("\n");

        String sql = "SELECT product_id, product_name, current_stock, " +
                "reorder_level, retail_price, " +
                "(current_stock * retail_price) as stock_value " +
                "FROM ca.stock_items WHERE is_active = TRUE " +
                "ORDER BY product_name";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            double totalValue = 0;
            while (rs.next()) {
                String status = rs.getInt("current_stock") <= rs.getInt("reorder_level") ? "LOW" : "OK";
                double val = rs.getDouble("stock_value");
                totalValue += val;
                sb.append(String.format("%-30s %-10d %-10d %-10.2f %-10s%n",
                        rs.getString("product_name"),
                        rs.getInt("current_stock"),
                        rs.getInt("reorder_level"),
                        val,
                        status));
            }
            sb.append("─".repeat(60)).append("\n");
            sb.append(String.format("%-50s %-10.2f%n", "TOTAL STOCK VALUE:", totalValue));
        }
        return sb.toString();
    }

    public String getDebtReport(LocalDate from, LocalDate to) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("ACCOUNT HOLDER DEBT REPORT\n");
        sb.append("Period: ").append(from).append(" to ").append(to).append("\n");
        sb.append("─".repeat(60)).append("\n\n");

        String sql = "SELECT account_id, full_name, credit_limit, " +
                "current_balance, account_status " +
                "FROM ca.account_holders " +
                "ORDER BY current_balance DESC";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            double totalDebt = 0;
            sb.append(String.format("%-12s %-25s %-12s %-12s %-12s%n",
                    "Account", "Name", "Limit(£)", "Balance(£)", "Status"));
            sb.append("─".repeat(60)).append("\n");
            while (rs.next()) {
                double balance = rs.getDouble("current_balance");
                totalDebt += balance;
                sb.append(String.format("%-12s %-25s %-12.2f %-12.2f %-12s%n",
                        rs.getString("account_id"),
                        rs.getString("full_name"),
                        rs.getDouble("credit_limit"),
                        balance,
                        rs.getString("account_status")));
            }
            sb.append("─".repeat(60)).append("\n");
            sb.append(String.format("%-50s %-12.2f%n", "TOTAL OUTSTANDING DEBT:", totalDebt));
        }
        return sb.toString();
    }
}