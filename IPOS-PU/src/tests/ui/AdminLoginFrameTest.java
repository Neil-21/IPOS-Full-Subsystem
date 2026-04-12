package tests.ui;

import main.db.DatabaseManager;
import main.service.AuthService;
import main.ui.AdminLoginFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class AdminLoginFrameTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: AdminLoginFrame can be constructed without throwing.
    @Test
    void testAdminLoginFrame_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        AdminLoginFrame frame = assertDoesNotThrow(() -> new AdminLoginFrame(authService));
        frame.dispose();
    }

    // Expected: admin login frame includes expected labels, fields, and actions.
    @Test
    void testAdminLoginFrame_HasExpectedFormStructure() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        AdminLoginFrame frame = new AdminLoginFrame(authService);
        try {
            assertEquals("Admin Login", frame.getTitle());
            assertEquals(WindowConstants.EXIT_ON_CLOSE, frame.getDefaultCloseOperation());

            List<JTextField> textFields = UiTestUtils.findAllWithExactClass(frame, JTextField.class);
            List<JPasswordField> passwordFields = UiTestUtils.findAllOfType(frame, JPasswordField.class);

            assertEquals(1, textFields.size(), "Expected one email input.");
            assertEquals(1, passwordFields.size(), "Expected one password input.");
            assertTrue(UiTestUtils.findButtonByText(frame, "Login").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Back").isPresent());
        } finally {
            frame.dispose();
        }
    }
}
