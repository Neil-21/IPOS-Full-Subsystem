package main.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;

import main.service.AuthService;
import main.model.User;
import main.model.Campaign;
import main.model.CampaignItem;
import main.service.CampaignStore;
import main.ui.WelcomeFrame;
import main.db.DatabaseManager;
import main.service.ReportService;


/**
 * IPOS-PU Desktop GUI Prototype
 * Public-facing online pharmacy portal for members of the public.
 * Features: Browse catalogue, filter, promotions, shopping cart, checkout (simulated), track orders.
 * Simple, clean, accessible Swing UI - consistent layout, clear labels, tooltips.
 */
public class IPOS_PU_GUI extends JFrame {

    // Fields
    private final List<Product> catalogue = new ArrayList<>();
    private final List<Promotion> activePromotions = new ArrayList<>();
    private final List<CartItem> shoppingCart = new ArrayList<>();
    private final List<Order> myOrders = new ArrayList<>();

    private JTable productTable;
    private JTable cartTable;
    private DefaultTableModel productTableModel;
    private DefaultTableModel cartTableModel;
    private JLabel cartTotalLabel;
    private JTextField searchField;
    private JTabbedPane mainTabs;
    private JButton cartBtn;
    private AuthService authService;
    private User currentUser;
    private JButton loginBtn;
    private ReportService reportService;
    private JTable ordersTable;
    private DefaultTableModel ordersTableModel;

    private int completedOrderCount = 0;

    public IPOS_PU_GUI() {
        this(new AuthService(), null);
    }

