package com.pavlent1yy.gcore.customExceptions;

public class PasswordIsIncorrect extends RuntimeException {
    public PasswordIsIncorrect(String message) {
        super(message);
    }
}
