package com.infy.enums;

/**
 * Alarm lifecycle: UNACKNOWLEDGED -> ACKNOWLEDGED -> CLEARED -> TERMINATED
 * (TERMINATED may also be reached directly from ACKNOWLEDGED).
 * A recurrence of a non-terminated alarm (simulator/XML ingestion) re-opens it
 * back to UNACKNOWLEDGED; a recurrence after TERMINATED creates a new alarm.
 */
public enum AlarmStatus {
    UNACKNOWLEDGED,
    ACKNOWLEDGED,
    CLEARED,
    TERMINATED
}
