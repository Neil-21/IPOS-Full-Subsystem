package main.api;

public interface PUCommsAPI {

    boolean sendEmail(String to, String subject, String body);

    boolean authorisePayment(String orderId, double amount);

    void recordTransaction(String refId, String type, String outcome, String timestamp);
}