package main.ui;

import main.model.User;
import main.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminLoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField emailField;
    private JPasswordField passwordField;

    public AdminLoginFrame(AuthService authService) {
        this.authService = authService;

        setTitle("Admin Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 260);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JLabel title = new JLabel("Admin Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));

        form.add(new JLabel("Email:"));
        emailField = new JTextField();
        form.add(emailField);

        form.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        form.add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton backButton = new JButton("Back");

        form.add(loginButton);
        form.add(backButton);

        root.add(form, BorderLayout.CENTER);

        loginButton.addActionListener(e -> handleLogin());
        backButton.addActionListener(e -> {
            new WelcomeFrame(authService).setVisible(true);
            dispose();
        });
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        User user = authService.login(email, password);

        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Invalid admin login.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        new AdminDashboard().setVisible(true);
        dispose();
//
//        if (user == null || !user.isAdmin()) {
//            JOptionPane.showMessageDialog(this,
//                    "Invalid admin login.",
//                    "Login Failed",
//                    JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        new AdminDashboard(user, authService).setVisible(true);
//        dispose();
        //small change
    }
}