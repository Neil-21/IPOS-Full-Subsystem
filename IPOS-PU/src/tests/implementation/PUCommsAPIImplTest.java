package tests.implementation;

import main.implementation.PUCommsAPIImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PUCommsAPIImplTest {

    private PUCommsAPIImpl comms;

    @BeforeEach
    void setUp() {
        comms = new PUCommsAPIImpl();
    }

    // Expected: sendEmail returns true for valid recipient and body.
    @Test
    void testSendEmail_ValidInput_ReturnsTrue() {
        boolean sent = comms.sendEmail("customer@ipos.com", "Welcome", "Your account is ready.");
        assertTrue(sent);
    }

    // Expected: sendEmail returns false when recipient is null/blank.
    @Test
    void testSendEmail_InvalidRecipient_ReturnsFalse() {
        assertFalse(comms.sendEmail(null, "Subject", "Body"));
        assertFalse(comms.sendEmail(" ", "Subject", "Body"));
    }

    // Expected: sendEmail returns false when body is null/blank.
    @Test
    void testSendEmail_InvalidBody_ReturnsFalse() {
        assertFalse(comms.sendEmail("customer@ipos.com", "Subject", null));
        assertFalse(comms.sendEmail("customer@ipos.com", "Subject", " "));
    }

    // Expected: authorisePayment(orderId, amount, cardNumber) returns true for valid inputs.
    @Test
    void testAuthorisePayment_WithCard_ValidInput_ReturnsTrue() {
        boolean authorised = comms.authorisePayment("ORD-1001", 25.50, "4242 4242 4242 4242");
        assertTrue(authorised);
    }

    // Expected: authorisePayment(orderId, amount, cardNumber) returns false for invalid inputs.
    @Test
    void testAuthorisePayment_WithCard_InvalidInput_ReturnsFalse() {
        assertFalse(comms.authorisePayment(null, 25.50, "424242424242"));
        assertFalse(comms.authorisePayment(" ", 25.50, "424242424242"));
        assertFalse(comms.authorisePayment("ORD-1002", 0, "424242424242"));
        assertFalse(comms.authorisePayment("ORD-1002", -1, "424242424242"));
        assertFalse(comms.authorisePayment("ORD-1002", 10.0, null));
        assertFalse(comms.authorisePayment("ORD-1002", 10.0, "1234"));
    }

    // Expected: two-argument authorisePayment delegates successfully for valid order/amount.
    @Test
    void testAuthorisePayment_TwoArgs_ValidInput_ReturnsTrue() {
        boolean authorised = comms.authorisePayment("ORD-2001", 19.99);
        assertTrue(authorised);
    }

    // Expected: two-argument authorisePayment returns false for invalid order/amount.
    @Test
    void testAuthorisePayment_TwoArgs_InvalidInput_ReturnsFalse() {
        assertFalse(comms.authorisePayment(null, 19.99));
        assertFalse(comms.authorisePayment(" ", 19.99));
        assertFalse(comms.authorisePayment("ORD-2002", 0));
    }

    // Expected: recordTransaction handles valid inputs without throwing.
    @Test
    void testRecordTransaction_ValidInput_DoesNotThrow() {
        assertDoesNotThrow(() ->
                comms.recordTransaction("REF-001", "email", "success", "2026-01-01T12:00:00"));
    }
}
