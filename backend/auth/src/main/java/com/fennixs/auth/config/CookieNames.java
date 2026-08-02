package com.fennixs.auth.config;

public class CookieNames {
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    @SuppressWarnings("java:S1075")
    public static final String ACCESS_TOKEN_PATH = "/api/";

    @SuppressWarnings("java:S1075")
    public static final String REFRESH_TOKEN_PATH = "/auth/";

    private CookieNames() {
        /* This utility class should not be instantiated */
    }
}
