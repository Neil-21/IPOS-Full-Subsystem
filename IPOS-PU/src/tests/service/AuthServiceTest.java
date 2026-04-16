package tests.service;

import main.db.DatabaseManager;
import main.model.User;
import main.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;
    private final List<String> usersToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (String email : usersToDelete) {
            deleteUser(email);
        }
        usersToDelete.clear();
    }

    // Expected: returns a User when credentials are valid.
    @Test
    void testLogin_ValidCredentials_ReturnsUser() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "Pass123!", "CUSTOMER", 1, "Login User");

        User user = authService.login(email, "Pass123!");

        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertEquals("CUSTOMER", user.getRole());
        assertTrue(user.isFirstLogin());
    }

    // Expected: returns null when password is incorrect.
    @Test
    void testLogin_InvalidPassword_ReturnsNull() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "Pass123!", "CUSTOMER", 1, "Wrong Password User");

        User user = authService.login(email, "WrongPass");

        assertNull(user);
    }

    // Expected: returns null when email or password is null.
    @Test
    void testLogin_NullEmailOrPassword_ReturnsNull() {
        assertNull(authService.login(null, "Pass123!"));
        assertNull(authService.login("user@example.com", null));
    }

    // Expected: updates password and first-login flag in memory and database.
    @Test
    void testChangePassword_ValidInput_UpdatesDatabaseAndUser() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "OldPass1", "CUSTOMER", 1, "Password Change User");
        User user = authService.login(email, "OldPass1");
        assertNotNull(user);

        boolean changed = authService.changePassword(user, "NewPassword1!");

        assertTrue(changed);
        assertEquals("NewPassword1!", user.getPassword());
        assertFalse(user.isFirstLogin());
        assertNotNull(authService.login(email, "NewPassword1!"));
        assertNull(authService.login(email, "OldPass1"));
    }

    // Expected: returns false for invalid password-change inputs.
    @Test
    void testChangePassword_InvalidInput_ReturnsFalse() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "OldPass1", "CUSTOMER", 1, "Invalid Change User");
        User user = authService.login(email, "OldPass1");
        assertNotNull(user);

        assertFalse(authService.changePassword(user, ""));
        assertFalse(authService.changePassword(user, "123"));
        assertFalse(authService.changePassword(null, "NewPass1"));
    }

    // Expected: creates a new member and returns a temporary password.
    @Test
    void testRegisterNonCommercialMember_ValidInput_CreatesUserAndReturnsPassword() throws SQLException {
        String email = uniqueEmail();

        String tempPassword = authService.registerNonCommercialMember("New Member", email);

        assertNotNull(tempPassword);
        assertEquals(8, tempPassword.length());
        usersToDelete.add(email);

        User created = authService.login(email, tempPassword);
        assertNotNull(created);
        assertTrue(created.isCustomer());
        assertTrue(created.isFirstLogin());
        assertEquals("New Member", created.getFullName());
    }

    // Expected: returns null when trying to register an existing email.
    @Test
    void testRegisterNonCommercialMember_DuplicateEmail_ReturnsNull() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "Pass123!", "CUSTOMER", 1, "Duplicate User");

        String tempPassword = authService.registerNonCommercialMember("Another Name", email);

        assertNull(tempPassword);
    }

    // Expected: true for existing email and false for non-existing/invalid email.
    @Test
    void testEmailExists_ReturnsExpectedResults() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "Pass123!", "CUSTOMER", 1, "Exists User");

        assertTrue(authService.emailExists(email.toUpperCase()));
        assertFalse(authService.emailExists("not-found-" + UUID.randomUUID() + "@ipos.com"));
        assertFalse(authService.emailExists(null));
        assertFalse(authService.emailExists(" "));
    }

    // Expected: login normalizes email case and surrounding spaces.
    @Test
    void testLogin_EmailWithCaseAndWhitespace_ReturnsUser() throws SQLException {
        String email = uniqueEmail();
        insertUser(email, "Pass123!", "CUSTOMER", 1, "Case User");

        User user = authService.login("  " + email.toUpperCase() + "  ", "Pass123!");

        assertNotNull(user);
        assertEquals(email, user.getEmail());
    }

    // Expected: registration trims full name and normalizes email before persisting.
    @Test
    void testRegisterNonCommercialMember_TrimsInputsAndNormalizesEmail() throws SQLException {
        String rawEmail = "  " + uniqueEmail().toUpperCase() + "  ";
        String rawName = "  Trim Me  ";

        String tempPassword = authService.registerNonCommercialMember(rawName, rawEmail);

        assertNotNull(tempPassword);
        String normalizedEmail = rawEmail.trim().toLowerCase();
        usersToDelete.add(normalizedEmail);

        User created = authService.login(normalizedEmail, tempPassword);
        assertNotNull(created);
        assertEquals("Trim Me", created.getFullName());
        assertEquals(normalizedEmail, created.getEmail());
    }

    // Expected: registration returns null for blank required values.
    @Test
    void testRegisterNonCommercialMember_BlankInputs_ReturnsNull() {
        assertNull(authService.registerNonCommercialMember(" ", "person@ipos.com"));
        assertNull(authService.registerNonCommercialMember("Name", " "));
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@ipos.com";
    }

    private void insertUser(String email, String password, String role, int firstLogin, String fullName) throws SQLException {
        String sql = """
            INSERT INTO users (email, full_name, password, role, first_login)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, fullName);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.setInt(5, firstLogin);
            ps.executeUpdate();
        }
        usersToDelete.add(email);
    }

    private void deleteUser(String email) throws SQLException {
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }
}
