package com.truecommunity.model;

/**
 * Verdict choices used by community votes and final editor decisions, with
 * localized labels and display colors for the UI.
 */
public enum Verdict {
    TRUE("נכון", "#198754"),
    FALSE("לא נכון", "#dc3545"),
    MISLEADING("מטעה", "#fd7e14"),
    MISSING_CONTEXT("חסר הקשר", "#ffc107"),
    SATIRE("סאטירה", "#6f42c1");

    private final String label;
    private final String color;

    Verdict(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}
