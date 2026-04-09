package main.ui;

import main.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CommercialApplicationFrame extends JFrame {

    private final AuthService authService;

    private JTextField companyNameField;
    private JTextField companyAddressField;
    private JTextField directorNameField;
    private JTextField directorContactField;
    private JComboBox<String> notificationMethodBox;

    public CommercialApplicationFrame(AuthService authService) {
        this.authService = authService;

        setTitle("Commercial Membership Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 350);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JLabel title = new JLabel("Apply for Commercial Membership", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));

        form.add(new JLabel("Company Name:"));
        companyNameField = new JTextField();
        form.add(companyNameField);

        form.add(new JLabel("Company Address:"));
        companyAddressField = new JTextField();
        form.add(companyAddressField);

        form.add(new JLabel("Director Name:"));
        directorNameField = new JTextField();
        form.add(directorNameField);

        form.add(new JLabel("Director Contact:"));
        directorContactField = new JTextField();
        form.add(directorContactField);

        form.add(new JLabel("Notification Method:"));
        notificationMethodBox = new JComboBox<>(new String[]{"Email", "Post"});
        form.add(notificationMethodBox);

        JButton submitButton = new JButton("Submit Application");
        JButton backButton = new JButton("Back");

        form.add(submitButton);
        form.add(backButton);

        root.add(form, BorderLayout.CENTER);

        submitButton.addActionListener(e -> handleSubmit());

        backButton.addActionListener(e -> {
            new WelcomeFrame(authService).setVisible(true);
            dispose();
        });
    }

    private void handleSubmit() {
        String companyName = companyNameField.getText().trim();
        String companyAddress = companyAddressField.getText().trim();
        String directorName = directorNameField.getText().trim();
        String directorContact = directorContactField.getText().trim();
        String notificationMethod = (String) notificationMethodBox.getSelectedItem();

        if (companyName.isEmpty() || companyAddress.isEmpty() ||
                directorName.isEmpty() || directorContact.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Commercial application submitted successfully.\n\n" +
                        "This is currently a prototype and can later be wired to IPOS-SA.",
                "Application Submitted",
                JOptionPane.INFORMATION_MESSAGE);

        new WelcomeFrame(authService).setVisible(true);
        dispose();
    }
}