package main.service;

import main.db.DatabaseManager;
import main.model.CommercialApplication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommercialApplicationService {

    public void saveApplication(CommercialApplication application) throws SQLException {
        String sql = """
            INSERT INTO commercial_applications (
                application_id,
                company_name,
                business_type,
                address_line_1,
                address_line_2,
                city,
                postcode,
                company_house_registration,
                director_name,
                director_contact,
                email,
                notification_method,
                status,
                submitted_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, application.getApplicationId());
            ps.setString(2, application.getCompanyName());
            ps.setString(3, application.getBusinessType());
            ps.setString(4, application.getAddressLine1());
            ps.setString(5, application.getAddressLine2());
            ps.setString(6, application.getCity());
            ps.setString(7, application.getPostcode());
            ps.setString(8, application.getCompanyHouseRegistration());
            ps.setString(9, application.getDirectorName());
            ps.setString(10, application.getDirectorContact());
            ps.setString(11, application.getEmail());
            ps.setString(12, application.getNotificationMethod());
            ps.setString(13, application.getStatus());
            ps.setString(14, application.getSubmittedAt().toString());

            ps.executeUpdate();
        }
    }

    public boolean emailExistsForApplication(String email) throws SQLException {
        String sql = "SELECT 1 FROM commercial_applications WHERE lower(email) = lower(?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean companyHouseExists(String reg) throws SQLException {
        String sql = "SELECT 1 FROM commercial_applications WHERE lower(company_house_registration) = lower(?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reg);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}