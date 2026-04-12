package tests.ui;

import main.db.DatabaseManager;
import main.model.User;
import main.service.AuthService;
import main.ui.CustomerDashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CustomerDashboardTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        authService = new AuthService();
    }

    // Expected: CustomerDashboard can be constructed with valid user/auth service.
    @Test
    void testCustomerDashboard_Constructs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", false, "Customer User");
        CustomerDashboard frame = assertDoesNotThrow(() -> new CustomerDashboard(user, authService));
        frame.dispose();
    }

    // Expected: customer dashboard creates expected tabbed sections.
    @Test
    void testCustomerDashboard_HasExpectedTabs() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", false, "Customer User");
        CustomerDashboard frame = new CustomerDashboard(user, authService);
        try {
            List<JTabbedPane> tabbedPanes = UiTestUtils.findAllOfType(frame, JTabbedPane.class);
            assertEquals(0, tabbedPanes.size(), "CustomerDashboard uses CardLayout, not JTabbedPane.");
            assertTrue(UiTestUtils.findLabelByText(frame, "IPOS-PU - Customer Portal").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Browse Catalogue").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Basket").isPresent());
        } finally {
            frame.dispose();
        }
    }

    // Expected: customer navigation controls are present.
    @Test
    void testCustomerDashboard_HasSideMenuButtons() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping UI tests in headless environment.");
        User user = new User("customer@ipos.com", "Test123!", "CUSTOMER", false, "Customer User");
        CustomerDashboard frame = new CustomerDashboard(user, authService);
        try {
            assertTrue(UiTestUtils.findButtonByText(frame, "Home").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Browse Catalogue").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Basket").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Promotions").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Track Orders").isPresent());
            assertTrue(UiTestUtils.findButtonByText(frame, "Logout").isPresent());
        } finally {
            frame.dispose();
        }
    }
}
