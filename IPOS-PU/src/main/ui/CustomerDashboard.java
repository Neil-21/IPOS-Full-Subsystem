package main.ui;

import main.model.User;
import main.service.AuthService;
import main.service.CatalogueService;
import main.model.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class CustomerDashboard extends JFrame {

    private final User user;
    private final AuthService authService;
    private CardLayout cardLayout;
    private JPanel contentPanel;

    public CustomerDashboard(User user, AuthService authService) {
        this.user = user;
        this.authService = authService;

        setTitle("IPOS-PU Customer Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        JLabel header = new JLabel("IPOS-PU - Customer Portal", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.setBorder(new EmptyBorder(15, 10, 15, 10));
        root.add(header, BorderLayout.NORTH);

        JPanel sideMenu = new JPanel(new GridLayout(0, 1, 8, 8));
        sideMenu.setBorder(new EmptyBorder(10, 10, 10, 10));
        sideMenu.setPreferredSize(new Dimension(220, 0));

        JButton homeButton = new JButton("Home");
        JButton catalogueButton = new JButton("Browse Catalogue");
        JButton basketButton = new JButton("Basket");
        JButton promotionsButton = new JButton("Promotions");
        JButton trackOrdersButton = new JButton("Track Orders");
        JButton logoutButton = new JButton("Logout");

        sideMenu.add(homeButton);
        sideMenu.add(catalogueButton);
        sideMenu.add(basketButton);
        sideMenu.add(promotionsButton);
        sideMenu.add(trackOrdersButton);
        sideMenu.add(logoutButton);

        root.add(sideMenu, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        contentPanel.add(createHomePanel(), "HOME");
        contentPanel.add(createCataloguePanel(), "CATALOGUE");
        contentPanel.add(createPlaceholderPanel("Basket screen"), "BASKET");
        contentPanel.add(createPlaceholderPanel("Promotions screen"), "PROMOTIONS");
        contentPanel.add(createPlaceholderPanel("Track Orders screen"), "TRACK_ORDERS");

        root.add(contentPanel, BorderLayout.CENTER);

        homeButton.addActionListener(e -> cardLayout.show(contentPanel, "HOME"));
        catalogueButton.addActionListener(e -> cardLayout.show(contentPanel, "CATALOGUE"));
        basketButton.addActionListener(e -> cardLayout.show(contentPanel, "BASKET"));
        promotionsButton.addActionListener(e -> cardLayout.show(contentPanel, "PROMOTIONS"));
        trackOrdersButton.addActionListener(e -> cardLayout.show(contentPanel, "TRACK_ORDERS"));

        logoutButton.addActionListener(e -> {
            new WelcomeFrame(authService).setVisible(true);
            dispose();
        });
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Welcome, " + user.getEmail());
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setText("""
                You are logged in as a customer.""");

        panel.add(title, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCataloguePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Catalogue");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        CatalogueService service = new CatalogueService();
        List<Product> products = service.getAllProducts();

        String[] columns = {"ID", "Name", "Package", "Price (£)", "Stock"};
        Object[][] data = new Object[products.size()][5];

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            data[i][0] = p.getId();
            data[i][1] = p.getName();
            data[i][2] = p.getPackageType();
            data[i][3] = p.getRetailPrice();
            data[i][4] = p.getStock();
        }

//        String[] columns = {"ID", "Name", "Type", "Price (£)", "Stock"};
//        Object[][] data = new Object[products.size()][5];
//
//        for (int i = 0; i < products.size(); i++) {
//            Product p = products.get(i);
//            data[i][0] = p.getId();
//            data[i][1] = p.getName();
//            data[i][2] = p.getCategory();
//            data[i][3] = p.getPrice();
//            data[i][4] = p.getStock();
//        }

        JTable table = new JTable(data, columns);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("Add to Basket");
        addButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product.");
                return;
            }

            String productName = table.getValueAt(selectedRow, 1).toString();
            JOptionPane.showMessageDialog(this,
                    productName + " added to basket (basket logic next).");
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(addButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(message + " coming next");
        label.setFont(new Font("SansSerif", Font.PLAIN, 20));
        panel.add(label);
        return panel;
    }
}