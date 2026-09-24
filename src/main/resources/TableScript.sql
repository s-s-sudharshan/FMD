-- Reference DDL for Phase 1 (plan.md Section 2.1 / Project Structure lists this file).
-- Not required to run manually: spring.jpa.hibernate.ddl-auto=update will create/update
-- this table automatically on application startup. Kept here (a) as the durable schema
-- reference plan.md calls for, and (b) for the one-off seed insert below, since there is
-- no "Add User" endpoint yet -- that ships in Phase 2.

CREATE DATABASE IF NOT EXISTS fault_management;
USE fault_management;

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            ENUM('ADMIN','OPERATOR','MANAGER') NOT NULL,
    user_state      ENUM('ACTIVATED','DEACTIVATED') NOT NULL DEFAULT 'ACTIVATED',
    secret_question ENUM('FIRST_PET','BIRTH_CITY','FAVOURITE_TEACHER','MOTHERS_MAIDEN_NAME','FAVOURITE_BOOK') NOT NULL,
    secret_answer   VARCHAR(255) NOT NULL
);

-- Seed users for Postman testing.
-- Password for every row below is: Passw0rd
-- Hash is a real BCrypt digest (strength 10, $2a$ prefix) matching Spring's
-- default BCryptPasswordEncoder(), so passwordEncoder.matches("Passw0rd", ...) succeeds.
-- Secret answer for every row below is: fluffy   (case-insensitive match in AuthServiceImpl)

INSERT INTO users (username, password, role, user_state, secret_question, secret_answer) VALUES
    ('admin1',   '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'ADMIN',    'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('operator1','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('manager1', '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'MANAGER',  'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('inactive1','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'DEACTIVATED', 'FIRST_PET', 'fluffy'),
    -- Extra seed rows added for Phase 2, purely so GET /api/users?page=0 vs page=1
    -- has something to show (10/page). No functional difference from the four above.
    ('operator2','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'BIRTH_CITY', 'fluffy'),
    ('operator3','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'BIRTH_CITY', 'fluffy'),
    ('manager2', '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'MANAGER',  'ACTIVATED',   'FAVOURITE_TEACHER', 'fluffy'),
    ('admin2',   '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'ADMIN',    'ACTIVATED',   'FAVOURITE_BOOK', 'fluffy'),
    ('operator4','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'MOTHERS_MAIDEN_NAME', 'fluffy'),
    ('operator5','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'MOTHERS_MAIDEN_NAME', 'fluffy'),
    ('manager3', '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'MANAGER',  'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('operator6','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'BIRTH_CITY', 'fluffy');
    
    
CREATE TABLE IF NOT EXISTS devices (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    ip_address    VARCHAR(255) NOT NULL UNIQUE,
    device_type   ENUM('HUB','SWITCH','ROUTER') NOT NULL,
    device_state  ENUM('ACTIVATED','DEACTIVATED') NOT NULL DEFAULT 'ACTIVATED'
);

INSERT INTO devices (serial_number, ip_address, device_type, device_state) VALUES
    ('SN-1001', '192.168.1.10', 'ROUTER', 'ACTIVATED'),
    ('SN-1002', '192.168.1.11', 'SWITCH', 'ACTIVATED'),
    ('SN-1003', '192.168.1.12', 'HUB',    'ACTIVATED'),
    ('SN-1004', '192.168.1.13', 'ROUTER', 'DEACTIVATED');
    
CREATE TABLE IF NOT EXISTS alarms (
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
    CONSTRAINT fk_alarm_device FOREIGN KEY (device_id) REFERENCES devices(id)
);
 
INSERT INTO alarms (device_id, device_ip, serial_number, device_type, severity, trap, notes, occurrence, status, created_at, updated_at)
SELECT id, ip_address, serial_number, device_type, s.severity, s.trap, s.notes, s.occ, s.status, NOW(6), NOW(6)
FROM devices d
JOIN (
    SELECT 'SN-1001' sn, 'CRITICAL' severity, 'CBGP_BACKWARD_TRANSITION' trap, 'BGP peer dropped' notes, 3 occ, 'UNACKNOWLEDGED' status
    UNION ALL SELECT 'SN-1001', 'MAJOR',   'CBGP_FSM_STATE_CHANGE',          NULL, 1, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'SN-1001', 'WARNING', 'CBGP_PREFIX_THRESHOLD_EXCEEDED', NULL, 2, 'ACKNOWLEDGED'
    UNION ALL SELECT 'SN-1002', 'SEVERE',  'CBGP_BACKWARD_TRANSITION',       NULL, 1, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'SN-1002', 'CLEAR',   'CBGP_PREFIX_THRESHOLD_CLEAR',    NULL, 1, 'ACKNOWLEDGED'
    UNION ALL SELECT 'SN-1002', 'MAJOR',   'CBGP_FSM_STATE_CHANGE',          NULL, 1, 'CLEARED'
    UNION ALL SELECT 'SN-1003', 'WARNING', 'CBGP_PREFIX_THRESHOLD_EXCEEDED', NULL, 4, 'UNACKNOWLEDGED'
    UNION ALL SELECT 'SN-1003', 'MAJOR',   'CBGP_FSM_STATE_CHANGE',          NULL, 1, 'TERMINATED'
    UNION ALL SELECT 'SN-1004', 'CRITICAL','CBGP_BACKWARD_TRANSITION',       'device deactivated - hidden', 1, 'UNACKNOWLEDGED'
) s ON s.sn = d.serial_number;