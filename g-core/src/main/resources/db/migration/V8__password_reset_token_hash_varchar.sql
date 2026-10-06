-- Match PasswordResetToken.tokenHash while preserving already issued hashes.
ALTER TABLE password_reset_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);
