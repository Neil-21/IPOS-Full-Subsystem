package iposca.model;

import java.math.BigDecimal;
import java.util.List;

public class DiscountPlan {
    private int discountPlanID;
    private String planName;
    private String planType;         // "FIXED" or "FLEXIBLE" (uppercase now)
    private BigDecimal discountPercentage;
    private String description;
    private boolean isActive;
    private List<FlexibleDiscountTier> tiers;

    public DiscountPlan() {}

    public int getDiscountPlanID() { return discountPlanID; }
    public void setDiscountPlanID(int id) { this.discountPlanID = id; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal d) { this.discountPercentage = d; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public List<FlexibleDiscountTier> getTiers() { return tiers; }
    public void setTiers(List<FlexibleDiscountTier> tiers) { this.tiers = tiers; }

    public BigDecimal calculateDiscount(BigDecimal purchaseAmount) {
        if ("FIXED".equals(planType) && discountPercentage != null) {
            return purchaseAmount.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100));
        }
        if ("FLEXIBLE".equals(planType) && tiers != null) {
            for (FlexibleDiscountTier tier : tiers) {
                boolean aboveMin = purchaseAmount.compareTo(tier.getMinValue()) >= 0;
                boolean belowMax = tier.getMaxValue() == null ||
                        purchaseAmount.compareTo(tier.getMaxValue()) <= 0;
                if (aboveMin && belowMax) {
                    return purchaseAmount.multiply(tier.getDiscountRate())
                            .divide(BigDecimal.valueOf(100));
                }
            }
        }
        return BigDecimal.ZERO;
    }
}