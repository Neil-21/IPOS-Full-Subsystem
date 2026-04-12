package main.implementation;

import main.api.PUCommsAPI;
import main.api.SAMerchantApplicationAPI;
import main.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class SAMerchantApplicationAPIImpl implements SAMerchantApplicationAPI {

    private final PUCommsAPI puCommsAPI = new PUCommsAPIImpl();

    @Override
    public String submitMerchantApplication(String application) {
        if (application == null || application.trim().isEmpty()) {
            return "Application submission failed: application data is empty.";
        }

        String[] parts = application.split("\\|", -1);

        if (parts.length < 11) {
            return "Application submission failed: invalid application format.";
        }

        String applicationId = "APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String companyName = parts[0].trim();
        String businessType = parts[1].trim();
        String addressLine1 = parts[2].trim();
        String addressLine2 = parts[3].trim();
        String city = parts[4].trim();
        String postcode = parts[5].trim();
        String companyHouseRegistration = parts[6].trim();
        String directorName = parts[7].trim();
        String directorContact = parts[8].trim();
        String email = parts[9].trim();
        String notificationMethod = parts[10].trim();

        if (companyName.isEmpty() || businessType.isEmpty() || addressLine1.isEmpty()
                || city.isEmpty() || postcode.isEmpty() || companyHouseRegistration.isEmpty()
                || directorName.isEmpty() || directorContact.isEmpty()
                || email.isEmpty() || notificationMethod.isEmpty()) {
            return "Application submission failed: one or more required fields are empty.";
        }

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

            ps.setString(1, applicationId);
            ps.setString(2, companyName);
            ps.setString(3, businessType);
            ps.setString(4, addressLine1);
            ps.setString(5, addressLine2);
            ps.setString(6, city);
            ps.setString(7, postcode);
            ps.setString(8, companyHouseRegistration);
            ps.setString(9, directorName);
            ps.setString(10, directorContact);
            ps.setString(11, email);
            ps.setString(12, notificationMethod);
            ps.setString(13, "PENDING");
            ps.setString(14, LocalDateTime.now().toString());

            ps.executeUpdate();
            return applicationId;

        } catch (SQLException e) {
            e.printStackTrace();
            return "Application submission failed due to database error.";
        }
    }

    @Override
    public String getApplicationStatus(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return "Invalid application ID.";
        }

        String sql = """
            SELECT status, email, director_name, company_name, notification_method
            FROM commercial_applications
            WHERE application_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, applicationId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Application not found.";
                }

                String status = rs.getString("status");
                return status;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Failed to retrieve application status.";
        }
    }

    public boolean updateApplicationStatus(String applicationId, String newStatus) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return false;
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return false;
        }

        String normalisedStatus = newStatus.trim().toUpperCase();

        if (!normalisedStatus.equals("PENDING")
                && !normalisedStatus.equals("APPROVED")
                && !normalisedStatus.equals("REJECTED")) {
            return false;
        }

        String selectSql = """
            SELECT email, director_name, company_name, notification_method
            FROM commercial_applications
            WHERE application_id = ?
        """;

        String updateSql = """
            UPDATE commercial_applications
            SET status = ?
            WHERE application_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection()) {

            String email;
            String directorName;
            String companyName;
            String notificationMethod;

            try (PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
                selectPs.setString(1, applicationId.trim());

                try (ResultSet rs = selectPs.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }

                    email = rs.getString("email");
                    directorName = rs.getString("director_name");
                    companyName = rs.getString("company_name");
                    notificationMethod = rs.getString("notification_method");
                }
            }

            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setString(1, normalisedStatus);
                updatePs.setString(2, applicationId.trim());

                if (updatePs.executeUpdate() == 0) {
                    return false;
                }
            }

            notifyApplicantIfRequired(email, directorName, companyName, notificationMethod, normalisedStatus);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void notifyApplicantIfRequired(String email,
                                           String directorName,
                                           String companyName,
                                           String notificationMethod,
                                           String newStatus) {

        if (!"Email".equalsIgnoreCase(notificationMethod)) {
            return;
        }

        String subject = "Commercial application update";
        String body;

        if ("APPROVED".equalsIgnoreCase(newStatus)) {
            body = "Dear " + directorName + ",\n\n"
                    + "Your commercial application for " + companyName
                    + " has been approved.\n"
                    + "You may now proceed to access the IPOS-SA services.\n\n"
                    + "Regards,\nIPOS-SA";
        } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
            body = "Dear " + directorName + ",\n\n"
                    + "Your commercial application for " + companyName
                    + " has been rejected.\n\n"
                    + "Regards,\nIPOS-SA";
        } else {
            return;
        }

        puCommsAPI.sendEmail(email, subject, body);
    }
}