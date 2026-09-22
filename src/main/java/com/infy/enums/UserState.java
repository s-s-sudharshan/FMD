package com.infy.enums;

/**
 * Lifecycle state of a User account. Users are never hard-deleted; deleting a
 * user (US06/BE US05) flips this to DEACTIVATED instead.
 */
public enum UserState {
    ACTIVATED,
    DEACTIVATED
}
