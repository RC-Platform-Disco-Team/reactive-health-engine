package com.ringcentral.health;

public class ForceHealthCheckFailedException extends RuntimeException {

    ForceHealthCheckFailedException(String message, Throwable e) {
        super(message, e);
    }
}
