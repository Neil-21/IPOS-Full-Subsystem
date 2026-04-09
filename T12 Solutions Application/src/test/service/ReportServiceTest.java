package test.service;

import main.model.SalesReport;
import main.model.CampaignsReport;
import main.service.ReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

// Tests for the ReportService

public class ReportServiceTest {

    private Connection connection;
    private ReportService reportService;

    /*
     * SETUP
     * 
     * Creates a fresh in-memory SQLite database before each test and sets up
     * the tables and sample data needed for the tests to run.
     *
     */

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        reportService = new ReportService(connection);
        createTablesAndSeedData();
    }

    /*
     * PU-RPT-01 - Valid Sales Report Generation
     */

    @Test
    void testGenerateSalesReport_ValidDateRange_ReturnsSalesData() throws SQLException {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 12, 31);

        SalesReport report = reportService.generateSalesReport(start, end);

        // Report should not be null or empty
        assertNotNull(report, "Report should not be null");
        assertFalse(report.isEmpty(), "Report should contain sales data");

        // Totals should be greater than zero
        assertTrue(report.getTotalUnitsSold() > 0, "Total units sold should be greater than 0");
        assertTrue(report.getTotalRevenue() > 0, "Total revenue should be greater than 0");

        // Date range should be preserved correctly
        assertEquals(start, report.getStartDate(), "Start date should match input");
        assertEquals(end,   report.getEndDate(),   "End date should match input");
    }

    /*
     * PU-RPT-02 - Invalid date range rejected for sales report
     */

    @Test
    void testGenerateSalesReport_StartDateAfterEndDate_ThrowsException() {
        LocalDate start = LocalDate.of(2025, 12, 31);
        LocalDate end   = LocalDate.of(2025, 1, 1); // end before start

        // Should throw IllegalArgumentException since start > end
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.generateSalesReport(start, end);
        }, "Should throw IllegalArgumentException when startDate is after endDate");
    }

    /**
     * PU-RPT-02 (edge case)
     */

    @Test
    void testGenerateSalesReport_NullDates_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.generateSalesReport(null, null);
        }, "Should throw IllegalArgumentException when dates are null");
    }

    /*
     * PU-RPT-03 - Valid Campaigns Report Generation
     */
    @Test
    void testGenerateCampaignsReport_ValidDateRange_ReturnsCampaignData() throws SQLException {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 12, 31);

        CampaignsReport report = reportService.generateCampaignsReport(start, end);

        // Report should not be null
        assertNotNull(report, "Report should not be null");

        // Should contain campaigns
        assertFalse(report.isEmpty(), "Report should contain campaign data");

        // Date range should be preserved correctly
        assertEquals(start, report.getStartDate(), "Start date should match input");
        assertEquals(end,   report.getEndDate(),   "End date should match input");
    }

    /*
     * PU-RPT-03 (invalid date range)
     */

    @Test
    void testGenerateCampaignsReport_InvalidDateRange_ThrowsException() {
        LocalDate start = LocalDate.of(2025, 12, 31);
        LocalDate end   = LocalDate.of(2025, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> {
            reportService.generateCampaignsReport(start, end);
        }, "Should throw IllegalArgumentException when startDate is after endDate");
    }

    /*
     * PU-RPT-04 - Campaigns report returns empty when no campaigns exist
     */

    @Test
    void testGenerateCampaignsReport_NoCampaignsInPeriod_ReturnsEmptyReport() throws SQLException {
        // Use a date range far in the future where no seeded campaigns exist
        LocalDate start = LocalDate.of(2099, 1, 1);
        LocalDate end   = LocalDate.of(2099, 12, 31);

        CampaignsReport report = reportService.generateCampaignsReport(start, end);

        assertNotNull(report, "Report should not be null even if empty");
        assertTrue(report.isEmpty(), "Report should be empty when no campaigns exist in period");
        assertEquals(0, report.getActiveCampaignCount(), "Active campaign count should be 0");
    }


    /**
     * Helpers - Creates the minimum database tables needed for ReportService to run,
     * and inserts sample data matching what the SQL queries expect.
     *
     * TODO: Update table and column names here to match the database schema when it is agreed upon.
     */
    private void createTablesAndSeedData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // Products table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    item_id     TEXT PRIMARY KEY,
                    description TEXT NOT NULL,
                    unit_price  REAL NOT NULL
                )
            """);

            // Online orders table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS online_orders (
                    order_id   TEXT PRIMARY KEY,
                    order_date TEXT NOT NULL,
                    status     TEXT NOT NULL
                )
            """);

            // Order items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    order_id TEXT NOT NULL,
                    item_id  TEXT NOT NULL,
                    quantity INTEGER NOT NULL
                )
            """);

            // Campaigns table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS campaigns (
                    campaign_id    TEXT PRIMARY KEY,
                    start_datetime TEXT NOT NULL,
                    end_datetime   TEXT NOT NULL,
                    discount_type  TEXT NOT NULL,
                    description    TEXT,
                    hit_count      INTEGER DEFAULT 0
                )
            """);

            // Campaign items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS campaign_items (
                    campaign_id    TEXT NOT NULL,
                    item_id        TEXT NOT NULL,
                    discount_rate  REAL NOT NULL,
                    hit_count      INTEGER DEFAULT 0,
                    purchase_count INTEGER DEFAULT 0
                )
            """);

            // Seed products
            stmt.execute("""
                INSERT INTO products VALUES
                ('100 00001', 'Paracetamol', 0.10),
                ('100 00002', 'Aspirin',     0.50)
            """);

            // Seed a completed order in 2025
            stmt.execute("""
                INSERT INTO online_orders VALUES
                ('ORD001', '2025-05-15', 'COMPLETED')
            """);

            // Seed order items
            stmt.execute("""
                INSERT INTO order_items VALUES
                ('ORD001', '100 00001', 250),
                ('ORD001', '100 00002', 100)
            """);

            // Seed a campaign active in 2025
            stmt.execute("""
                INSERT INTO campaigns VALUES
                ('Camp 05', '2025-03-01T00:00:00', '2025-03-31T23:59:59', 'FIXED', 'March Campaign', 3000)
            """);

            // Seed campaign items
            stmt.execute("""
                INSERT INTO campaign_items VALUES
                ('Camp 05', '100 00002', 5.0, 2000, 50)
            """);
        }
    }
}