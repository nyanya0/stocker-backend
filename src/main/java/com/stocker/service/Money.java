package com.stocker.service;

final class Money {
    private Money() {}

    /** Rounds to 2 decimal places (cents). */
    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
