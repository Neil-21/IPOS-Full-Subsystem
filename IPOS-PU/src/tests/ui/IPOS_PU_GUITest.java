package tests.ui;

import main.db.DatabaseManager;
import main.model.User;
import main.service.AuthService;
import main.ui.IPOS_PU_GUI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class IPOS_PU_GUITest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: IPOS_PU_GUI can be constructed with valid dependencies.
    @Test
    void testIposPuGui_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", false, "Customer User");
        IPOS_PU_GUI frame = assertDoesNotThrow(() -> new IPOS_PU_GUI(authService, user));
        frame.dispose();
    }

    // Expected: IPOS GUI creates expected primary tabs and logged-in header state.
    @Test
    void testIposPuGui_HasExpectedTabsAndButtonsForLoggedInUser() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", false, "Customer User");
        IPOS_PU_GUI frame = new IPOS_PU_GUI(authService, user);
        try {
            assertEquals("IPOS - Public Online Pharmacy", frame.getTitle());
            List<JTabbedPane> tabbedPanes = UiTestUtils.findAllOfType(frame, JTabbedPane.class);
            assertEquals(1, tabbedPanes.size());

            JTabbedPane tabs = tabbedPanes.get(0);
            assertEquals(5, tabs.getTabCount());
            assertEquals("Browse Catalogue", tabs.getTitleAt(0));
            assertEquals("Promotions", tabs.getTitleAt(1));
            assertEquals("Shopping Cart", tabs.getTitleAt(2));
            assertEquals("My Orders", tabs.getTitleAt(3));
            assertEquals("Membership", tabs.getTitleAt(4));

            assertTrue(UiTestUtils.findButtonByText(frame, "Logout").isPresent());
        } finally {
            frame.dispose();
        }
    }

    // Expected: cart button starts at zero items for fresh session.
    @Test
    void testIposPuGui_CartButtonStartsAtZero() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", false, "Customer User");
        IPOS_PU_GUI frame = new IPOS_PU_GUI(authService, user);
        try {
            boolean foundCartButton = UiTestUtils.findAllOfType(frame, JButton.class).stream()
                    .map(JButton::getText)
                    .anyMatch(text -> text != null && text.contains("Cart (0)"));
            assertTrue(foundCartButton, "Expected cart button to show zero items.");
        } finally {
            frame.dispose();
        }
    }
}
