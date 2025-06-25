package com.musimizer.exception;

public class MusicDirectoryException extends RuntimeException {
    public MusicDirectoryException(String message) {
        super(message);
    }

    public MusicDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
