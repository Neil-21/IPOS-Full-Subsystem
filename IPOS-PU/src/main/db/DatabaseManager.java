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

        String campaignMetricsTable = """
            CREATE TABLE IF NOT EXISTS campaign_metrics (
                campaign_id    TEXT    PRIMARY KEY,
                campaign_hits  INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (campaign_id) REFERENCES campaigns(campaign_id)
            );
        """;

        String campaignItemMetricsTable = """
            CREATE TABLE IF NOT EXISTS campaign_item_metrics (
                campaign_id      TEXT    NOT NULL,
                item_id          TEXT    NOT NULL,
                item_hits        INTEGER NOT NULL DEFAULT 0,
                item_purchases   INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (campaign_id, item_id),
                FOREIGN KEY (campaign_id) REFERENCES campaigns(campaign_id)
            );
        """;

        String productsTable = """
            CREATE TABLE IF NOT EXISTS products (
                product_id    TEXT    PRIMARY KEY,
                product_name  TEXT    NOT NULL,
                category      TEXT    NOT NULL,
                price         REAL    NOT NULL,
                stock         INTEGER NOT NULL
            );
        """;

        String ordersTable = """
            CREATE TABLE IF NOT EXISTS orders (
                order_id      TEXT    PRIMARY KEY,
                user_email    TEXT    NOT NULL,
                order_date    TEXT    NOT NULL,
                item_count    INTEGER NOT NULL,
                status        TEXT    NOT NULL,
                total_amount  REAL    NOT NULL,
                delivery_name             TEXT,
                delivery_address_line_1   TEXT,
                delivery_address_line_2   TEXT,
                delivery_city             TEXT,
                delivery_postcode         TEXT,
                tracking_ref              TEXT,
                FOREIGN KEY (user_email) REFERENCES users(email)
            );
        """;

        String orderItemsTable = """
            CREATE TABLE IF NOT EXISTS order_items (
                order_id      TEXT    NOT NULL,
                product_id    TEXT    NOT NULL,
                product_name  TEXT    NOT NULL,
                quantity      INTEGER NOT NULL,
                unit_price    REAL    NOT NULL,
                line_total    REAL    NOT NULL,
                campaign_id   TEXT,
                FOREIGN KEY (order_id) REFERENCES orders(order_id)
            );
        """;

        String commercialApplicationsTable = """
            CREATE TABLE IF NOT EXISTS commercial_applications (
                    application_id               TEXT    PRIMARY KEY,
                    company_name                 TEXT    NOT NULL,
                    business_type                TEXT    NOT NULL,
                    address_line_1               TEXT    NOT NULL,
                    address_line_2               TEXT,
                    city                         TEXT    NOT NULL,
                    postcode                     TEXT    NOT NULL,
                    company_house_registration   TEXT    NOT NULL,
                    director_name                TEXT    NOT NULL,
                    director_contact             TEXT    NOT NULL,
                    email                        TEXT    NOT NULL,
                    notification_method          TEXT    NOT NULL,
                    status                       TEXT    NOT NULL DEFAULT 'PENDING',
                    submitted_at                 TEXT    NOT NULL
                );
        """;

        String paymentsTable  = """
            CREATE TABLE IF NOT EXISTS payments (
                payment_id     TEXT    PRIMARY KEY,
                order_id       TEXT    NOT NULL,
                user_email     TEXT    NOT NULL,
                address_line_1 TEXT,
                address_line_2 TEXT,
                payment_date   TEXT    NOT NULL DEFAULT(datetime('now')),
                payment_status TEXT    NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY (user_email) REFERENCES users(email),
                FOREIGN KEY (order_id) REFERENCES orders(order_id),
                CHECK (payment_status IN ('PENDING','COMPLETED','FAILED','REFUNDED'))
            );
        """;
        
        String paymentInfo = """
                SELECT p.payment_id,
                       u.full_name,
                       u.email,
                       o.order_id,
                       oi.product_id,
                       oi.product_name,
                       oi.quantity,
                       oi.line_total,
                       p.payment_date,
                       p.address_line_1,
                       p.address_line_2,
                       p.payment_status
                FROM payments p 
                JOIN orders o ON p.order_id = o.order_id
                JOIN order_items oi ON o.order_id = oi.order_id
                JOIN users u ON p.user_email = u.email
                """;
        
        /*
        String paymentsTable  = """
            CREATE TABLE IF NOT EXISTS payments (
                payment_id     TEXT    PRIMARY KEY,
                order_id       TEXT    NOT NULL,
                user_email     TEXT    NOT NULL,
                address_line_1 TEXT    NOT NULL,
                address_line_2 TEXT    DEFAULT NULL,
                product_id     TEXT    NOT NULL,
                product_name   TEXT    NOT NULL,
                quantity       INTEGER NOT NULL, 
                payment_total  REAL    NOT NULL,
                payment_date   TEXT    NOT NULL DEFAULT(datetime('now')),
                status         TEXT    NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY (user_email) REFERENCES users(email),
                FOREIGN KEY (order_id) REFERENCES orders(order_id),
                CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED'))
            );
        """;
        */
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(campaignsTable);
            stmt.execute(campaignItemsTable);
            stmt.execute(campaignMetricsTable);
            stmt.execute(campaignItemMetricsTable);
            stmt.execute(productsTable);
            stmt.execute(ordersTable);
            stmt.execute(orderItemsTable);
            stmt.execute(commercialApplicationsTable);
            stmt.execute(paymentsTable);
            stmt.execute(paymentInfo);
            seedUsersIfEmpty(conn);
            seedProductsIfEmpty(conn);
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

    private static void seedProductsIfEmpty(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM products")) {
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute("""
                INSERT INTO products (product_id, product_name, category, price, stock) VALUES
                ('PARA001', 'Paracetamol 500mg (16 tablets)', 'Pain relief', 2.99, 120),
                ('IBU002', 'Ibuprofen 400mg (24 tablets)', 'Anti-inflammatory', 4.49, 85),
                ('VIT003', 'Vitamin D3 1000IU (90 capsules)', 'Supplements', 6.99, 200),
                ('ALL004', 'Allergy Relief (Cetirizine 10mg)', 'Antihistamine', 3.79, 45),
                ('BAND005', 'Bandages & Plasters Pack', 'First Aid', 5.49, 30)
            """);
            }
        }
    }
}
