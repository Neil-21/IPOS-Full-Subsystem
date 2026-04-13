package iposca.model;

import java.math.BigDecimal;

public class FlexibleDiscountTier {
    private int tierID;
    private int discountPlanID;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal discountRate;

    public FlexibleDiscountTier() {}

    public int getTierID() { return tierID; }
    public void setTierID(int tierID) { this.tierID = tierID; }

    public int getDiscountPlanID() { return discountPlanID; }
    public void setDiscountPlanID(int id) { this.discountPlanID = id; }

    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }

    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }

    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
}