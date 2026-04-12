package tests.ui;

import main.db.DatabaseManager;
import main.service.AuthService;
import main.ui.CommercialApplicationFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CommercialApplicationFrameTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: CommercialApplicationFrame can be constructed without throwing.
    @Test
    void testCommercialApplicationFrame_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        CommercialApplicationFrame frame =
                assertDoesNotThrow(() -> new CommercialApplicationFrame(authService));
        frame.dispose();
    }

    // Expected: commercial application form defines expected window and required controls.
    @Test
    void testCommercialApplicationFrame_HasExpectedFormStructure() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        CommercialApplicationFrame frame = new CommercialApplicationFrame(authService);
        try {
            assertEquals("Commercial Membership Application", frame.getTitle());
            assertEquals(WindowConstants.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());

            List<JTextField> textFields = UiTestUtils.findAllOfType(frame, JTextField.class);
            List<JComboBox> comboBoxes = UiTestUtils.findAllOfType(frame, JComboBox.class);
            assertEquals(10, textFields.size(), "Expected all commercial application text inputs.");
            assertEquals(1, comboBoxes.size(), "Expected one notification method selector.");

            JComboBox<?> notificationBox = comboBoxes.get(0);
            assertEquals(2, notificationBox.getItemCount());
            assertEquals("Email", notificationBox.getItemAt(0));
            assertEquals("Post", notificationBox.getItemAt(1));
            assertTrue(UiTestUtils.findButtonByText(frame, "Submit Application").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Back").isPresent());
        } finally {
            frame.dispose();
        }
    }
}
