package tests.ui;

import main.db.DatabaseManager;
import main.service.AuthService;
import main.ui.WelcomeFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class WelcomeFrameTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: WelcomeFrame can be constructed without throwing.
    @Test
    void testWelcomeFrame_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        WelcomeFrame frame = assertDoesNotThrow(() -> new WelcomeFrame(authService));
        frame.dispose();
    }

    // Expected: frame metadata is configured for welcome navigation screen.
    @Test
    void testWelcomeFrame_HasExpectedWindowSettings() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        WelcomeFrame frame = new WelcomeFrame(authService);
        try {
            assertEquals("IPOS-PU Welcome", frame.getTitle());
            assertEquals(WindowConstants.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
            assertEquals(500, frame.getWidth());
            assertEquals(420, frame.getHeight());
        } finally {
            frame.dispose();
        }
    }

    // Expected: all primary navigation buttons are present.
    @Test
    void testWelcomeFrame_HasAllPrimaryButtons() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        WelcomeFrame frame = new WelcomeFrame(authService);
        try {
            List<JButton> buttons = UiTestUtils.findAllOfType(frame, JButton.class);
            assertEquals(5, buttons.size());
            assertTrue(UiTestUtils.findButtonByText(frame, "Login as Customer").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Login as Admin").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Register Non-Commercial").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Apply for Commercial Membership").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Exit").isPresent());
        } finally {
            frame.dispose();
        }
    }
}
