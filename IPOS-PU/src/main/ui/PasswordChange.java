package main.ui;

import main.model.User;
import main.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class PasswordChange extends JDialog {

    public PasswordChange(JFrame parent, AuthService authService, User user) {
        super(parent, "Change Password", true);

        setSize(350, 180);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(3, 2, 10, 10));

        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        add(new JLabel("New Password:"));
        add(newPasswordField);
        add(new JLabel("Confirm Password:"));
        add(confirmPasswordField);
        add(saveButton);
        add(cancelButton);

        saveButton.addActionListener(e -> {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.");
                return;
            }

            if (!authService.changePassword(user, newPassword)) {
                JOptionPane.showMessageDialog(this, "Password must be at least 10 characters and must contain at least 1 number and 1 special character.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Password changed successfully.");
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());
    }
}