package com.reviewassist.review;

public enum Severity {
    CRITICAL(30),
    HIGH(15),
    MEDIUM(8),
    LOW(3),
    INFO(0);

    private final int penalty;

    Severity(int penalty) {
        this.penalty = penalty;
    }

    /** Score deduction used by the offline demo analyzer. */
    public int penalty() {
        return penalty;
    }
}
