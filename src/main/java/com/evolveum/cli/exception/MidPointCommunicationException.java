package com.evolveum.cli.exception;

public class MidPointCommunicationException extends Exception {
    public MidPointCommunicationException(String message) {
        super(message);
    }
    public MidPointCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
