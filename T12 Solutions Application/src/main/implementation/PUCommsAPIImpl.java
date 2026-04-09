package main.implementation;

import main.api.PUCommsAPI;

import java.time.LocalDateTime;

public class PUCommsAPIImpl implements PUCommsAPI {

    public PUCommsAPIImpl() {
    }

    /**
      Simulates sending an email by printing details
      Returns false if the recipient address or body is null or empty.
      @param to      email address
      @param subject the email subject line
      @param body    the body text
     */
    @Override
    public boolean sendEmail(String to, String subject, String body) {
        if (to == null || to.trim().isEmpty() || body == null || body.trim().isEmpty()) {
            System.out.println("[EMAIL] Failed: recipient or body is missing.");
            return false;
        }
        System.out.println("[EMAIL] To: " + to);
        System.out.println("[EMAIL] Subject: " + subject);
        System.out.println("[EMAIL] Body: " + body);
        recordTransaction("EMAIL_" + System.currentTimeMillis(), "email", "success",
                LocalDateTime.now().toString());
        return true;
    }

    /**
      Full payment authorisation with card number validation.
      Validates orderId, amount, and card number before simulating authorisation. Card number must be at least 12 digits. Only the last 4 digits are shown in the output.
      Returns false if any input is invalid.
      @param orderId    the unique ID of the order being paid for
      @param amount     the total amount to charge
      @param cardNumber the customer's card number
     */
    public boolean authorisePayment(String orderId, double amount, String cardNumber) {
        if (orderId == null || orderId.trim().isEmpty()) {
            System.out.println("[PAYMENT] Failed: orderId is null or empty.");
            return false;
        }
        if (amount <= 0) {
            System.out.println("[PAYMENT] Failed: amount must be greater than 0.");
            return false;
        }
        if (cardNumber == null || cardNumber.replaceAll("\\s", "").length() < 12) {
            System.out.println("[PAYMENT] Failed: card number is invalid.");
            return false;
        }
        // Mask card: show only last 4 digits
        String digits = cardNumber.replaceAll("\\s", "");
        String masked = "**** **** **** " + digits.substring(digits.length() - 4);
        System.out.println("[PAYMENT] Authorised: Order " + orderId
                + " | Amount: £" + String.format("%.2f", amount)
                + " | Card: " + masked);
        recordTransaction("PAY_" + orderId, "payment", "success",
                LocalDateTime.now().toString());
        return true;
    }

    /**
      Delegates to the full three-argument version.
      @param orderId the unique ID of the order being paid for
      @param amount  the total amount to charge in GBP; must be greater than 0
     */
    @Override
    public boolean authorisePayment(String orderId, double amount) {
        return authorisePayment(orderId, amount, "000000000000");
    }

    /**
     * Records a transaction to the console (prototype substitute for database logging).
      @param refId     a unique reference ID
      @param type      type of transaction
      @param outcome   result
      @param timestamp date and time of the transaction
     */
    @Override
    public void recordTransaction(String refId, String type, String outcome, String timestamp) {
        System.out.println("[TRANSACTION] Ref: " + refId
                + " | Type: " + type
                + " | Outcome: " + outcome
                + " | Time: " + timestamp);
    }
}
