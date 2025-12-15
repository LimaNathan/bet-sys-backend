package com.coticbet.domain.enums;

/**
 * Status of an individual leg in a multiple bet
 */
public enum LegStatus {
    PENDING, // Awaiting event settlement
    WON, // Leg won
    LOST, // Leg lost
    VOID // Event canceled - odd becomes 1.00
}
