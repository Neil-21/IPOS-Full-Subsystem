package tests.implementation;

import main.db.DatabaseManager;
import main.implementation.SAMerchantApplicationAPIImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SAMerchantApplicationAPIImplTest {

    private SAMerchantApplicationAPIImpl api;
    private final List<String> applicationIdsToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        api = new SAMerchantApplicationAPIImpl();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String applicationId : applicationIdsToDelete) {
            deleteApplication(applicationId);
        }
        applicationIdsToDelete.clear();
    }

    // Expected: submitMerchantApplication returns generated application ID and persists PENDING record.
    @Test
    void testSubmitMerchantApplication_ValidInput_ReturnsIdAndPersists() throws SQLException {
        String payload = validApplicationPayload(uniqueSuffix(), "Email");

        String result = api.submitMerchantApplication(payload);

        assertTrue(result.startsWith("APP-"));
        applicationIdsToDelete.add(result);
        assertEquals("PENDING", getApplicationStatusDirect(result));
    }

    // Expected: submitMerchantApplication returns validation errors for empty/invalid payload.
    @Test
    void testSubmitMerchantApplication_InvalidInput_ReturnsFailureMessage() {
        assertEquals(
                "Application submission failed: application data is empty.",
                api.submitMerchantApplication(null)
        );
        assertEquals(
                "Application submission failed: invalid application format.",
                api.submitMerchantApplication("company|only|few|parts")
        );
        assertEquals(
                "Application submission failed: one or more required fields are empty.",
                api.submitMerchantApplication("Comp||Line1||City|P1 1AA|REG|Director|0123|a@b.com|Email")
        );
    }

    // Expected: getApplicationStatus returns status for existing app and proper messages otherwise.
    @Test
    void testGetApplicationStatus_ReturnsExpectedValues() {
        String missingId = "APP-NOT-FOUND-" + UUID.randomUUID();
        assertEquals("Invalid application ID.", api.getApplicationStatus(null));
        assertEquals("Invalid application ID.", api.getApplicationStatus(" "));
        assertEquals("Application not found.", api.getApplicationStatus(missingId));
    }

    // Expected: updateApplicationStatus updates existing application when valid status provided.
    @Test
    void testUpdateApplicationStatus_ValidTransition_UpdatesStatus() throws SQLException {
        String payload = validApplicationPayload(uniqueSuffix(), "Post");
        String appId = api.submitMerchantApplication(payload);
        assertTrue(appId.startsWith("APP-"));
        applicationIdsToDelete.add(appId);

        boolean updated = api.updateApplicationStatus(appId, "approved");

        assertTrue(updated);
        assertEquals("APPROVED", api.getApplicationStatus(appId));
    }

    // Expected: updateApplicationStatus rejects invalid statuses and missing IDs.
    @Test
    void testUpdateApplicationStatus_InvalidInput_ReturnsFalse() {
        assertFalse(api.updateApplicationStatus(null, "APPROVED"));
        assertFalse(api.updateApplicationStatus(" ", "APPROVED"));
        assertFalse(api.updateApplicationStatus("APP-UNKNOWN", null));
        assertFalse(api.updateApplicationStatus("APP-UNKNOWN", " "));
        assertFalse(api.updateApplicationStatus("APP-UNKNOWN", "ON_HOLD"));
        assertFalse(api.updateApplicationStatus("APP-UNKNOWN", "APPROVED"));
    }

    private String validApplicationPayload(String suffix, String notificationMethod) {
        return String.join("|",
                "Test Company " + suffix,
                "Retail",
                "1 Example Street",
                "Suite " + suffix,
                "London",
                "EC1A 1AA",
                "CH-" + suffix,
                "Jane Director",
                "0123456789",
                "merchant-" + suffix + "@example.com",
                notificationMethod
        );
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getApplicationStatusDirect(String applicationId) throws SQLException {
        String sql = "SELECT status FROM commercial_applications WHERE application_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString("status");
            }
        }
    }

    private void deleteApplication(String applicationId) throws SQLException {
        String sql = "DELETE FROM commercial_applications WHERE application_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, applicationId);
            ps.executeUpdate();
        }
    }
}
