package com.truecommunity.util;

import java.net.URI;

/**
 * Shared server-side URL validation. Only absolute {@code http}/{@code https}
 * URLs with a host are accepted; schemes such as {@code javascript:} or
 * {@code data:} are rejected so user/editor-supplied links can be safely
 * rendered into {@code href}/{@code src} attributes without stored-XSS risk.
 */
public final class UrlValidator {

    private UrlValidator() {
    }

    /**
     * Reports whether the value is a valid absolute http/https URL with a host.
     *
     * @param value the candidate URL, may be {@code null} or blank
     * @return {@code true} only for a well-formed http/https URL
     */
    public static boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Normalizes an optional URL for storage: blank values become {@code null},
     * a valid http/https URL is trimmed and kept, and anything else is rejected.
     *
     * @param value the raw optional URL
     * @return the trimmed URL, or {@code null} when blank
     * @throws IllegalArgumentException when a non-blank value is not http/https
     */
    public static String sanitizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!isValidHttpUrl(trimmed)) {
            throw new IllegalArgumentException("Only http or https URLs are allowed.");
        }
        return trimmed;
    }
}
