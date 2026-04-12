package tests.ui;

import main.db.DatabaseManager;
import main.service.AuthService;
import main.ui.CustomerLoginFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CustomerLoginFrameTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: CustomerLoginFrame can be constructed without throwing.
    @Test
    void testCustomerLoginFrame_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        CustomerLoginFrame frame = assertDoesNotThrow(() -> new CustomerLoginFrame(authService));
        frame.dispose();
    }

    // Expected: login form has expected frame configuration and input fields.
    @Test
    void testCustomerLoginFrame_HasExpectedFormStructure() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        CustomerLoginFrame frame = new CustomerLoginFrame(authService);
        try {
            assertEquals("Customer Login", frame.getTitle());
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
