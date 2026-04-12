package main.api;

public interface SAMerchantApplicationAPI {

    String submitMerchantApplication(String application);

    String getApplicationStatus(String applicationId);
}