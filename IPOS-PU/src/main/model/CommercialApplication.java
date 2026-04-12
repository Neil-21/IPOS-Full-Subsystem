package main.model;

import java.time.LocalDateTime;

public class CommercialApplication {

    private final String applicationId;
    private final String companyName;
    private final String businessType;
    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String postcode;
    private final String companyHouseRegistration;
    private final String directorName;
    private final String directorContact;
    private final String email;
    private final String notificationMethod;
    private final String status;
    private final LocalDateTime submittedAt;

    public CommercialApplication(String applicationId,
                                 String companyName,
                                 String businessType,
                                 String addressLine1,
                                 String addressLine2,
                                 String city,
                                 String postcode,
                                 String companyHouseRegistration,
                                 String directorName,
                                 String directorContact,
                                 String email,
                                 String notificationMethod,
                                 String status,
                                 LocalDateTime submittedAt) {
        this.applicationId = applicationId;
        this.companyName = companyName;
        this.businessType = businessType;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.postcode = postcode;
        this.companyHouseRegistration = companyHouseRegistration;
        this.directorName = directorName;
        this.directorContact = directorContact;
        this.email = email;
        this.notificationMethod = notificationMethod;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getApplicationId() { return applicationId; }
    public String getCompanyName() { return companyName; }
    public String getBusinessType() { return businessType; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getPostcode() { return postcode; }
    public String getCompanyHouseRegistration() { return companyHouseRegistration; }
    public String getDirectorName() { return directorName; }
    public String getDirectorContact() { return directorContact; }
    public String getEmail() { return email; }
    public String getNotificationMethod() { return notificationMethod; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}