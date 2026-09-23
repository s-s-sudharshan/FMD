package com.infy.exception;

/** Thrown when a device serial number or IP address is already in use. Maps to 409. */
public class DuplicateDeviceException extends RuntimeException {
    public DuplicateDeviceException(String message) {
        super(message);
    }
}
