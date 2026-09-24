-- Reference DDL + seed data for the Fault Management Dashboard (plan.md Section 2).
-- Enum columns are native MySQL ENUMs because Hibernate 6.2+ expects that for
-- @Enumerated(EnumType.STRING) under ddl-auto=validate. Adding a Java enum value
-- later means ALTER TABLE-ing the matching ENUM here too.
-- Run this once BEFORE starting the backend (spring.jpa.hibernate.ddl-auto=validate).
-- DEV ONLY: the DROP below wipes the database so the script can be re-run cleanly.
-- Remove it before pointing this at a database that holds real data.

DROP DATABASE IF EXISTS fault_management;
CREATE DATABASE fault_management;
USE fault_management;

-- ---------------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            ENUM('ADMIN','OPERATOR','MANAGER') NOT NULL,
    user_state      ENUM('ACTIVATED','DEACTIVATED') NOT NULL DEFAULT 'ACTIVATED',
    secret_question ENUM('FIRST_PET','BIRTH_CITY','FAVOURITE_TEACHER','MOTHERS_MAIDEN_NAME','FAVOURITE_BOOK') NOT NULL,
    secret_answer   VARCHAR(255) NOT NULL
);

-- Passwords are real BCrypt hashes (strength 10). Each user's password is
-- <Firstname>@2026, e.g. ananya.iyer / Ananya@2026. Dev/test credentials only.
-- Secret answers are stored plain and matched case-insensitively.
INSERT INTO users (username, password, role, user_state, secret_question, secret_answer) VALUES
    -- Admins
    ('ananya.iyer',  '$2a$10$x6LGjPxVmk2L/RDOr95i1.29z8vcNEgqkoiAabTVZWKHt6vACyiym', 'ADMIN',    'ACTIVATED',   'FIRST_PET',           'bruno'),
    ('vikram.rao',   '$2a$10$LZ6c8SjtReU/nzSIt0e.FuO8k4LmVD4a5TI7pK24WG25XuOXsPiEG', 'ADMIN',    'ACTIVATED',   'BIRTH_CITY',          'mysuru'),
    -- Operators
    ('rahul.verma',  '$2a$10$jjniLWGF9TY7aQMbPquixufjZBgIh7Slq3t8piOTBpYPN.K9mHEsG', 'OPERATOR', 'ACTIVATED',   'FAVOURITE_TEACHER',   'kulkarni'),
    ('sneha.patil',  '$2a$10$Rh/9N2LY1T2uf/dFwYD3FuXr4g9kwjQWBBxgmdeS//QsjGTCQqCVu', 'OPERATOR', 'ACTIVATED',   'MOTHERS_MAIDEN_NAME', 'deshpande'),
    ('arjun.nair',   '$2a$10$90Aerss08aErJdzkIsQ/uOcptH/BlU29051xgC2dtDgAQgtXUo7cm', 'OPERATOR', 'ACTIVATED',   'FIRST_PET',           'simba'),
    ('kavya.reddy',  '$2a$10$S698EAH8YlNikbUjvN6xj.gU5WZmrm/Q..iO2Jv/W4AWZ6KpplWRa', 'OPERATOR', 'ACTIVATED',   'BIRTH_CITY',          'hyderabad'),
    ('mohan.das',    '$2a$10$qKgXAw1M47/sacbZe0uKa.KcgyxiyJXxHk8gHeLwgOjRnfhx/I9Ii', 'OPERATOR', 'ACTIVATED',   'FAVOURITE_BOOK',      'godan'),
    ('divya.menon',  '$2a$10$a8Ti30e9zzhnl.68kI8e0OjrW.PiLjA27V7K8aJT5/EDdufaYYA5u', 'OPERATOR', 'ACTIVATED',   'FIRST_PET',           'coco'),
    -- Deactivated operator (left the team) - use to test the "User is Deactivated" login path
    ('suresh.kumar', '$2a$10$qsxWvluA4zpQWGIt/KzIW.tdrwFvgtmTYwp9GgihSRRLgf.0uLbcW', 'OPERATOR', 'DEACTIVATED', 'BIRTH_CITY',          'chennai'),
    -- Managers
    ('priya.sharma', '$2a$10$bEechX0WyJam8AFxpbJhY.EDKB.57sHpl6gguAVWDX3hhIYg2RXwi', 'MANAGER',  'ACTIVATED',   'FAVOURITE_TEACHER',   'iyengar'),
    ('karthik.bhat', '$2a$10$A4Id8vbmBP/ZT3EgyJQfUOmL09u3B8jmk3w2TODtvZPtDcrQ7Li/C', 'MANAGER',  'ACTIVATED',   'MOTHERS_MAIDEN_NAME', 'hegde'),
    ('meera.joshi',  '$2a$10$2JN27nB/6kYzYGLSZTqPGu83Weu2Z5BAinAJoZXgI24s3L.ZJVqLW', 'MANAGER',  'ACTIVATED',   'FAVOURITE_BOOK',      'malgudi days'),
    ('rohit.gupta',  '$2a$10$wtDyLuaCA6U1S9folls04uiKqCn.Qqor7kgocGpHpbwzyhHqgJ6Gy', 'MANAGER',  'ACTIVATED',   'FIRST_PET',           'tommy');
