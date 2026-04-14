package main.ui;

import main.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class WelcomeFrame extends JFrame {

    private final AuthService authService;

    public WelcomeFrame(AuthService authService) {
        this.authService = authService;

        setTitle("IPOS-PU Welcome");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 420);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JLabel title = new JLabel("Welcome to T-12 Applications", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        root.add(title, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 10, 10));

        JButton customerLoginButton = new JButton("Login as Customer");
        JButton adminLoginButton = new JButton("Login as Admin");
        JButton nonCommercialButton = new JButton("Register Non-Commercial Membership");
        JButton commercialButton = new JButton("Apply for Commercial Membership");
        JButton backButton = new JButton("Back to Shop");
        JButton exitButton = new JButton("Exit");

        buttonPanel.add(customerLoginButton);
        buttonPanel.add(adminLoginButton);
        buttonPanel.add(nonCommercialButton);
        buttonPanel.add(commercialButton);
        buttonPanel.add(backButton);
        buttonPanel.add(exitButton);

        root.add(buttonPanel, BorderLayout.CENTER);

        customerLoginButton.addActionListener(e -> {
            new CustomerLoginFrame(authService).setVisible(true);
            dispose();
        });

        adminLoginButton.addActionListener(e -> {
            new AdminLoginFrame(authService).setVisible(true);
            dispose();
        });

        nonCommercialButton.addActionListener(e -> {
            new NonCommercialRegistrationFrame(authService).setVisible(true);
            dispose();
        });

        commercialButton.addActionListener(e -> {
            new CommercialApplicationFrame(authService).setVisible(true);
            dispose();
        });

        backButton.addActionListener(e -> {
            new IPOS_PU_GUI(authService, null).setVisible(true);
            dispose();
        });

        exitButton.addActionListener(e -> this.dispose());
    }
}