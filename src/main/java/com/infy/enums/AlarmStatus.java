package com.infy.enums;

/**
 * Alarm lifecycle: UNACKNOWLEDGED -> ACKNOWLEDGED -> CLEARED -> TERMINATED
 * (TERMINATED may also be reached directly from ACKNOWLEDGED).
 */
public enum AlarmStatus {
    UNACKNOWLEDGED,
    ACKNOWLEDGED,
    CLEARED,
    TERMINATED
}
