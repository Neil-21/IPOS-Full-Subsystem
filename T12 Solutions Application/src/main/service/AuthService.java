package main.service;

import main.db.DatabaseManager;
import main.model.User;

import java.sql.*;
import java.util.UUID;

public class AuthService {

    public User login(String email, String password) {
        if (email == null || password == null) return null;

        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            if (!rs.getString("password").equals(password)) return null;

            return new User(
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getInt("first_login") == 1,
                    rs.getString("full_name")
            );

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean changePassword(User user, String newPassword) {
        if (user == null || newPassword == null || newPassword.trim().isEmpty()) return false;
        if (newPassword.length() < 6) return false;

        String sql = "UPDATE users SET password = ?, first_login = 0 WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setString(2, user.getEmail());
            ps.executeUpdate();

            user.setPassword(newPassword);
            user.setFirstLogin(false);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String registerNonCommercialMember(String fullName, String email) {
        if (fullName == null || fullName.trim().isEmpty()) return null;
        if (email == null || email.trim().isEmpty()) return null;

        String cleanEmail = email.trim().toLowerCase();
        if (emailExists(cleanEmail)) return null;

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String sql = """
            INSERT INTO users (email, full_name, password, role, first_login)
            VALUES (?, ?, ?, 'CUSTOMER', 1)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cleanEmail);
            ps.setString(2, fullName.trim());
            ps.setString(3, tempPassword);
            ps.executeUpdate();
            return tempPassword;

        } catch (SQLException e) {
            return null;
        }
    }

    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) return false;

        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            return ps.executeQuery().next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}