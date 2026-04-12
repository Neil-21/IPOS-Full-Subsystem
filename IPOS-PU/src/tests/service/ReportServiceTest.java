package tests.service;

import main.model.SalesReport;
import main.model.CampaignsReport;
import main.model.CampaignEngagementReport;
import main.model.CampaignEngagementRow;
import main.model.CampaignReportItem;
import main.model.CampaignSoldItem;
import main.service.ReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportServiceTest {

    private Connection connection;
    private ReportService reportService;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        reportService = new ReportService(connection);
        createTablesAndSeedData();
    }

    // Expected: returns a non-empty sales report for a valid date range with seeded data.
    @Test
    void testGenerateSalesReport_ValidDateRange_ReturnsSalesData() throws SQLException {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 12, 31);

        SalesReport report = reportService.generateSalesReport(start, end);

        assertNotNull(report, "Report should not be null");
        assertFalse(report.isEmpty(), "Report should contain sales data");
        assertTrue(report.getTotalUnitsSold() > 0, "Total units sold should be greater than 0");
        assertTrue(report.getTotalRevenue() > 0, "Total revenue should be greater than 0");
        assertEquals(start, report.getStartDate(), "Start date should match input");
        assertEquals(end,   report.getEndDate(),   "End date should match input");
    }

    // Expected: includes only 'Received' orders and excludes other statuses.
    @Test
    void testGenerateSalesReport_ExcludesNonReceivedOrders() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                INSERT INTO orders VALUES
                ('ORD002', 'customer@ipos.com', '2025-05-16', 1, 'Pending', 100.00)
            """);
            stmt.execute("""
                INSERT INTO order_items VALUES
                ('ORD002', '100 00003', 'Vitamin C', 500, 0.20, 100.00, NULL)
            """);
        }

        SalesReport report = reportService.generateSalesReport(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        assertEquals(350, report.getTotalUnitsSold(), "Pending order should be excluded from totals.");
        assertEquals(75.0, report.getTotalRevenue(), 0.0001, "Pending order revenue should be excluded.");
    }

    // Expected: aggregates quantities and totals when the same item appears in multiple received orders.
    @Test
    void testGenerateSalesReport_AggregatesSameProductAcrossOrders() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                INSERT INTO orders VALUES
                ('ORD003', 'customer@ipos.com', '2025-06-01', 1, 'Received', 10.00)
            """);
            stmt.execute("""
                INSERT INTO order_items VALUES
                ('ORD003', '100 00002', 'Aspirin', 20, 0.50, 10.00, 'Camp 05')
            """);
        }

        SalesReport report = reportService.generateSalesReport(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        assertEquals(370, report.getTotalUnitsSold());
        assertEquals(85.0, report.getTotalRevenue(), 0.0001);

        assertEquals(2, report.getItems().size(), "Aspirin should be aggregated into a single row.");
        assertEquals(120, findSalesItem(report, "100 00002").getQuantitySold());
    }

    // Expected: returns empty report with zero totals when no received orders exist in range.
    @Test
    void testGenerateSalesReport_NoOrdersInRange_ReturnsEmptyTotals() throws SQLException {
        SalesReport report = reportService.generateSalesReport(
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 12, 31)
        );

        assertNotNull(report);
        assertTrue(report.isEmpty());
        assertEquals(0, report.getTotalUnitsSold());
        assertEquals(0.0, report.getTotalRevenue(), 0.0001);
    }

    // Expected: throws IllegalArgumentException when start date is after end date.
    @Test
    void testGenerateSalesReport_StartDateAfterEndDate_ThrowsException() {
        LocalDate start = LocalDate.of(2025, 12, 31);
        LocalDate end   = LocalDate.of(2025, 1, 1);

        assertThrows(IllegalArgumentException.class,
                () -> reportService.generateSalesReport(start, end),
                "Should throw IllegalArgumentException when startDate is after endDate");
    }

    // Expected: throws IllegalArgumentException when both dates are null.
    @Test
    void testGenerateSalesReport_NullDates_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.generateSalesReport(null, null),
                "Should throw IllegalArgumentException when dates are null");
    }

    // Expected: returns a non-empty campaigns report for a valid date range with seeded data.
    @Test
    void testGenerateCampaignsReport_ValidDateRange_ReturnsCampaignData() throws SQLException {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 12, 31);

        CampaignsReport report = reportService.generateCampaignsReport(start, end);

        assertNotNull(report, "Report should not be null");
        assertFalse(report.isEmpty(), "Report should contain campaign data");
        assertEquals(start, report.getStartDate(), "Start date should match input");
        assertEquals(end,   report.getEndDate(),   "End date should match input");
    }

    // Expected: report includes campaign item rows with sold quantities and campaign total.
    @Test
    void testGenerateCampaignsReport_ReturnsExpectedSoldItemMetrics() throws SQLException {
        CampaignsReport report = reportService.generateCampaignsReport(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        assertFalse(report.getCampaigns().isEmpty());
        CampaignReportItem campaign = report.getCampaigns().get(0);
        assertEquals("Camp 05", campaign.getCampaignId());
        assertEquals(1, campaign.getSoldItems().size());

        CampaignSoldItem soldItem = campaign.getSoldItems().get(0);
        assertEquals("100 00002", soldItem.getItemId());
        assertEquals(100, soldItem.getItemsSold());
        assertEquals(50.0, soldItem.getTotalSales(), 0.0001);
        assertEquals(50.0, campaign.getTotalCampaignSales(), 0.0001);
    }

    // Expected: date-only campaign strings parse to start-of-day and end-of-day boundaries.
    @Test
    void testGenerateCampaignsReport_DateOnlyCampaignDates_ParseCorrectly() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                INSERT INTO campaigns VALUES
                ('Camp DateOnly', '2025-02-01', '2025-02-10', 'PERCENTAGE', 0)
            """);
            stmt.execute("""
                INSERT INTO campaign_items VALUES
                ('Camp DateOnly', '100 00001', 3.0)
            """);
        }

        CampaignsReport report = reportService.generateCampaignsReport(
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 2, 10)
        );

        CampaignReportItem dateOnlyCampaign = report.getCampaigns().stream()
                .filter(c -> "Camp DateOnly".equals(c.getCampaignId()))
                .findFirst()
                .orElseThrow();

        assertEquals(LocalDateTime.of(2025, 2, 1, 0, 0), dateOnlyCampaign.getStartDateTime());
        assertEquals(LocalDateTime.of(2025, 2, 10, 23, 59), dateOnlyCampaign.getEndDateTime());
    }

    // Expected: throws IllegalArgumentException when campaigns report range is invalid.
    @Test
    void testGenerateCampaignsReport_InvalidDateRange_ThrowsException() {
        LocalDate start = LocalDate.of(2025, 12, 31);
        LocalDate end   = LocalDate.of(2025, 1, 1);

        assertThrows(IllegalArgumentException.class,
                () -> reportService.generateCampaignsReport(start, end),
                "Should throw IllegalArgumentException when startDate is after endDate");
    }

    // Expected: returns an empty campaigns report when no campaigns exist in the period.
    @Test
    void testGenerateCampaignsReport_NoCampaignsInPeriod_ReturnsEmptyReport() throws SQLException {
        LocalDate start = LocalDate.of(2099, 1, 1);
        LocalDate end   = LocalDate.of(2099, 12, 31);

        CampaignsReport report = reportService.generateCampaignsReport(start, end);

        assertNotNull(report, "Report should not be null even if empty");
        assertTrue(report.isEmpty(), "Report should be empty when no campaigns exist in period");
        assertEquals(0, report.getActiveCampaignCount(), "Active campaign count should be 0");
    }

    // Expected: returns campaign engagement rows with seeded campaign and item metrics.
    @Test
    void testGenerateCampaignEngagementReport_ValidCampaign_ReturnsEngagementData() throws SQLException {
        CampaignEngagementReport report = reportService.generateCampaignEngagementReport("Camp 05");

        assertNotNull(report);
        assertEquals("Camp 05", report.getCampaignId());
        assertEquals("FIXED", report.getCampaignDescription());
        assertEquals(2, report.getRows().size(), "Expected campaign row + one item row.");

        List<CampaignEngagementRow> rows = report.getRows();
        CampaignEngagementRow campaignRow = rows.get(0);
        assertEquals("Campaign", campaignRow.getCounterId());
        assertEquals(120, campaignRow.getHitsCount());
        assertEquals(0, campaignRow.getpurchases());

        CampaignEngagementRow firstItemRow = rows.get(1);
        assertEquals("Item(1)", firstItemRow.getCounterId());
        assertEquals(40, firstItemRow.getHitsCount());
        assertEquals(10, firstItemRow.getpurchases());
    }

    // Expected: engagement report metadata is parsed correctly from campaign row.
    @Test
    void testGenerateCampaignEngagementReport_ContainsCampaignMetadata() throws SQLException {
        CampaignEngagementReport report = reportService.generateCampaignEngagementReport("Camp 05");

        assertEquals(LocalDateTime.of(2025, 3, 1, 0, 0), report.getStartDateTime());
        assertEquals(LocalDateTime.of(2025, 3, 31, 23, 59), report.getEndDateTime());
        assertFalse(report.isEmpty());
    }

    // Expected: missing campaign-metrics row defaults campaign hits to zero while item metrics stay intact.
    @Test
    void testGenerateCampaignEngagementReport_MissingCampaignMetrics_DefaultsCampaignHitsOnly() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM campaign_metrics WHERE campaign_id = 'Camp 05'");
        }

        CampaignEngagementReport report = reportService.generateCampaignEngagementReport("Camp 05");

        assertNotNull(report);
        assertEquals(2, report.getRows().size());
        assertEquals(0, report.getRows().get(0).getHitsCount(), "Campaign hits should default to 0.");
        assertEquals(40, report.getRows().get(1).getHitsCount(), "Item hits should remain from existing metrics.");
        assertEquals(10, report.getRows().get(1).getpurchases(), "Item purchases should remain from existing metrics.");
    }

    // Expected: throws IllegalArgumentException when campaign ID is null or blank.
    @Test
    void testGenerateCampaignEngagementReport_NullOrBlankCampaignId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> reportService.generateCampaignEngagementReport(null));
        assertThrows(IllegalArgumentException.class, () -> reportService.generateCampaignEngagementReport(" "));
    }

    // Expected: throws IllegalArgumentException when campaign does not exist.
    @Test
    void testGenerateCampaignEngagementReport_MissingCampaign_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.generateCampaignEngagementReport("MISSING-CAMPAIGN"));
    }

    // Expected: defaults metrics to zero when metric rows are missing.
    @Test
    void testGenerateCampaignEngagementReport_MissingMetrics_DefaultsToZero() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM campaign_metrics WHERE campaign_id = 'Camp 05'");
            stmt.execute("DELETE FROM campaign_item_metrics WHERE campaign_id = 'Camp 05'");
        }

        CampaignEngagementReport report = reportService.generateCampaignEngagementReport("Camp 05");

        assertNotNull(report);
        assertEquals(2, report.getRows().size());
        assertEquals(0, report.getRows().get(0).getHitsCount(), "Campaign hits should default to 0.");
        assertEquals(0, report.getRows().get(1).getHitsCount(), "Item hits should default to 0.");
        assertEquals(0, report.getRows().get(1).getpurchases(), "Item purchases should default to 0.");
    }

    private void createTablesAndSeedData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    email       TEXT PRIMARY KEY,
                    full_name   TEXT NOT NULL,
                    password    TEXT NOT NULL,
                    role        TEXT NOT NULL,
                    first_login INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id     TEXT PRIMARY KEY,
                    user_email   TEXT NOT NULL,
                    order_date   TEXT NOT NULL,
                    item_count   INTEGER NOT NULL,
                    status       TEXT NOT NULL,
                    total_amount REAL NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    order_id     TEXT NOT NULL,
                    product_id   TEXT NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity     INTEGER NOT NULL,
                    unit_price   REAL NOT NULL,
                    line_total   REAL NOT NULL,
                    campaign_id  TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS campaigns (
                    campaign_id   TEXT PRIMARY KEY,
                    start_date    TEXT NOT NULL,
                    end_date      TEXT NOT NULL,
                    discount_type TEXT NOT NULL,
                    cancelled     INTEGER NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS campaign_items (
                    campaign_id   TEXT NOT NULL,
                    item_id       TEXT NOT NULL,
                    discount_rate REAL NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS campaign_metrics (
                    campaign_id   TEXT PRIMARY KEY,
                    campaign_hits INTEGER NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS campaign_item_metrics (
                    campaign_id    TEXT NOT NULL,
                    item_id        TEXT NOT NULL,
                    item_hits      INTEGER NOT NULL DEFAULT 0,
                    item_purchases INTEGER NOT NULL DEFAULT 0
                )
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO users VALUES
                ('customer@ipos.com', 'Test Customer', 'Test123!', 'CUSTOMER', 1)
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO orders VALUES
                ('ORD001', 'customer@ipos.com', '2025-05-15', 2, 'Received', 75.00)
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO order_items VALUES
                ('ORD001', '100 00001', 'Paracetamol', 250, 0.10, 25.00, NULL),
                ('ORD001', '100 00002', 'Aspirin',     100, 0.50, 50.00, 'Camp 05')
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO campaigns VALUES
                ('Camp 05', '2025-03-01', '2025-03-31', 'FIXED', 0)
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO campaign_items VALUES
                ('Camp 05', '100 00002', 5.0)
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO campaign_metrics VALUES
                ('Camp 05', 120)
            """);

            //noinspection SqlResolve
            stmt.execute("""
                INSERT INTO campaign_item_metrics VALUES
                ('Camp 05', '100 00002', 40, 10)
            """);
        }
    }

    private main.model.SalesReportItem findSalesItem(SalesReport report, String itemId) {
        return report.getItems().stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing expected item in sales report: " + itemId));
    }
}
