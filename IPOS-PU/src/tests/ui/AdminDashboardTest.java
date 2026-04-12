package tests.ui;

import main.db.DatabaseManager;
import main.ui.AdminDashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class AdminDashboardTest {

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
    }

    // Expected: AdminDashboard can be constructed without throwing.
    @Test
    void testAdminDashboard_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        AdminDashboard frame = assertDoesNotThrow(AdminDashboard::new);
        frame.dispose();
    }

    // Expected: dashboard exposes all admin action buttons.
    @Test
    void testAdminDashboard_HasPrimaryActionButtons() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        AdminDashboard frame = new AdminDashboard();
        try {
            assertEquals("Admin Dashboard", frame.getTitle());
            assertTrue(UiTestUtils.findButtonByText(frame, "Sales Report").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Campaign Report").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Create Campaign").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Campaign Engagement").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Print Report").isPresent());
        } finally {
            frame.dispose();
        }
    }

    // Expected: dashboard contains a single read-only output area.
    @Test
    void testAdminDashboard_OutputAreaIsReadOnly() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        AdminDashboard frame = new AdminDashboard();
        try {
            List<JTextArea> textAreas = UiTestUtils.findAllOfType(frame, JTextArea.class);
            assertEquals(1, textAreas.size());
            assertFalse(textAreas.get(0).isEditable());
        } finally {
            frame.dispose();
        }
    }
}
