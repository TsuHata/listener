package com.example.exception;

public class ListenerException extends RuntimeException {
    public ListenerException(String message) {
        super(message);
    }

    public ListenerException(String message, Throwable cause) {
        super(message, cause);
    }
}