    public IPOS_PU_GUI(AuthService authService, User user) {
        super("IPOS - Public Online Pharmacy");

        this.authService = authService;
        this.currentUser = user;

        if (currentUser != null) {
            completedOrderCount = 0;
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        cartTotalLabel = new JLabel("Total: £0.00");
        cartTotalLabel.setFont(new Font("Arial", Font.BOLD, 16));

        loadSampleData();
        createHeader();

        mainTabs = new JTabbedPane();
        mainTabs.addTab("Browse Catalogue", createBrowsePanel());
        mainTabs.addTab("Promotions", createPromotionsPanel());
        mainTabs.addTab("Shopping Cart", createCartPanel());
        mainTabs.addTab("My Orders", createOrdersPanel());
        mainTabs.addTab("Membership", createMembershipPanel());

        add(mainTabs, BorderLayout.CENTER);

        JLabel status = new JLabel(" Welcome to IPOS-PU • Connected to merchant stock • " + LocalDateTime.now().toLocalDate());
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        add(status, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadSampleData() {
        // Catalogue
        catalogue.add(new Product("PARA001", "Paracetamol 500mg (16 tablets)", 2.99, 120, "Pain relief"));
        catalogue.add(new Product("IBU002", "Ibuprofen 400mg (24 tablets)", 4.49, 85, "Anti-inflammatory"));
        catalogue.add(new Product("VIT003", "Vitamin D3 1000IU (90 capsules)", 6.99, 200, "Supplements"));
        catalogue.add(new Product("ALL004", "Allergy Relief (Cetirizine 10mg)", 3.79, 45, "Antihistamine"));
        catalogue.add(new Product("BAND005", "Bandages & Plasters Pack", 5.49, 30, "First Aid"));

    }

    private void createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 102, 204));
        header.setPreferredSize(new Dimension(0, 60));

        JLabel title = new JLabel("IPOS-PU - T12 Solutions");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        header.add(title, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);

        loginBtn = new JButton(currentUser == null ? "Login / Register" : "Logout");

        loginBtn.addActionListener(e -> {
            if (currentUser == null) {
                new WelcomeFrame(authService).setVisible(true);
            } else {
                new IPOS_PU_GUI(authService, null).setVisible(true);
            }
            dispose();
        });
        rightPanel.add(loginBtn);

        cartBtn = new JButton();
        updateCartButton();
        cartBtn.addActionListener(e -> mainTabs.setSelectedIndex(2)); // switch to cart tab
        rightPanel.add(cartBtn);

        header.add(rightPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private JPanel createBrowsePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(25);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(this::performSearch);
        searchPanel.add(new JLabel("Search products:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        panel.add(searchPanel, BorderLayout.NORTH);

        // Product table
        String[] columns = {"ID", "Product", "Price (£)", "Stock", "Category"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        productTable = new JTable(productTableModel);
        productTable.setRowHeight(28);
        productTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(300);

        refreshProductTable(catalogue);

        JScrollPane scroll = new JScrollPane(productTable);
        panel.add(scroll, BorderLayout.CENTER);

        // Add to cart button
        JButton addToCartBtn = new JButton("Add Selected to Cart");
        addToCartBtn.addActionListener(e -> addSelectedToCart());
        panel.add(addToCartBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void performSearch(ActionEvent e) {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            refreshProductTable(catalogue);
            return;
        }
        List<Product> filtered = catalogue.stream()
                .filter(p -> p.name.toLowerCase().contains(keyword) || p.category.toLowerCase().contains(keyword))
                .toList();
        refreshProductTable(filtered);
    }

    private void refreshProductTable(List<Product> products) {
        productTableModel.setRowCount(0);
        for (Product p : products) {
            double displayPrice = getEffectivePrice(p);

            productTableModel.addRow(new Object[]{
                    p.id, p.name, String.format("%.2f", displayPrice), getAvailableStock(p), p.category
            });
        }
    }

    private void addSelectedToCart() {
        int row = productTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product first.");
            return;
        }

        String id = (String) productTableModel.getValueAt(row, 0);
        Product product = catalogue.stream()
                .filter(p -> p.id.equals(id))
                .findFirst()
                .orElse(null);

        if (product == null) {
            JOptionPane.showMessageDialog(this, "Selected product could not be found.");
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this, "Quantity for " + product.name + "?", "1");
        if (qtyStr == null) {
            return;
        }

        try {
            int qtyToAdd = Integer.parseInt(qtyStr.trim());

            if (qtyToAdd < 1) {
                JOptionPane.showMessageDialog(this, "Quantity must be at least 1.");
                return;
            }

            int availableStock = getAvailableStock(product);

            if (qtyToAdd > availableStock) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot add " + qtyToAdd + " of this product.\n" +
                                "Only " + availableStock + " more can be added."
                );
                return;
            }

            CartItem existingItem = findCartItem(product);

            if (existingItem != null) {
                existingItem.quantity += qtyToAdd;
            } else {
                shoppingCart.add(new CartItem(product, qtyToAdd));
            }

            refreshCartTable();
            updateCartButton();
            refreshBrowseView();

            JOptionPane.showMessageDialog(this, qtyToAdd + " × " + product.name + " added to cart!");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a whole number.");
        }
    }

    private double getEffectivePrice(Product product) {
        double bestPrice = product.price;

        for (Campaign campaign : CampaignStore.getActiveCampaigns()) {
            for (CampaignItem item : campaign.getItems()) {
                if (item.getItemId().equals(product.id)) {
                    double discounted = product.price * (1 - item.getDiscountRate() / 100.0);
                    if (discounted < bestPrice) {
                        bestPrice = discounted;
                    }
                }
            }
        }

        return bestPrice;
    }

    private int getQuantityInCart(Product product) {
        CartItem item = findCartItem(product);
        return (item == null) ? 0 : item.quantity;
    }

    private int getAvailableStock(Product product) {
        return Math.max(product.stock - getQuantityInCart(product), 0);
    }

    private void refreshBrowseView() {
        if (productTableModel == null) {
            return;
        }

        String keyword = (searchField == null) ? "" : searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            refreshProductTable(catalogue);
        } else {
            List<Product> filtered = catalogue.stream()
                    .filter(p -> p.name.toLowerCase().contains(keyword) || p.category.toLowerCase().contains(keyword))
                    .toList();

            refreshProductTable(filtered);
        }

        refreshPromotionsView();
    }

    private JPanel createPromotionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea promoArea = new JTextArea();
        promoArea.setEditable(false);
        promoArea.setFont(new Font("Arial", Font.PLAIN, 14));

        StringBuilder sb = new StringBuilder("=== ACTIVE PROMOTIONS ===\n\n");

        List<Campaign> campaigns = CampaignStore.getActiveCampaigns();

        if (campaigns.isEmpty()) {
            sb.append("No active campaigns right now.");
        } else {
            for (Campaign c : campaigns) {
                sb.append("Campaign ID: ").append(c.getCampaignId()).append("\n");
                sb.append("Valid: ").append(c.getStartDateTime().toLocalDate())
                        .append(" - ").append(c.getEndDateTime().toLocalDate()).append("\n");
                sb.append("Type: ").append(c.getDiscountType()).append("\n");
                sb.append("Items:\n");

                for (CampaignItem item : c.getItems()) {
                    sb.append(" - ").append(item.getItemId())
                            .append(" : ").append(item.getDiscountRate()).append("% off\n");
                }

                sb.append("\n");
            }
        }

        promoArea.setText(sb.toString());

        panel.add(new JLabel("Current Promotions"), BorderLayout.NORTH);
        panel.add(new JScrollPane(promoArea), BorderLayout.CENTER);

        return panel;
    }

