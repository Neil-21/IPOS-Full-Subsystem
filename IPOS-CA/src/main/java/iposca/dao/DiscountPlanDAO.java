package iposca.dao;

import iposca.db.DatabaseManager;
import iposca.model.DiscountPlan;
import iposca.model.FlexibleDiscountTier;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiscountPlanDAO {

    public DiscountPlan findByID(int discountPlanID) throws SQLException {
        String sql = "SELECT * FROM discount_plans WHERE discount_plan_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, discountPlanID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                DiscountPlan plan = mapRow(rs);
                if ("FLEXIBLE".equals(plan.getPlanType())) {
                    plan.setTiers(getTiersForPlan(discountPlanID));
                }
                return plan;
            }
        }
        return null;
    }

    public List<DiscountPlan> getAll() throws SQLException {
        List<DiscountPlan> list = new ArrayList<>();
        String sql = "SELECT DISTINCT ON (plan_name) * " +
                "FROM ca.discount_plans " +
                "WHERE is_active = TRUE " +
                "ORDER BY plan_name, discount_plan_id";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private List<FlexibleDiscountTier> getTiersForPlan(int planId) throws SQLException {
        List<FlexibleDiscountTier> tiers = new ArrayList<>();
        // note: table is discount_tiers (not flexible_discount_tiers)
        String sql = "SELECT * FROM discount_tiers " +
                "WHERE discount_plan_id = ? ORDER BY min_value";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, planId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FlexibleDiscountTier tier = new FlexibleDiscountTier();
                tier.setTierID(rs.getInt("tier_id"));
                tier.setDiscountPlanID(rs.getInt("discount_plan_id"));
                tier.setMinValue(rs.getBigDecimal("min_value"));
                tier.setMaxValue(rs.getBigDecimal("max_value"));
                tier.setDiscountRate(rs.getBigDecimal("discount_rate"));
                tiers.add(tier);
            }
        }
        return tiers;
    }

    public boolean delete(int discountPlanId) throws SQLException {
        String sql = "DELETE FROM ca.discount_plans WHERE discount_plan_id = ?";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, discountPlanId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean insert(String planName, String planType,
                          java.math.BigDecimal discountPercentage) throws SQLException {
        String sql = "INSERT INTO ca.discount_plans (plan_name, plan_type, discount_percentage) " +
                "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, planName);
            stmt.setString(2, planType);
            stmt.setBigDecimal(3, discountPercentage);
            return stmt.executeUpdate() > 0;
        }
    }

    private DiscountPlan mapRow(ResultSet rs) throws SQLException {
        DiscountPlan plan = new DiscountPlan();
        plan.setDiscountPlanID(rs.getInt("discount_plan_id"));
        plan.setPlanName(rs.getString("plan_name"));
        plan.setPlanType(rs.getString("plan_type"));
        plan.setDiscountPercentage(rs.getBigDecimal("discount_percentage"));
        plan.setDescription(rs.getString("description"));
        plan.setActive(rs.getBoolean("is_active"));
        return plan;
    }
}