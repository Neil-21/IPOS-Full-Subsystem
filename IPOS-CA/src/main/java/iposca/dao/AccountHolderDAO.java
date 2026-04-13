package iposca.dao;

import iposca.db.DatabaseManager;
import iposca.model.AccountHolder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountHolderDAO {

    public AccountHolder findByID(String accountId) throws SQLException {
        String sql = "SELECT * FROM account_holders WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<AccountHolder> getAll() throws SQLException {
        List<AccountHolder> list = new ArrayList<>();
        String sql = "SELECT * FROM account_holders ORDER BY full_name";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean insert(AccountHolder ah) throws SQLException {
        String sql = "INSERT INTO account_holders " +
                "(account_id, full_name, address, phone, email, " +
                "credit_limit, current_balance, account_status, discount_plan_id, " +
                "status_1st_reminder, status_2nd_reminder) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0.00, 'Normal', ?, 'no_need', 'no_need')";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, ah.getAccountID());
            stmt.setString(2, ah.getFullName());
            stmt.setString(3, ah.getAddress());
            stmt.setString(4, ah.getPhone());
            stmt.setString(5, ah.getEmail());
            stmt.setBigDecimal(6, ah.getCreditLimit());
            stmt.setObject(7, ah.getDiscountPlanID());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean update(AccountHolder ah) throws SQLException {
        String sql = "UPDATE account_holders SET full_name=?, address=?, phone=?, " +
                "email=?, credit_limit=?, discount_plan_id=? " +
                "WHERE account_id=?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, ah.getFullName());
            stmt.setString(2, ah.getAddress());
            stmt.setString(3, ah.getPhone());
            stmt.setString(4, ah.getEmail());
            stmt.setBigDecimal(5, ah.getCreditLimit());
            stmt.setObject(6, ah.getDiscountPlanID());
            stmt.setString(7, ah.getAccountID());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(String accountId) throws SQLException {
        String sql = "DELETE FROM account_holders WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, accountId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(String accountId, String status) throws SQLException {
        String sql = "UPDATE account_holders SET account_status = ? " +
                "WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, accountId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateReminderStatus(String accountId, String type,
                                        String status) throws SQLException {
        String col = "1st".equals(type)
                ? "status_1st_reminder" : "status_2nd_reminder";
        String sql = "UPDATE account_holders SET " + col + " = ? WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, accountId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean setDate2ndReminder(String accountId,
                                      java.time.LocalDate date) throws SQLException {
        String sql = "UPDATE account_holders SET date_2nd_reminder = ? " +
                "WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setString(2, accountId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean addToBalance(String accountId,
                                java.math.BigDecimal amount) throws SQLException {
        String sql = "UPDATE account_holders " +
                "SET current_balance = current_balance + ? " +
                "WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, accountId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateBalance(String accountId,
                                 java.math.BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE account_holders SET current_balance = ? " +
                "WHERE account_id = ?";
        try (PreparedStatement stmt =
                     DatabaseManager.getConnection().prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, accountId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<AccountHolder> getAccountsWithReminderDue() throws SQLException {
        List<AccountHolder> list = new ArrayList<>();
        String sql = "SELECT * FROM account_holders " +
                "WHERE status_1st_reminder = 'due' " +
                "OR status_2nd_reminder = 'due'";
        try (Statement stmt = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private AccountHolder mapRow(ResultSet rs) throws SQLException {
        AccountHolder ah = new AccountHolder();
        ah.setAccountID(rs.getString("account_id"));
        ah.setFullName(rs.getString("full_name"));
        ah.setAddress(rs.getString("address"));
        ah.setPhone(rs.getString("phone"));
        ah.setEmail(rs.getString("email"));
        ah.setCreditLimit(rs.getBigDecimal("credit_limit"));
        ah.setCurrentBalance(rs.getBigDecimal("current_balance"));
        ah.setAccountStatus(rs.getString("account_status"));
        ah.setDiscountPlanID(rs.getObject("discount_plan_id", Integer.class));
        ah.setStatus1stReminder(rs.getString("status_1st_reminder"));
        ah.setStatus2ndReminder(rs.getString("status_2nd_reminder"));
        Date d1 = rs.getDate("date_1st_reminder");
        Date d2 = rs.getDate("date_2nd_reminder");
        if (d1 != null) ah.setDate1stReminder(d1.toLocalDate());
        if (d2 != null) ah.setDate2ndReminder(d2.toLocalDate());
        return ah;
    }
}