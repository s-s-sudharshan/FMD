package com.infy.enums;

/**
 * Application roles. Each role maps 1:1 to a Spring Security authority of the
 * form ROLE_<name()> (e.g. ADMIN -> ROLE_ADMIN).
 */
public enum Role {
    ADMIN,
    OPERATOR,
    MANAGER
}