-- 13 users -> GET /api/users?page=1 returns the last 3 (10 per page).

-- ---------------------------------------------------------------------------
-- DEVICES
-- ---------------------------------------------------------------------------
CREATE TABLE devices (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    ip_address    VARCHAR(255) NOT NULL UNIQUE,
    device_type   ENUM('HUB','SWITCH','ROUTER') NOT NULL,
    device_state  ENUM('ACTIVATED','DEACTIVATED') NOT NULL DEFAULT 'ACTIVATED'
);

INSERT INTO devices (serial_number, ip_address, device_type, device_state) VALUES
    -- Bengaluru campus (10.10.x.x)
    ('FOC2245X0AB', '10.10.1.1',  'ROUTER', 'ACTIVATED'),
    ('FOC2245X0CD', '10.10.1.2',  'ROUTER', 'ACTIVATED'),
    ('FDO2312A1EF', '10.10.2.11', 'SWITCH', 'ACTIVATED'),
    ('FDO2312A1GH', '10.10.2.12', 'SWITCH', 'ACTIVATED'),
    ('FDO2318B2JK', '10.10.2.13', 'SWITCH', 'ACTIVATED'),
    ('JAE2401C3LM', '10.10.3.21', 'HUB',    'ACTIVATED'),
    ('JAE2401C3NP', '10.10.3.22', 'HUB',    'ACTIVATED'),
    -- Hyderabad campus (10.20.x.x)
    ('FOC2250Y4QR', '10.20.1.1',  'ROUTER', 'ACTIVATED'),
    ('FOC2250Y4CD', '10.20.1.2',  'ROUTER', 'ACTIVATED'),
    ('FDO2320B2WX', '10.20.2.11', 'SWITCH', 'ACTIVATED'),
    ('FDO2320B2YZ', '10.20.2.12', 'SWITCH', 'ACTIVATED'),
    ('JAE2405C5AB', '10.20.3.21', 'HUB',    'ACTIVATED'),
    -- Decommissioned devices (soft-deleted): hidden from the operator list and the alarm view
    ('FOC2119Z9ST', '10.10.9.99', 'ROUTER', 'DEACTIVATED'),
    ('FDO2105D8UV', '10.10.9.98', 'SWITCH', 'DEACTIVATED');
-- 12 active devices -> GET /api/devices?page=1 returns the last 2 (10 per page).

-- ---------------------------------------------------------------------------
-- ALARMS
-- ---------------------------------------------------------------------------
CREATE TABLE alarms (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id     BIGINT        NOT NULL,
    device_ip     VARCHAR(255)  NOT NULL,
    serial_number VARCHAR(255)  NOT NULL,
    device_type   ENUM('HUB','SWITCH','ROUTER') NOT NULL,
    severity      ENUM('CLEAR','WARNING','MAJOR','SEVERE','CRITICAL') NOT NULL,
    trap          ENUM('CBGP_FSM_STATE_CHANGE','CBGP_BACKWARD_TRANSITION','CBGP_PREFIX_THRESHOLD_EXCEEDED','CBGP_PREFIX_THRESHOLD_CLEAR') NOT NULL,
    notes         VARCHAR(1000),
    occurrence    INT           NOT NULL DEFAULT 1,
    status        ENUM('UNACKNOWLEDGED','ACKNOWLEDGED','CLEARED','TERMINATED') NOT NULL DEFAULT 'UNACKNOWLEDGED',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    -- Audit trail (Extra 3): who/when for each transition. Nullable so pre-existing rows need no backfill.
    acknowledged_by VARCHAR(255) NULL,
    acknowledged_at DATETIME(6)  NULL,
    cleared_by      VARCHAR(255) NULL,
    cleared_at      DATETIME(6)  NULL,
    terminated_by   VARCHAR(255) NULL,
    terminated_at   DATETIME(6)  NULL,
    CONSTRAINT fk_alarm_device FOREIGN KEY (device_id) REFERENCES devices(id)
);

