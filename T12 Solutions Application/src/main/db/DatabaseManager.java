package main.db;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:ipos_pu.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialise() {
        String usersTable = """
            CREATE TABLE IF NOT EXISTS users (
                email       TEXT    PRIMARY KEY,
                full_name   TEXT    NOT NULL,
                password    TEXT    NOT NULL,
                role        TEXT    NOT NULL DEFAULT 'CUSTOMER',
                first_login INTEGER NOT NULL DEFAULT 1
            );
        """;

        String campaignsTable = """
            CREATE TABLE IF NOT EXISTS campaigns (
                campaign_id   TEXT    PRIMARY KEY,
                start_date    TEXT    NOT NULL,
                end_date      TEXT    NOT NULL,
                discount_type TEXT    NOT NULL,
                cancelled     INTEGER NOT NULL DEFAULT 0
            );
        """;

        String campaignItemsTable = """
            CREATE TABLE IF NOT EXISTS campaign_items (
                campaign_id   TEXT    NOT NULL,
                item_id       TEXT    NOT NULL,
                discount_rate REAL    NOT NULL,
                PRIMARY KEY (campaign_id, item_id),
                FOREIGN KEY (campaign_id) REFERENCES campaigns(campaign_id)
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(campaignsTable);
            stmt.execute(campaignItemsTable);
            seedUsersIfEmpty(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static void seedUsersIfEmpty(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute("""
                    INSERT INTO users VALUES
                    ('customer@ipos.com', 'Test Customer', 'Test123!', 'CUSTOMER', 1),
                    ('admin@ipos.com',    'System Admin',  'Admin123!', 'ADMIN',   0)
                """);
            }
        }
    }
}