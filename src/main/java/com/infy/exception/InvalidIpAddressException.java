package com.infy.exception;

/** Thrown for a malformed IPv4 address, or 0.0.0.0 / 255.255.255.255. Maps to 400. */
public class InvalidIpAddressException extends RuntimeException {
    public InvalidIpAddressException(String message) {
        super(message);
    }
}
