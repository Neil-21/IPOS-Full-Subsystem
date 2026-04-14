package main.ui;

import main.service.ReportService;
import main.db.DatabaseManager;
import main.model.*;
import main.service.CampaignStore;
import main.service.PromotionService;
import main.service.CatalogueService;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class AdminDashboard extends JFrame {

    private JTextArea outputArea;
    private ReportService reportService;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            Connection conn = DatabaseManager.getConnection();
            reportService = new ReportService(conn);
            CampaignStore.loadFromDatabase(new PromotionService());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database connection failed");
            return;
        }

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));

        JButton salesBtn = new JButton("Sales Report");
        JButton campaignBtn = new JButton("Campaign Report");
        JButton createCampaignBtn = new JButton("Create Campaign");
        JButton viewCampaignsBtn = new JButton("View Campaigns");
        JButton cancelCampaignBtn = new JButton("Terminate Early");
        JButton deleteCampaignBtn = new JButton("Delete Campaign");
        JButton modifyCampaignBtn = new JButton("Modify Campaign");
        JButton engagementBtn = new JButton("Campaign Engagement");
        JButton viewDbBtn = new JButton("View Users DB");
        JButton logoutBtn = new JButton("Logout");
        JButton printButton = new JButton("Print Report");

        styleButton(salesBtn);
        styleButton(campaignBtn);
        styleButton(createCampaignBtn);
        styleButton(viewCampaignsBtn);
        styleButton(cancelCampaignBtn);
        styleButton(deleteCampaignBtn);
        styleButton(modifyCampaignBtn);
        styleButton(engagementBtn);
        styleButton(viewDbBtn);
        styleButton(logoutBtn);
        styleButton(printButton);

        topPanel.add(salesBtn);
        topPanel.add(campaignBtn);
        topPanel.add(createCampaignBtn);
        topPanel.add(viewCampaignsBtn);
        topPanel.add(cancelCampaignBtn);
        topPanel.add(deleteCampaignBtn);
        topPanel.add(modifyCampaignBtn);
        topPanel.add(engagementBtn);
        topPanel.add(viewDbBtn);
        topPanel.add(logoutBtn);
        topPanel.add(printButton);

        topPanel.setPreferredSize(new Dimension(900, 70));
        add(topPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        salesBtn.addActionListener(e -> generateSalesReport());
        campaignBtn.addActionListener(e -> generateCampaignReport());
        createCampaignBtn.addActionListener(e -> openCreateCampaignDialog());
        viewCampaignsBtn.addActionListener(e -> viewCampaigns());
        cancelCampaignBtn.addActionListener(e -> cancelCampaign());
        deleteCampaignBtn.addActionListener(e -> deleteCampaign());
        modifyCampaignBtn.addActionListener(e -> modifyCampaign());
        engagementBtn.addActionListener(e -> generateEngagementReport());
        viewDbBtn.addActionListener(e -> new DatabaseViewer().setVisible(true));
        logoutBtn.addActionListener(e -> {
            new IPOS_PU_GUI().setVisible(true);
            dispose();
        });
        printButton.addActionListener(e -> {
            try {
                boolean printed = outputArea.print();

                if (!printed) {
                    JOptionPane.showMessageDialog(this, "Printing cancelled.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error printing: " + ex.getMessage());
            }
        });
    }

    private void styleButton(JButton button) {
        button.setPreferredSize(new Dimension(150, 30));
    }

    private void generateSalesReport() {
        try {
            LocalDate start = LocalDate.now().minusDays(30);
            LocalDate end = LocalDate.now();

            SalesReport report = reportService.generateSalesReport(start, end);
            outputArea.setText(report.toString());

        } catch (Exception e) {
            outputArea.setText("Error generating sales report:\n" + e.getMessage());
        }
    }

    private void generateCampaignReport() {
        try {
            LocalDate start = LocalDate.now().minusDays(30);
            LocalDate end = LocalDate.now();

            CampaignsReport report = reportService.generateCampaignsReport(start, end);
            outputArea.setText(report.toString());

        } catch (Exception e) {
            outputArea.setText("Error generating campaign report:\n" + e.getMessage());
        }
    }

    private void openCreateCampaignDialog() {

        JTextField idField = new JTextField();
        JTextField startField = new JTextField("2026-04-01");
        JTextField endField = new JTextField("2026-04-30");
        JTextField discountTypeField = new JTextField("Percentage");
        JTextField itemsField = new JTextField("10000001:15, 40000001:10");
//        JTextField itemsField = new JTextField("PARA001:15, VIT003:10");

        JPanel panel = new JPanel(new GridLayout(0, 1));

        panel.add(new JLabel("Campaign ID:"));
        panel.add(idField);

        panel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        panel.add(startField);

        panel.add(new JLabel("End Date (YYYY-MM-DD):"));
        panel.add(endField);

        panel.add(new JLabel("Discount Type:"));
        panel.add(discountTypeField);

        panel.add(new JLabel("Items (PRODUCT_ID:DISCOUNT, comma separated):"));
        panel.add(itemsField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Create Campaign",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                String id = idField.getText().trim();

                if (id.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Campaign ID is required.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate startDate = LocalDate.parse(startField.getText().trim());
                LocalDate endDate = LocalDate.parse(endField.getText().trim());

                if (endDate.isBefore(startDate)) {
                    JOptionPane.showMessageDialog(this,
                            "End date must be after start date.",
                            "Invalid Dates",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(23, 59);

                String discountType = discountTypeField.getText().trim();
                List<CampaignItem> items = buildCampaignItemsFromInput(itemsField.getText());

                if (items.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please include at least one campaign item.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String conflictMessage = detectConflictMessage(null, start, end, items);
                if (conflictMessage != null) {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            conflictMessage + "\n\nContinue anyway? The storefront will apply the better discount.",
                            "Campaign Conflict",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                Campaign campaign = new Campaign(
                        id,
                        start,
                        end,
                        discountType,
                        items,
                        false
                );

                CampaignStore.addCampaign(campaign);
                saveCampaignToDatabase(campaign);

                JOptionPane.showMessageDialog(this,
                        "Campaign created:\n" + campaign.getCampaignId());

                viewCampaigns();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid input.\n\nUse:\n" +
                                "- dates like 2026-04-01\n" +
                                "- items like PARA001:15, VIT003:10",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private List<CampaignItem> buildCampaignItemsFromInput(String itemInput) {
        List<CampaignItem> items = new ArrayList<>();

        if (itemInput == null || itemInput.trim().isEmpty()) {
            return items;
        }

        String cleaned = itemInput.replace("\n", " ");
        String[] entries = cleaned.split(",");

        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Each item must be PRODUCT_ID:DISCOUNT");
            }

            String itemId = parts[0].trim();
            double discountRate = Double.parseDouble(parts[1].trim());

            if (discountRate < 0 || discountRate > 100) {
                throw new IllegalArgumentException("Discount must be between 0 and 100");
            }

            items.add(new CampaignItem(itemId, discountRate));
        }

        return items;
    }

    private void viewCampaigns() {
        List<Campaign> campaigns = CampaignStore.getAllCampaigns();

        if (campaigns.isEmpty()) {
            outputArea.setText("No campaigns available.");
            return;
        }

        StringBuilder sb = new StringBuilder("=== CURRENT CAMPAIGNS ===\n\n");

        for (Campaign c : campaigns) {
            sb.append("Campaign ID: ").append(c.getCampaignId()).append("\n");
            sb.append("Start: ").append(c.getStartDateTime().toLocalDate()).append("\n");
            sb.append("End: ").append(c.getEndDateTime().toLocalDate()).append("\n");
            sb.append("Discount Type: ").append(c.getDiscountType()).append("\n");
            sb.append("Cancelled: ").append(c.isCancelled()).append("\n");
            sb.append("Active: ").append(c.isActive()).append("\n");
            sb.append("Items:\n");

            for (CampaignItem item : c.getItems()) {
                sb.append(" - ")
                        .append(getProductName(item.getItemId()))
//                        .append(item.getItemId())
                        .append(" : ")
                        .append(item.getDiscountRate())
                        .append("% off\n");
            }

            sb.append("\n");
        }

        outputArea.setText(sb.toString());
    }

    private String getProductName(String productId) {
        CatalogueService catalogueService = new CatalogueService();

        return catalogueService.getAllProducts().stream()
                .filter(p -> p.getId().equalsIgnoreCase(productId))
                .map(Product::getName)
                .findFirst()
                .orElse(productId);
    }

    private void cancelCampaign() {
        String id = JOptionPane.showInputDialog(this, "Enter Campaign ID to terminate early:");

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        Campaign c = CampaignStore.findById(id.trim());

        if (c == null) {
            JOptionPane.showMessageDialog(this,
                    "Campaign not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        c.cancel();
        markCampaignCancelledInDatabase(c.getCampaignId());

        JOptionPane.showMessageDialog(this,
                "Campaign terminated early:\n" + c.getCampaignId());

        viewCampaigns();

    }

    private void deleteCampaign() {
        String id = JOptionPane.showInputDialog(this, "Enter Campaign ID to delete:");

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        boolean removed = CampaignStore.removeCampaign(id.trim());

        if (!removed) {
            JOptionPane.showMessageDialog(this,
                    "Campaign not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        deleteCampaignFromDatabase(id.trim());

        JOptionPane.showMessageDialog(this,
                "Campaign deleted: " + id.trim());

        viewCampaigns();
    }

    private void modifyCampaign() {
        String id = JOptionPane.showInputDialog(this, "Enter Campaign ID to modify:");

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        Campaign existing = CampaignStore.findById(id.trim());

        if (existing == null) {
            JOptionPane.showMessageDialog(this,
                    "Campaign not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField startField = new JTextField(existing.getStartDateTime().toLocalDate().toString());
        JTextField endField = new JTextField(existing.getEndDateTime().toLocalDate().toString());
        JTextField discountTypeField = new JTextField(existing.getDiscountType());

        StringBuilder itemsText = new StringBuilder();
        for (CampaignItem item : existing.getItems()) {
            if (!itemsText.isEmpty()) {
                itemsText.append(", ");
            }
            itemsText.append(item.getItemId()).append(":").append(item.getDiscountRate());
        }

        JTextField itemsField = new JTextField(itemsText.toString());
//        JTextArea itemsArea = new JTextArea(5, 25);
//        itemsArea.setText(itemsText.toString());

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        panel.add(startField);

        panel.add(new JLabel("End Date (YYYY-MM-DD):"));
        panel.add(endField);

        panel.add(new JLabel("Discount Type:"));
        panel.add(discountTypeField);

        panel.add(new JLabel("Items (PRODUCT_ID:DISCOUNT, comma separated):"));
        panel.add(itemsField);
//        panel.add(new JScrollPane(itemsArea));

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Modify Campaign: " + existing.getCampaignId(),
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalDate startDate = LocalDate.parse(startField.getText().trim());
                LocalDate endDate = LocalDate.parse(endField.getText().trim());

                if (endDate.isBefore(startDate)) {
                    JOptionPane.showMessageDialog(this,
                            "End date must be after start date.",
                            "Invalid Dates",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(23, 59);
                String discountType = discountTypeField.getText().trim();
                List<CampaignItem> items = buildCampaignItemsFromInput(itemsField.getText());
//                List<CampaignItem> items = buildCampaignItemsFromInput(itemsArea.getText());

                String conflictMessage = detectConflictMessage(existing.getCampaignId(), start, end, items);
                if (conflictMessage != null) {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            conflictMessage + "\n\nContinue anyway? The storefront will apply the better discount.",
                            "Campaign Conflict",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                CampaignStore.removeCampaign(existing.getCampaignId());

                Campaign updated = new Campaign(
                        existing.getCampaignId(),
                        start,
                        end,
                        discountType,
                        items,
                        existing.isCancelled()
                );

                CampaignStore.addCampaign(updated);
                updateCampaignInDatabase(updated);

                JOptionPane.showMessageDialog(this,
                        "Campaign updated: " + updated.getCampaignId());

                viewCampaigns();


            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid input.\n\nUse:\n" +
                                "- dates like 2026-04-01\n" +
                                "- items like PARA001:15, VIT003:10",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String detectConflictMessage(String currentCampaignId,
                                         LocalDateTime newStart,
                                         LocalDateTime newEnd,
                                         List<CampaignItem> newItems) {

        for (Campaign existing : CampaignStore.getAllCampaigns()) {

            if (currentCampaignId != null &&
                    existing.getCampaignId().equalsIgnoreCase(currentCampaignId)) {
                continue;
            }

            boolean overlaps = !(newEnd.isBefore(existing.getStartDateTime())
                    || newStart.isAfter(existing.getEndDateTime()));

            if (!overlaps || existing.isCancelled()) {
                continue;
            }

            for (CampaignItem newItem : newItems) {
                for (CampaignItem existingItem : existing.getItems()) {
                    if (newItem.getItemId().equalsIgnoreCase(existingItem.getItemId())) {
                        return "Conflict detected.\n" +
                                "Product " + newItem.getItemId() +
                                " is already in campaign " + existing.getCampaignId() +
                                " during an overlapping period.\n" +
                                "Existing discount: " + existingItem.getDiscountRate() + "%\n" +
                                "New discount: " + newItem.getDiscountRate() + "%";
                    }
                }
            }
        }

        return null;
    }

    private void generateEngagementReport() {
        String campaignId = JOptionPane.showInputDialog(this, "Enter Campaign ID:");

        if (campaignId == null || campaignId.trim().isEmpty()) return;

        try {
            CampaignEngagementReport report =
                    reportService.generateCampaignEngagementReport(campaignId);

            outputArea.setText(report.toString());

        } catch (Exception e) {
            outputArea.setText("Error:\n" + e.getMessage());
        }
    }

    private void saveCampaignToDatabase(Campaign campaign) {
        String insertCampaign = """
            INSERT INTO campaigns (campaign_id, start_date, end_date, discount_type, cancelled)
            VALUES (?, ?, ?, ?, ?)
        """;

        String insertCampaignItem = """
            INSERT INTO campaign_items (campaign_id, item_id, discount_rate)
            VALUES (?, ?, ?)
        """;

        String insertCampaignMetrics = """
            INSERT INTO campaign_metrics (campaign_id, campaign_hits)
            VALUES (?, 0)
        """;

        String insertCampaignItemMetrics = """
            INSERT INTO campaign_item_metrics (campaign_id, item_id, item_hits, item_purchases)
            VALUES (?, ?, 0, 0)
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement campaignStmt = conn.prepareStatement(insertCampaign);
                 PreparedStatement itemStmt = conn.prepareStatement(insertCampaignItem);
                 PreparedStatement campaignMetricsStmt = conn.prepareStatement(insertCampaignMetrics);
                 PreparedStatement itemMetricsStmt = conn.prepareStatement(insertCampaignItemMetrics)) {

                campaignStmt.setString(1, campaign.getCampaignId());
                campaignStmt.setString(2, campaign.getStartDateTime().toString());
                campaignStmt.setString(3, campaign.getEndDateTime().toString());
                campaignStmt.setString(4, campaign.getDiscountType());
                campaignStmt.setInt(5, campaign.isCancelled() ? 1 : 0);
                campaignStmt.executeUpdate();

                for (CampaignItem item : campaign.getItems()) {
                    itemStmt.setString(1, campaign.getCampaignId());
                    itemStmt.setString(2, item.getItemId());
                    itemStmt.setDouble(3, item.getDiscountRate());
                    itemStmt.addBatch();
                }

                itemStmt.executeBatch();

                campaignMetricsStmt.setString(1, campaign.getCampaignId());
                campaignMetricsStmt.executeUpdate();

                for (CampaignItem item : campaign.getItems()) {
                    itemMetricsStmt.setString(1, campaign.getCampaignId());
                    itemMetricsStmt.setString(2, item.getItemId());
                    itemMetricsStmt.addBatch();
                }

                itemMetricsStmt.executeBatch();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save campaign to database:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCampaignInDatabase(Campaign campaign) {
        String updateCampaign = """
            UPDATE campaigns
            SET start_date = ?, end_date = ?, discount_type = ?, cancelled = ?
            WHERE campaign_id = ?
        """;

        String deleteItems = """
            DELETE FROM campaign_items
            WHERE campaign_id = ?
        """;

        String insertCampaignItem = """
            INSERT INTO campaign_items (campaign_id, item_id, discount_rate)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement campaignStmt = conn.prepareStatement(updateCampaign);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteItems);
                 PreparedStatement itemStmt = conn.prepareStatement(insertCampaignItem)) {

                campaignStmt.setString(1, campaign.getStartDateTime().toString());
                campaignStmt.setString(2, campaign.getEndDateTime().toString());
                campaignStmt.setString(3, campaign.getDiscountType());
                campaignStmt.setInt(4, campaign.isCancelled() ? 1 : 0);
                campaignStmt.setString(5, campaign.getCampaignId());
                campaignStmt.executeUpdate();

                deleteStmt.setString(1, campaign.getCampaignId());
                deleteStmt.executeUpdate();

                for (CampaignItem item : campaign.getItems()) {
                    itemStmt.setString(1, campaign.getCampaignId());
                    itemStmt.setString(2, item.getItemId());
                    itemStmt.setDouble(3, item.getDiscountRate());
                    itemStmt.addBatch();
                }

                itemStmt.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to update campaign in database:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markCampaignCancelledInDatabase(String campaignId) {
        String sql = """
            UPDATE campaigns
            SET cancelled = 1
            WHERE campaign_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, campaignId);
            ps.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to cancel campaign in database:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCampaignFromDatabase(String campaignId) {
        String deleteItems = "DELETE FROM campaign_items WHERE campaign_id = ?";
        String deleteCampaign = "DELETE FROM campaigns WHERE campaign_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement itemStmt = conn.prepareStatement(deleteItems);
                 PreparedStatement campaignStmt = conn.prepareStatement(deleteCampaign)) {

                itemStmt.setString(1, campaignId);
                itemStmt.executeUpdate();

                campaignStmt.setString(1, campaignId);
                campaignStmt.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to delete campaign from database:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}