-- For an existing database created before Extra 3, run this once instead of re-running this script:
-- ALTER TABLE alarms
--   ADD COLUMN acknowledged_by VARCHAR(255) NULL, ADD COLUMN acknowledged_at DATETIME(6) NULL,
--   ADD COLUMN cleared_by      VARCHAR(255) NULL, ADD COLUMN cleared_at      DATETIME(6) NULL,
--   ADD COLUMN terminated_by   VARCHAR(255) NULL, ADD COLUMN terminated_at   DATETIME(6) NULL;

-- Denormalised device columns are copied from the device row via the join.
-- Covers every severity and status, plus a TERMINATED alarm and one on a deactivated device.
-- Seeded rows leave the audit columns NULL (they were not produced by a logged-in manager).
INSERT INTO alarms (device_id, device_ip, serial_number, device_type, severity, trap, notes, occurrence, status, created_at, updated_at)
SELECT d.id, d.ip_address, d.serial_number, d.device_type, s.severity, s.trap, s.notes, s.occ, s.status, NOW(6), NOW(6)
FROM devices d
JOIN (
    SELECT 'FOC2245X0AB' sn, 'CRITICAL' severity, 'CBGP_BACKWARD_TRANSITION' trap, 'BGP peer 10.10.1.254 dropped; checking upstream link' notes, 3 occ, 'UNACKNOWLEDGED' status
    UNION ALL SELECT 'FOC2245X0AB', 'MAJOR',    'CBGP_FSM_STATE_CHANGE',          NULL,                                  1, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'FOC2245X0CD', 'WARNING',  'CBGP_PREFIX_THRESHOLD_EXCEEDED', 'Prefix count at 85% of max-prefix',   2, 'ACKNOWLEDGED'
    UNION ALL SELECT 'FDO2312A1EF', 'SEVERE',   'CBGP_BACKWARD_TRANSITION',       NULL,                                  1, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'FDO2312A1GH', 'CLEAR',    'CBGP_PREFIX_THRESHOLD_CLEAR',    'Prefix count back to normal',         1, 'ACKNOWLEDGED'
    UNION ALL SELECT 'FDO2318B2JK', 'MAJOR',    'CBGP_FSM_STATE_CHANGE',          'Session flapped once, now stable',    1, 'CLEARED'
    UNION ALL SELECT 'JAE2401C3LM', 'WARNING',  'CBGP_PREFIX_THRESHOLD_EXCEEDED', NULL,                                  4, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'JAE2401C3NP', 'MAJOR',    'CBGP_FSM_STATE_CHANGE',          'Resolved after firmware upgrade',     1, 'TERMINATED'
    UNION ALL SELECT 'FOC2250Y4QR', 'CRITICAL', 'CBGP_BACKWARD_TRANSITION',       NULL,                                  2, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'FOC2250Y4CD', 'SEVERE',   'CBGP_PREFIX_THRESHOLD_EXCEEDED', 'Escalated to network team',           1, 'ACKNOWLEDGED'
    UNION ALL SELECT 'FDO2320B2WX', 'WARNING',  'CBGP_FSM_STATE_CHANGE',          NULL,                                  1, 'CLEARED'
    UNION ALL SELECT 'FDO2320B2YZ', 'MAJOR',    'CBGP_BACKWARD_TRANSITION',       NULL,                                  1, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'JAE2405C5AB', 'CLEAR',    'CBGP_PREFIX_THRESHOLD_CLEAR',    NULL,                                  1, 'UNACKNOWLEDGED'
    -- Belongs to a deactivated device: must NOT appear in GET /api/alarms
    UNION ALL SELECT 'FOC2119Z9ST', 'CRITICAL', 'CBGP_BACKWARD_TRANSITION',       'device decommissioned - hidden',      1, 'UNACKNOWLEDGED'
) s ON s.sn = d.serial_number;