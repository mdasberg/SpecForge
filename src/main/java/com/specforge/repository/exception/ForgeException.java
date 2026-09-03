package com.specforge.repository.exception;

/** The forge could not be reached, or answered something SpecForge cannot use. */
public class ForgeException extends RuntimeException {

    public ForgeException(String message) {
        super(message);
    }

    public ForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
