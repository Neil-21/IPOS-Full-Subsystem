package main.model;

public class PUApplicationRequest {
    private String applicationId;
    private String type;
    private String email;
    private String companyName;
    private String companyHouseReg;
    private String directorName;
    private String businessType;
    private String address;

    public PUApplicationRequest() {}

    public PUApplicationRequest(String applicationId, String type, String email,
                                String companyName, String companyHouseReg,
                                String directorName, String businessType, String address) {
        this.applicationId = applicationId;
        this.type = type;
        this.email = email;
        this.companyName = companyName;
        this.companyHouseReg = companyHouseReg;
        this.directorName = directorName;
        this.businessType = businessType;
        this.address = address;
    }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyHouseReg() { return companyHouseReg; }
    public void setCompanyHouseReg(String companyHouseReg) { this.companyHouseReg = companyHouseReg; }

    public String getDirectorName() { return directorName; }
    public void setDirectorName(String directorName) { this.directorName = directorName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}