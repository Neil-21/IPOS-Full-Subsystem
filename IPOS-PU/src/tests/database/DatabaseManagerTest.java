package tests.database;

import main.db.DatabaseManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {

    // Expected: initialise creates all required core tables.
    @Test
    void testInitialise_CreatesRequiredTables() throws SQLException {
        DatabaseManager.initialise();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            assertTrue(tableExists(stmt, "users"));
            assertTrue(tableExists(stmt, "campaigns"));
            assertTrue(tableExists(stmt, "campaign_items"));
            assertTrue(tableExists(stmt, "products"));
            assertTrue(tableExists(stmt, "orders"));
            assertTrue(tableExists(stmt, "order_items"));
        }
    }

    // Expected: initialise seeds default users when user table is empty.
    @Test
    void testInitialise_SeedsDefaultUsers() throws SQLException {
        DatabaseManager.initialise();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             //noinspection SqlResolve
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {

            int count = rs.getInt(1);
            assertTrue(count >= 2, "Expected at least the default customer/admin users.");
        }
    }

    private boolean tableExists(Statement stmt, String tableName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
        try (ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
}
