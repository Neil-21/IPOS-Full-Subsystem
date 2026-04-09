package main.ui;

import main.service.AuthService;
import main.api.PUCommsAPI;
import main.implementation.PUCommsAPIImpl;
import main.ui.WelcomeFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class NonCommercialRegistrationFrame extends JFrame {

    private final AuthService authService;

    private JTextField nameField;
    private JTextField emailField;

    public NonCommercialRegistrationFrame(AuthService authService) {
        this.authService = authService;

        setTitle("Non-Commercial Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JLabel title = new JLabel("Register Non-Commercial Member", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));

        form.add(new JLabel("Full Name:"));
        nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Email:"));
        emailField = new JTextField();
        form.add(emailField);

        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back");

        form.add(registerButton);
        form.add(backButton);

        root.add(form, BorderLayout.CENTER);

        registerButton.addActionListener(e -> handleRegister());

        backButton.addActionListener(e -> {
            new WelcomeFrame(authService).setVisible(true);
            dispose();
        });
    }

    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();

        String tempPassword = authService.registerNonCommercialMember(name, email);

        if (tempPassword == null) {
            JOptionPane.showMessageDialog(this,
                    "Registration failed. Email may already exist or fields are invalid.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        PUCommsAPI comms = new PUCommsAPIImpl();

        comms.sendEmail(
                email,
                "Your IPOS-PU Login Details",
                "Welcome to IPOS-PU!\n\n" +
                        "Username: " + email + "\n" +
                        "Temporary Password: " + tempPassword + "\n\n" +
                        "You will be required to change your password on first login."
        );

        JOptionPane.showMessageDialog(this,
                "Registration successful!\n\n" +
                        "A login email has been generated.\n" +
                        "Temporary password: " + tempPassword + "\n\n" +
                        "You will be asked to change it on first login.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);

//        JOptionPane.showMessageDialog(this,
//                "Registration successful!\n\nTemporary password: " + tempPassword +
//                        "\n\nYou will be asked to change it on first login.",
//                "Success",
//                JOptionPane.INFORMATION_MESSAGE);
//
//        new WelcomeFrame(authService).setVisible(true);
//        dispose();
    }
}