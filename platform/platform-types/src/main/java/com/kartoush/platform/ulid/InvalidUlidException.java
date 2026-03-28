package com.kartoush.platform.ulid;

public class InvalidUlidException extends RuntimeException {
    public InvalidUlidException(String message) { super(message); }
    public InvalidUlidException(String message, Throwable cause) { super(message, cause); }

}
