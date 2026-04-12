package tests.ui;

import main.db.DatabaseManager;
import main.model.User;
import main.service.AuthService;
import main.ui.PasswordChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class PasswordChangeTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: PasswordChange dialog can be constructed without throwing.
    @Test
    void testPasswordChange_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        JFrame parent = new JFrame();
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", true, "Test User");

        PasswordChange dialog = assertDoesNotThrow(() -> new PasswordChange(parent, authService, user));
        dialog.dispose();
        parent.dispose();
    }

    // Expected: password dialog is modal and includes expected controls.
    @Test
    void testPasswordChange_HasExpectedDialogConfiguration() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        JFrame parent = new JFrame();
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", true, "Test User");
        PasswordChange dialog = new PasswordChange(parent, authService, user);
        try {
            assertTrue(dialog.isModal());
            assertEquals("Change Password", dialog.getTitle());

            List<JPasswordField> passwordFields = UiTestUtils.findAllOfType(dialog, JPasswordField.class);
            assertEquals(2, passwordFields.size());
            assertTrue(UiTestUtils.findButtonByText(dialog, "Save").isPresent());
            assertTrue(UiTestUtils.findButtonByText(dialog, "Cancel").isPresent());
        } finally {
            dialog.dispose();
            parent.dispose();
        }
    }
}
