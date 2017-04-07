package com.ringcentral.platform.health;

public class ForceHealthCheckFailedException extends RuntimeException {

    ForceHealthCheckFailedException(String message, Throwable e) {
        super(message, e);
    }
}
