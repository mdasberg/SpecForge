package com.specforge.repository.exception;

/** The forge could not be reached, or answered something SpecForge cannot use. */
public class ForgeException extends RuntimeException {

    public ForgeException(final String message) {
        super(message);
    }

    public ForgeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
