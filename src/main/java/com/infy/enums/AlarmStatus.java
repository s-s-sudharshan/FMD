package com.infy.enums;

/**
 * Alarm lifecycle: UNACKNOWLEDGED -> ACKNOWLEDGED -> CLEARED -> TERMINATED
 * (TERMINATED may also be reached directly from ACKNOWLEDGED).
 * A repeat of an active (UNACKNOWLEDGED/ACKNOWLEDGED) alarm only bumps `occurrence`
 * and leaves status and audit values alone; a repeat after CLEARED or TERMINATED
 * creates a new UNACKNOWLEDGED alarm and leaves the completed one unchanged.
 */
public enum AlarmStatus {
    UNACKNOWLEDGED,
    ACKNOWLEDGED,
    CLEARED,
    TERMINATED
}
