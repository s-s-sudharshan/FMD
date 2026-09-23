package com.infy.exception;

/** Thrown when no (active) Device matches the supplied serial number. Maps to 404. */
public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String message) {
        super(message);
    }
}
