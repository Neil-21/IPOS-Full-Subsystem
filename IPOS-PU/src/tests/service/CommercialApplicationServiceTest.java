package tests.service;

import main.db.DatabaseManager;
import main.model.CommercialApplication;
import main.service.CommercialApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CommercialApplicationServiceTest {

    private CommercialApplicationService service;
    private final List<String> applicationIdsToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        service = new CommercialApplicationService();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String applicationId : applicationIdsToDelete) {
            deleteApplicationById(applicationId);
        }
        applicationIdsToDelete.clear();
    }

    // Expected: persists a commercial application record with searchable unique fields.
    @Test
    void testSaveApplication_ValidInput_PersistsApplication() throws SQLException {
        CommercialApplication application = newApplication(uniqueApplicationId(), uniqueRegistration(), uniqueEmail());

        service.saveApplication(application);
        applicationIdsToDelete.add(application.getApplicationId());

        assertTrue(service.emailExistsForApplication(application.getEmail()));
        assertTrue(service.companyHouseExists(application.getCompanyHouseRegistration()));
    }

    // Expected: returns true for existing email (case-insensitive) and false for non-existing/null.
    @Test
    void testEmailExistsForApplication_ReturnsExpectedResults() throws SQLException {
        CommercialApplication application = newApplication(uniqueApplicationId(), uniqueRegistration(), uniqueEmail());
        service.saveApplication(application);
        applicationIdsToDelete.add(application.getApplicationId());

        assertTrue(service.emailExistsForApplication(application.getEmail().toUpperCase()));
        assertFalse(service.emailExistsForApplication("missing-" + UUID.randomUUID() + "@ipos.com"));
        assertFalse(service.emailExistsForApplication(null));
    }

    // Expected: returns true for existing registration (case-insensitive) and false for non-existing/null.
    @Test
    void testCompanyHouseExists_ReturnsExpectedResults() throws SQLException {
        CommercialApplication application = newApplication(uniqueApplicationId(), uniqueRegistration(), uniqueEmail());
        service.saveApplication(application);
        applicationIdsToDelete.add(application.getApplicationId());

        assertTrue(service.companyHouseExists(application.getCompanyHouseRegistration().toLowerCase()));
        assertFalse(service.companyHouseExists("NO-REG-" + UUID.randomUUID()));
        assertFalse(service.companyHouseExists(null));
    }

    // Expected: saving duplicate application ID throws SQLException.
    @Test
    void testSaveApplication_DuplicateApplicationId_ThrowsSQLException() throws SQLException {
        String applicationId = uniqueApplicationId();
        CommercialApplication first = newApplication(applicationId, uniqueRegistration(), uniqueEmail());
        CommercialApplication duplicateId = newApplication(applicationId, uniqueRegistration(), uniqueEmail());

        service.saveApplication(first);
        applicationIdsToDelete.add(applicationId);

        assertThrows(SQLException.class, () -> service.saveApplication(duplicateId));
    }

    // Expected: null optional address line 2 is accepted and persisted.
    @Test
    void testSaveApplication_NullAddressLine2_PersistsSuccessfully() throws SQLException {
        CommercialApplication application = new CommercialApplication(
                uniqueApplicationId(),
                "MediCorp Optional",
                "Retail",
                "22 Health Street",
                null,
                "London",
                "E1 1AA",
                uniqueRegistration(),
                "Jane Director",
                "01234 567890",
                uniqueEmail(),
                "SMS",
                "PENDING",
                LocalDateTime.now()
        );

        service.saveApplication(application);
        applicationIdsToDelete.add(application.getApplicationId());

        assertTrue(service.emailExistsForApplication(application.getEmail()));
        assertTrue(service.companyHouseExists(application.getCompanyHouseRegistration()));
    }

    private CommercialApplication newApplication(String applicationId, String companyHouseRegistration, String email) {
        return new CommercialApplication(
                applicationId,
                "MediCorp " + UUID.randomUUID().toString().substring(0, 8),
                "Wholesaler",
                "10 Health Street",
                "Suite 4",
                "London",
                "EC1A 1BB",
                companyHouseRegistration,
                "Jane Director",
                "01234 567890",
                email,
                "Email",
                "PENDING",
                LocalDateTime.now()
        );
    }

    private String uniqueApplicationId() {
        return "APP-" + UUID.randomUUID();
    }

    private String uniqueRegistration() {
        return "CH-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String uniqueEmail() {
        return "commercial-" + UUID.randomUUID() + "@ipos.com";
    }

    private void deleteApplicationById(String applicationId) throws SQLException {
        String sql = "DELETE FROM commercial_applications WHERE application_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, applicationId);
            ps.executeUpdate();
        }
    }
}