    private void refreshPromotionsView() {
        if (mainTabs != null) {
            mainTabs.setComponentAt(1, createPromotionsPanel());
        }
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {"Product", "Qty", "Unit Price", "Total"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartTableModel);

        refreshCartTable();

        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cartTotalLabel = new JLabel("Total: £0.00");
        cartTotalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bottom.add(cartTotalLabel);
        JButton decreaseBtn = new JButton("Decrease Qty");
        decreaseBtn.addActionListener(e -> decreaseSelectedCartItemQuantity());
        bottom.add(decreaseBtn);

        JButton increaseBtn = new JButton("Increase Qty");
        increaseBtn.addActionListener(e -> increaseSelectedCartItemQuantity());
        bottom.add(increaseBtn);

        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> removeSelectedCartItem());
        bottom.add(removeBtn);
        JButton checkoutBtn = new JButton("Proceed to Checkout");
        checkoutBtn.addActionListener(e -> simulateCheckout());
        bottom.add(checkoutBtn);

        JButton clearBtn = new JButton("Clear Cart");
        clearBtn.addActionListener(e -> {
            shoppingCart.clear();
            refreshCartTable();
            updateCartButton();
            refreshBrowseView();
        });
        bottom.add(clearBtn);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshCartTable() {
        if (cartTableModel == null) {
            return;
        }

        cartTableModel.setRowCount(0);

        double total = 0.0;
        for (CartItem item : shoppingCart) {
            double price = getEffectivePrice(item.product);
            double lineTotal = price * item.quantity;
            total += lineTotal;

            cartTableModel.addRow(new Object[]{
                    item.product.name,
                    item.quantity,
                    String.format("%.2f", price),
                    String.format("%.2f", lineTotal)
            });
        }

        double finalTotal = calculateCartTotalWithMemberDiscount();

        if (qualifiesForTenthOrderDiscount()) {
            cartTotalLabel.setText("Total: £" + String.format("%.2f", finalTotal) + " (includes 10% member discount)");
        } else {
            cartTotalLabel.setText("Total: £" + String.format("%.2f", finalTotal));
        }

//        cartTotalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private boolean qualifiesForTenthOrderDiscount() {
        if (currentUser == null) {
            return false;
        }

        return (completedOrderCount + 1) % 10 == 0;
    }

    private double calculateCartTotalWithMemberDiscount() {
        double total = 0.0;

        for (CartItem item : shoppingCart) {
            double price = getEffectivePrice(item.product);
            total += price * item.quantity;
        }

        if (qualifiesForTenthOrderDiscount()) {
            total = total * 0.90;
        }

        return total;
    }

    private void simulateCheckout() {
        if (shoppingCart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }

        String card = JOptionPane.showInputDialog(this, "Enter card number (demo):", "4242 4242 4242 4242");
        if (card == null || card.length() < 4) return;

        boolean tenthDiscountApplied = qualifiesForTenthOrderDiscount();
        double finalTotal = calculateCartTotalWithMemberDiscount();

        String orderId = "ORD-" + System.currentTimeMillis();
        myOrders.add(new Order(orderId, new ArrayList<>(shoppingCart), LocalDateTime.now(), "Received"));

        completedOrderCount++;

        if (tenthDiscountApplied) {
            JOptionPane.showMessageDialog(this,
                    "Payment successful!\nOrder ID: " + orderId +
                            "\nA 10% member discount was applied.\n" +
                            "Final total: £" + String.format("%.2f", finalTotal) +
                            "\nConfirmation emailed.");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Payment successful!\nOrder ID: " + orderId +
                            "\nFinal total: £" + String.format("%.2f", finalTotal) +
                            "\nConfirmation emailed.");
        }

        shoppingCart.clear();
        refreshCartTable();
        updateCartButton();
        refreshBrowseView();
        refreshOrdersTable();
        mainTabs.setSelectedIndex(3);
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Order ID", "Date", "Items", "Status"};
        ordersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ordersTable = new JTable(ordersTableModel);
        refreshOrdersTable();

        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshOrdersTable() {
        if (ordersTableModel == null) {
            return;
        }

        ordersTableModel.setRowCount(0);

        for (Order o : myOrders) {
            ordersTableModel.addRow(new Object[]{
                    o.id,
                    o.date.toLocalDate(),
                    o.items.size() + " items",
                    o.status
            });
        }
    }

