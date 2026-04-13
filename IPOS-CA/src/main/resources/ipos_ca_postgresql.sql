-- IPOS-CA PostgreSQL Schema
-- InfoPharma Ordering System - Client Application
-- Run this once against the Railway PostgreSQL database to set up CA tables.
-- All CA tables live in the 'ca' schema to avoid conflicts with SA ('public' schema).

CREATE SCHEMA IF NOT EXISTS ca;
SET search_path TO ca;

-- TABLE 1: USERS

CREATE TABLE IF NOT EXISTS users (
    user_id     SERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('Admin', 'Pharmacist', 'Manager')),
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_login  TIMESTAMP    NULL
);

-- TABLE 2: MERCHANT_CONFIG

CREATE TABLE IF NOT EXISTS merchant_config (
    config_id        SERIAL PRIMARY KEY,
    merchant_name    VARCHAR(100) NOT NULL,
    merchant_address TEXT,
    merchant_phone   VARCHAR(20),
    merchant_email   VARCHAR(100),
    merchant_logo_path VARCHAR(255),
    vat_rate         NUMERIC(5,4) NOT NULL DEFAULT 0.0000,
    markup_rate      NUMERIC(5,4) NOT NULL DEFAULT 1.0000,
    low_stock_threshold INT DEFAULT 10,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by       INT,
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

-- TABLE 3: STOCK_ITEMS

CREATE TABLE IF NOT EXISTS stock_items (
    product_id    VARCHAR(20)   PRIMARY KEY,
    product_name  VARCHAR(100)  NOT NULL,
    description   TEXT,
    unit_type     VARCHAR(20),
    form          VARCHAR(20),
    pack_size     INT,
    wholesale_cost NUMERIC(10,2) NOT NULL,
    retail_price  NUMERIC(10,2) NOT NULL,
    current_stock INT           NOT NULL DEFAULT 0,
    reorder_level INT           DEFAULT 10,
    supplier_code VARCHAR(50),
    is_active     BOOLEAN       DEFAULT TRUE,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- TABLE 4: DISCOUNT_PLANS

CREATE TABLE IF NOT EXISTS discount_plans (
    discount_plan_id   SERIAL PRIMARY KEY,
    plan_name          VARCHAR(50) NOT NULL,
    plan_type          VARCHAR(10) NOT NULL CHECK (plan_type IN ('FIXED', 'FLEXIBLE')),
    discount_percentage NUMERIC(5,2),
    min_purchase_amount NUMERIC(10,2),
    description        TEXT,
    is_active          BOOLEAN   DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TABLE 4b: DISCOUNT_TIERS

CREATE TABLE IF NOT EXISTS discount_tiers (
    tier_id          SERIAL PRIMARY KEY,
    discount_plan_id INT            NOT NULL,
    min_value        NUMERIC(10,2)  NOT NULL,
    max_value        NUMERIC(10,2),
    discount_rate    NUMERIC(5,2)   NOT NULL,
    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(discount_plan_id) ON DELETE CASCADE
);

-- TABLE 5: ACCOUNT_HOLDERS

CREATE TABLE IF NOT EXISTS account_holders (
    account_id         VARCHAR(20)  PRIMARY KEY,
    full_name          VARCHAR(100) NOT NULL,
    address            TEXT,
    phone              VARCHAR(20),
    email              VARCHAR(100),
    credit_limit       NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    current_balance    NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    account_status     VARCHAR(20)  DEFAULT 'Normal' CHECK (account_status IN ('Normal', 'Suspended', 'In Default')),
    discount_plan_id   INT,
    status_1st_reminder VARCHAR(10) DEFAULT 'no_need' CHECK (status_1st_reminder IN ('no_need', 'due', 'sent')),
    status_2nd_reminder VARCHAR(10) DEFAULT 'no_need' CHECK (status_2nd_reminder IN ('no_need', 'due', 'sent')),
    date_1st_reminder  DATE NULL,
    date_2nd_reminder  DATE NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(discount_plan_id)
);

CREATE INDEX IF NOT EXISTS idx_ah_status  ON account_holders(account_status);
CREATE INDEX IF NOT EXISTS idx_ah_balance ON account_holders(current_balance);
CREATE INDEX IF NOT EXISTS idx_ah_reminders ON account_holders(status_1st_reminder, status_2nd_reminder);

-- TABLE 6: SALES

CREATE TABLE IF NOT EXISTS sales (
    sale_id        SERIAL PRIMARY KEY,
    sale_reference VARCHAR(30)  UNIQUE NOT NULL,
    sale_date      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    customer_type  VARCHAR(30)  NOT NULL CHECK (customer_type IN ('Account Holder', 'Occasional Customer')),
    account_id     VARCHAR(20)  NULL,
    subtotal       NUMERIC(10,2) NOT NULL,
    discount_amount NUMERIC(10,2) DEFAULT 0.00,
    vat_amount     NUMERIC(10,2) NOT NULL,
    total_amount   NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(10)  NOT NULL CHECK (payment_method IN ('Cash', 'Card', 'Credit')),
    payment_status VARCHAR(10)  DEFAULT 'Paid' CHECK (payment_status IN ('Paid', 'Pending', 'Failed')),
    served_by      INT          NOT NULL,
    notes          TEXT,
    FOREIGN KEY (account_id) REFERENCES account_holders(account_id),
    FOREIGN KEY (served_by)  REFERENCES users(user_id)
);

-- TABLE 7: SALE_ITEMS

CREATE TABLE IF NOT EXISTS sale_items (
    item_id    SERIAL PRIMARY KEY,
    sale_id    INT           NOT NULL,
    product_id VARCHAR(20)   NOT NULL,
    quantity   INT           NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    line_total NUMERIC(10,2) NOT NULL,
    FOREIGN KEY (sale_id)    REFERENCES sales(sale_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES stock_items(product_id)
);

-- TABLE 8: PAYMENT_RECORDS

CREATE TABLE IF NOT EXISTS payment_records (
    payment_id       SERIAL PRIMARY KEY,
    sale_id          INT          NOT NULL,
    payment_method   VARCHAR(10)  NOT NULL CHECK (payment_method IN ('Cash', 'Card', 'Credit')),
    amount           NUMERIC(10,2) NOT NULL,
    payment_date     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    card_type        VARCHAR(20),
    card_first_four  CHAR(4),
    card_last_four   CHAR(4),
    card_expiry_month INT,
    card_expiry_year  INT,
    authorization_code VARCHAR(50),
    reduces_balance  BOOLEAN      DEFAULT FALSE,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
);

-- TABLE 9: ORDERS_TO_INFOPHARMA

CREATE TABLE IF NOT EXISTS orders_to_infopharma (
    order_id        SERIAL PRIMARY KEY,
    order_reference VARCHAR(30)  UNIQUE NOT NULL,
    order_date      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    order_status    VARCHAR(20)  DEFAULT 'Draft'
                    CHECK (order_status IN ('Draft','Submitted','Accepted','Processing','Dispatched','Delivered','Cancelled')),
    total_amount    NUMERIC(10,2),
    placed_by       INT          NOT NULL,
    dispatch_date   DATE         NULL,
    delivery_date   DATE         NULL,
    courier         VARCHAR(50),
    tracking_number VARCHAR(100),
    notes           TEXT,
    FOREIGN KEY (placed_by) REFERENCES users(user_id)
);

-- TABLE 10: ORDER_ITEMS

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id      INT           NOT NULL,
    item_id       VARCHAR(20)   NOT NULL,
    quantity      INT           NOT NULL,
    unit_cost     NUMERIC(10,2) NOT NULL,
    total_cost    NUMERIC(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders_to_infopharma(order_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id)  REFERENCES stock_items(product_id)
);

-- TABLE 11: REMINDERS

CREATE TABLE IF NOT EXISTS reminders (
    reminder_id   SERIAL PRIMARY KEY,
    account_id    VARCHAR(20)  NOT NULL,
    reminder_type VARCHAR(20)  NOT NULL CHECK (reminder_type IN ('1st Reminder', '2nd Reminder')),
    amount_owed   NUMERIC(10,2) NOT NULL,
    reminder_text TEXT         NOT NULL,
    date_generated DATE        NOT NULL,
    date_sent     DATE,
    sent_via      VARCHAR(10)  DEFAULT 'Print' CHECK (sent_via IN ('Print', 'Email')),
    status        VARCHAR(20)  DEFAULT 'Generated' CHECK (status IN ('Generated', 'Sent', 'Failed')),
    FOREIGN KEY (account_id) REFERENCES account_holders(account_id)
);

-- TABLE 12: REMINDER_TEMPLATES

CREATE TABLE IF NOT EXISTS reminder_templates (
    template_id   SERIAL PRIMARY KEY,
    template_type VARCHAR(20)  NOT NULL UNIQUE CHECK (template_type IN ('1st Reminder', '2nd Reminder')),
    subject       VARCHAR(255) NOT NULL,
    body_text     TEXT         NOT NULL,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by    INT,
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

-- TABLE 13: ONLINE_ORDERS

CREATE TABLE IF NOT EXISTS online_orders (
    online_order_id    SERIAL PRIMARY KEY,
    order_reference    VARCHAR(30)  UNIQUE NOT NULL,
    order_date         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    customer_email     VARCHAR(100),
    delivery_address   TEXT,
    items_json         TEXT         NOT NULL,
    total_amount       NUMERIC(10,2) NOT NULL,
    payment_status     VARCHAR(10)  DEFAULT 'Paid' CHECK (payment_status IN ('Pending', 'Paid', 'Failed')),
    fulfillment_status VARCHAR(30)  DEFAULT 'Received'
                       CHECK (fulfillment_status IN ('Received','Ready for Shipment','Dispatched','Delivered','Failed')),
    stock_deducted     BOOLEAN      DEFAULT FALSE,
    processed_at       TIMESTAMP    NULL,
    processed_by       INT,
    FOREIGN KEY (processed_by) REFERENCES users(user_id)
);

-- TABLE 14: ACCOUNT_HOLDER_PAYMENTS

CREATE TABLE IF NOT EXISTS account_holder_payments (
    payment_id        SERIAL PRIMARY KEY,
    account_id        VARCHAR(20)  NOT NULL,
    amount            NUMERIC(10,2) NOT NULL,
    payment_method    VARCHAR(20)  NOT NULL CHECK (payment_method IN ('Cash', 'Card', 'Bank Transfer')),
    card_type         VARCHAR(20),
    card_first_four   CHAR(4),
    card_last_four    CHAR(4),
    card_expiry_month INT,
    card_expiry_year  INT,
    payment_date      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    recorded_by       INT          NOT NULL,
    notes             TEXT,
    FOREIGN KEY (account_id)  REFERENCES account_holders(account_id),
    FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

-- ─── SEED DATA ────────────────────────────────────────────────────────────────

-- Users — credentials kept in sync with SA (source of truth)

INSERT INTO users (username, password_hash, role, full_name, is_active) VALUES
('sysdba',    'masterkey', 'Admin',      'System Administrator', TRUE),
('manager',   'Get_it_done',      'Manager',    'Operations Manager',   TRUE),
('accountant','Count_money',      'Pharmacist', 'Senior Accountant',    TRUE),
('clerk',     'Paperwork',        'Pharmacist', 'Accounts Clerk',       TRUE)
ON CONFLICT (username) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        is_active     = EXCLUDED.is_active;

-- Merchant config
INSERT INTO merchant_config (merchant_name, merchant_address, merchant_phone, vat_rate, markup_rate) VALUES
('Cosymed Ltd', '25, Bond Street, London WC1V 8LS', '0207 321 8001', 0.0000, 1.0000)
ON CONFLICT DO NOTHING;

-- Discount plans
INSERT INTO discount_plans (plan_name, plan_type, discount_percentage, description) VALUES
('Fixed 3%', 'FIXED', 3.00, 'Fixed 3% discount on all purchases')
ON CONFLICT DO NOTHING;

INSERT INTO discount_plans (plan_name, plan_type, description) VALUES
('Variable Volume', 'FLEXIBLE', 'Volume-based: 0% under £100, 1% £100-£300, 2% over £300')
ON CONFLICT DO NOTHING;

-- Discount tiers for plan_id 2 (Variable Volume)
DELETE FROM ca.discount_tiers;

INSERT INTO ca.discount_tiers (discount_plan_id, min_value, max_value, discount_rate)
SELECT discount_plan_id, 0.00, 99.99, 0.00
FROM ca.discount_plans WHERE plan_name = 'Variable Volume';

INSERT INTO ca.discount_tiers (discount_plan_id, min_value, max_value, discount_rate)
SELECT discount_plan_id, 100.00, 299.99, 1.00
FROM ca.discount_plans WHERE plan_name = 'Variable Volume';

INSERT INTO ca.discount_tiers (discount_plan_id, min_value, max_value, discount_rate)
SELECT discount_plan_id, 300.00, NULL, 2.00
FROM ca.discount_plans WHERE plan_name = 'Variable Volume';

-- Account holders
INSERT INTO account_holders (account_id, full_name, address, phone, credit_limit, current_balance, account_status, discount_plan_id) VALUES
('ACC0001', 'Ms Eva Bauyer',      '1, Liverpool Street, London EC2V 8NS', '0207 321 8001', 500.00, 0.00, 'Normal', 1),
('ACC0002', 'Mr Glynne Morrison', '1, Liverpool Street, London EC2V 8NS', '0207 321 8001', 500.00, 0.00, 'Normal', 2)
ON CONFLICT (account_id) DO NOTHING;

-- Stock items
INSERT INTO stock_items (product_id, product_name, unit_type, form, pack_size, wholesale_cost, retail_price, current_stock, reorder_level) VALUES
('100 00001', 'Paracetamol',           'box',    'Caps', 20, 0.10,  0.20,  121, 10),
('100 00002', 'Aspirin',               'box',    'Caps', 20, 0.50,  1.00,  201, 15),
('100 00003', 'Analgin',               'box',    'Caps', 10, 1.20,  2.40,  25,  10),
('100 00004', 'Celebrex, caps 100 mg', 'box',    'Caps', 10, 10.00, 20.00, 43,  10),
('100 00005', 'Celebrex, caps 200 mg', 'box',    'Caps', 10, 18.50, 37.00, 35,  5),
('100 00006', 'Retin-A Tretin, 30 g',  'box',    'Caps', 20, 25.00, 50.00, 28,  10),
('100 00007', 'Lipitor TB, 20 mg',     'box',    'Caps', 30, 15.50, 31.00, 10,  10),
('100 00008', 'Claritin CR, 60g',      'box',    'Caps', 20, 19.50, 39.00, 21,  10),
('200 00004', 'Iodine tincture',       'bottle', 'ml',  100, 0.30,  0.60,  35,  10),
('200 00005', 'Rhynol',                'bottle', 'ml',  200, 2.50,  5.00,  14,  15),
('300 00001', 'Ospen',                 'box',    'Caps', 20, 10.50, 21.00, 78,  10),
('300 00002', 'Amopen',                'box',    'Caps', 30, 15.00, 30.00, 90,  15),
('400 00001', 'Vitamin C',             'box',    'Caps', 30, 1.20,  2.40,  22,  15),
('400 00002', 'Vitamin B12',           'box',    'Caps', 30, 1.30,  2.60,  43,  15)
    ON CONFLICT (product_id) DO NOTHING;

-- Default reminder templates
INSERT INTO reminder_templates (template_type, subject, body_text) VALUES
('1st Reminder',
 'REMINDER - Outstanding Balance on Your Account',
 'Dear {customer_name},

REMINDER - ACCOUNT NO.: {account_id}
Total Amount Outstanding: £{amount_owed}

According to our records, it appears that we have not yet received payment for goods purchased from {merchant_name}.

We would appreciate payment in full by {payment_due_date}.

If you have already sent a payment to us recently, please accept our apologies.

Yours sincerely,
{merchant_name}'),
('2nd Reminder',
 'SECOND REMINDER - Outstanding Balance on Your Account',
 'Dear {customer_name},

SECOND REMINDER - ACCOUNT NO.: {account_id}
Total Amount Outstanding: £{amount_owed}

It appears that we still have not received payment for goods purchased from {merchant_name}, despite the reminder sent to you on {first_reminder_date}.

We would appreciate it if you would settle this account in full by {payment_due_date}.

If you have already sent a payment to us recently, please accept our apologies.

Yours sincerely,
{merchant_name}')
ON CONFLICT (template_type) DO NOTHING;
