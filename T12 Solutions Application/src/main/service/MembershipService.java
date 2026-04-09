package main.service;

import java.util.ArrayList;
import java.util.List;

public class MembershipService {

    private final List<String> commercialApplications = new ArrayList<>();

    public boolean submitCommercialApplication(
            String companyName,
            String companyAddress,
            String directorName,
            String directorContact,
            String notificationMethod
    ) {
        if (companyName == null || companyName.trim().isEmpty()) return false;
        if (companyAddress == null || companyAddress.trim().isEmpty()) return false;
        if (directorName == null || directorName.trim().isEmpty()) return false;
        if (directorContact == null || directorContact.trim().isEmpty()) return false;
        if (notificationMethod == null || notificationMethod.trim().isEmpty()) return false;

        String application = "Company: " + companyName
                + ", Address: " + companyAddress
                + ", Director: " + directorName
                + ", Contact: " + directorContact
                + ", Notify by: " + notificationMethod;

        commercialApplications.add(application);
        return true;
    }

    public List<String> getCommercialApplications() {
        return commercialApplications;
    }
}