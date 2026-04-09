package main.service;

import main.model.Campaign;

import java.util.ArrayList;
import java.util.List;

public class CampaignStore {

    private static final List<Campaign> campaigns = new ArrayList<>();

    public static void addCampaign(Campaign campaign) {
        if (campaign != null) {
            campaigns.add(campaign);
        }
    }

    public static List<Campaign> getAllCampaigns() {
        return new ArrayList<>(campaigns);
    }

    public static List<Campaign> getActiveCampaigns() {
        List<Campaign> active = new ArrayList<>();
        for (Campaign c : campaigns) {
            if (c.isActive()) {
                active.add(c);
            }
        }
        return active;
    }

    public static Campaign findById(String campaignId) {
        for (Campaign c : campaigns) {
            if (c.getCampaignId().equalsIgnoreCase(campaignId)) {
                return c;
            }
        }
        return null;
    }

    public static boolean removeCampaign(String campaignId) {
        return campaigns.removeIf(c -> c.getCampaignId().equalsIgnoreCase(campaignId));
    }
}