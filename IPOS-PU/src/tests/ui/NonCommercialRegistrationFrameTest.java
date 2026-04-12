package tests.ui;

import main.db.DatabaseManager;
import main.service.AuthService;
import main.ui.NonCommercialRegistrationFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class NonCommercialRegistrationFrameTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: NonCommercialRegistrationFrame can be constructed without throwing.
    @Test
    void testNonCommercialRegistrationFrame_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        NonCommercialRegistrationFrame frame =
                assertDoesNotThrow(() -> new NonCommercialRegistrationFrame(authService));
        frame.dispose();
    }

    // Expected: non-commercial registration form exposes required controls.
    @Test
    void testNonCommercialRegistrationFrame_HasExpectedFormStructure() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        NonCommercialRegistrationFrame frame = new NonCommercialRegistrationFrame(authService);
        try {
            assertEquals("Non-Commercial Registration", frame.getTitle());
            assertEquals(WindowConstants.EXIT_ON_CLOSE, frame.getDefaultCloseOperation());

            List<JTextField> textFields = UiTestUtils.findAllOfType(frame, JTextField.class);
            assertEquals(2, textFields.size(), "Expected name and email fields.");
            assertTrue(UiTestUtils.findButtonByText(frame, "Register").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Back").isPresent());
        } finally {
            frame.dispose();
        }
    }
}
