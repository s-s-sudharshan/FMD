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
    role            VARCHAR(50)  NOT NULL,
    user_state      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVATED',
    secret_question VARCHAR(50)  NOT NULL,
    secret_answer   VARCHAR(255) NOT NULL
);

-- Seed users for Postman testing (Phase 1 has no Add User endpoint yet).
-- Password for every row below is: Passw0rd
-- Hash is a real BCrypt digest (strength 10, $2a$ prefix) matching Spring's
-- default BCryptPasswordEncoder(), so passwordEncoder.matches("Passw0rd", ...) succeeds.
-- Secret answer for every row below is: fluffy   (case-insensitive match in AuthServiceImpl)

INSERT INTO users (username, password, role, user_state, secret_question, secret_answer) VALUES
    ('admin1',   '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'ADMIN',    'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('operator1','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('manager1', '$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'MANAGER',  'ACTIVATED',   'FIRST_PET', 'fluffy'),
    ('inactive1','$2a$10$Si.kcZWumAw0qsqRHFZX7.mw6Fo.fKM3DmapnYRKTHO4gWbIfKd1O', 'OPERATOR', 'DEACTIVATED', 'FIRST_PET', 'fluffy');
