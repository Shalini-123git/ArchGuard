package com.archguard.rules;

/**
 * A directed layer pair that must not appear as a package dependency.
 */
public final class ForbiddenEdgeRule {

    private String from;
    private String to;
    private String severity = "HIGH";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity == null || severity.isBlank() ? "HIGH" : severity;
    }
}
