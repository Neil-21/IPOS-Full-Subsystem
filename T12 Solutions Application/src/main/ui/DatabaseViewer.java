package main.ui;

import main.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DatabaseViewer extends JFrame {

    public DatabaseViewer() {
        setTitle("Database Viewer - Users");
        setSize(700, 400);
        setLocationRelativeTo(null);

        String[] columns = {"Email", "Full Name", "Password", "Role", "First Login"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        JTable table = new JTable(model);

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getInt("first_login")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading database: " + e.getMessage());
        }

        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}