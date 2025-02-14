package com.suyeon.suyeon.exception;

import lombok.Getter;

@Getter
public class TokenUpgradeRequiredException extends RuntimeException {
    private final String accessToken;

    public TokenUpgradeRequiredException(String message, String accessToken) {
        super(message);
        this.accessToken = accessToken;
    }
}
