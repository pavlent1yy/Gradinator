package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.PasswordIsIncorrect;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new PasswordIsIncorrect("Пароль не может быть пустым");
        }
        if (password.length() < MIN_LENGTH) {
            throw new PasswordIsIncorrect("Пароль должен быть не короче " + MIN_LENGTH + " символов");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new PasswordIsIncorrect("Пароль слишком длинный");
        }
    }
}