    private JPanel createMembershipPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel nonComm = new JPanel();
        nonComm.setBorder(BorderFactory.createTitledBorder("Non-Commercial Member"));
        nonComm.add(new JLabel("<html><b>10% off every 10th order!</b><br>Quick email registration</html>"));
        JButton regNon = new JButton("Register (Non-Commercial)");
        regNon.addActionListener(e -> {
            new WelcomeFrame(authService).setVisible(true);
            dispose();
        });
        nonComm.add(regNon);

        JPanel comm = new JPanel();
        comm.setBorder(BorderFactory.createTitledBorder("Commercial Member"));
        comm.add(new JLabel("<html>Business / Company account<br>Application reviewed by InfoPharma</html>"));
        JButton regComm = new JButton("Apply as Commercial");
        regComm.addActionListener(e -> {
            new WelcomeFrame(authService).setVisible(true);
            dispose();
        });
        comm.add(regComm);

        panel.add(nonComm);
        panel.add(comm);
        return panel;
    }

    private CartItem findCartItem(Product product) {
        for (CartItem item : shoppingCart) {
            if (item.product.id.equals(product.id)) {
                return item;
            }
        }
        return null;
    }

    private int getSelectedCartRow() {
        if (cartTable == null) {
            return -1;
        }
        return cartTable.getSelectedRow();
    }

    private CartItem getSelectedCartItem() {
        int selectedRow = getSelectedCartRow();

        if (selectedRow < 0 || selectedRow >= shoppingCart.size()) {
            return null;
        }

        return shoppingCart.get(selectedRow);
    }

    private void removeSelectedCartItem() {
        CartItem selectedItem = getSelectedCartItem();

        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select an item in the shopping cart first.");
            return;
        }

        shoppingCart.remove(selectedItem);
        refreshCartTable();
        updateCartButton();
        refreshBrowseView();
    }

    private void increaseSelectedCartItemQuantity() {
        CartItem selectedItem = getSelectedCartItem();

        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select an item in the shopping cart first.");
            return;
        }

        int availableStock = getAvailableStock(selectedItem.product);

        if (availableStock < 1) {
            JOptionPane.showMessageDialog(this, "No more stock is available for this product.");
            return;
        }

        selectedItem.quantity += 1;
        refreshCartTable();
        updateCartButton();
        refreshBrowseView();
    }

    private void decreaseSelectedCartItemQuantity() {
        CartItem selectedItem = getSelectedCartItem();

        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select an item in the shopping cart first.");
            return;
        }

        if (selectedItem.quantity > 1) {
            selectedItem.quantity -= 1;
        } else {
            shoppingCart.remove(selectedItem);
        }

        refreshCartTable();
        updateCartButton();
        refreshBrowseView();
    }

    private void updateCartButton() {
        if (cartBtn != null) {
            cartBtn.setText("🛒 Cart (" + getCartItemCount() + ")");
        }
    }

    private int getCartItemCount() {
        int total = 0;
        for (CartItem item : shoppingCart) {
            total += item.quantity;
        }
        return total;
    }

    // ==================== Simple Model Classes ====================
    private static class Product {
        String id, name, category;
        double price;
        int stock;
        Product(String id, String name, double price, int stock, String category) {
            this.id = id; this.name = name; this.price = price; this.stock = stock; this.category = category;
        }
    }

    private static class CartItem {
        Product product;
        int quantity;
        CartItem(Product p, int q) { product = p; quantity = q; }
    }

    private static class Promotion {
        String id, name, startDate, endDate;
        List<String> items;
        double discountRate;
        Promotion(String id, String name, String start, String end, List<String> items, double rate) {
            this.id = id; this.name = name; this.startDate = start; this.endDate = end;
            this.items = items; this.discountRate = rate;
        }
    }

    private static class Order {
        String id;
        List<CartItem> items;
        LocalDateTime date;
        String status;
        Order(String id, List<CartItem> items, LocalDateTime date, String status) {
            this.id = id; this.items = items; this.date = date; this.status = status;
        }
    }

    // ==================== MAIN ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager.initialise();
                Connection conn = DatabaseManager.getConnection();
                IPOS_PU_GUI gui = new IPOS_PU_GUI();
                gui.reportService = new ReportService(conn);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Database connection failed: " + e.getMessage());
            }
        });
    }    
}
