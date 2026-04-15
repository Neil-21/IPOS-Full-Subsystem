package main.service;

import main.db.DatabaseManager;
import main.model.CommercialApplication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    public boolean sendToSA(CommercialApplication application) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            Gson gson = new Gson();

            String fullAddress = application.getAddressLine1();
            if (application.getAddressLine2() != null && !application.getAddressLine2().isBlank()) {
                fullAddress = fullAddress + ", " + application.getAddressLine2();
            }
            fullAddress = fullAddress + ", " + application.getCity() + ", " + application.getPostcode();

            java.util.Map<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("applicationId", application.getApplicationId());
            requestBody.put("type", "commercial");
            requestBody.put("email", application.getEmail());
            requestBody.put("companyName", application.getCompanyName());
            requestBody.put("companyHouseReg", application.getCompanyHouseRegistration());
            requestBody.put("directorName", application.getDirectorName());
            requestBody.put("businessType", application.getBusinessType());
            requestBody.put("address", fullAddress);

            String json = gson.toJson(requestBody);

            System.out.println("Sending JSON to SA: " + json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/pu-applications"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("SA response code: " + response.statusCode());
            System.out.println("SA response body: " + response.body());

            return response.statusCode() == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

//    public boolean sendToSA(CommercialApplication application) {
//        try {
//            HttpClient client = HttpClient.newHttpClient();
//            Gson gson = new Gson();
//
//            String fullAddress = application.getAddressLine1();
//            if (application.getAddressLine2() != null && !application.getAddressLine2().isBlank()) {
//                fullAddress = fullAddress + ", " + application.getAddressLine2();
//            }
//            fullAddress = fullAddress + ", " + application.getCity() + ", " + application.getPostcode();
//
//            final String addressFinal = fullAddress;
//
//
//            Object requestBody = new Object() {
//                final String applicationId = application.getApplicationId();
//                final String type = "commercial";
//                final String email = application.getEmail();
//                final String companyName = application.getCompanyName();
//                final String companyHouseReg = application.getCompanyHouseRegistration();
//                final String directorName = application.getDirectorName();
//                final String businessType = application.getBusinessType();
//                final String address = addressFinal;
//            };
//
//            String json = gson.toJson(requestBody);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("http://localhost:8080/api/pu-applications"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//
//            HttpResponse<String> response =
//                    client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            System.out.println("SA response code: " + response.statusCode());
//            System.out.println("SA response body: " + response.body());
//
//            return response.statusCode() == 201;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
}