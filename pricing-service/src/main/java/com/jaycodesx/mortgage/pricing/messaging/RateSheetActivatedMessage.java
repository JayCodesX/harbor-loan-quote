package com.jaycodesx.mortgage.pricing.messaging;

import java.time.LocalDateTime;

/**
 * Event payload published when a new rate sheet is activated per ADR-0048.
 *
 * <p>Published by pricing-service via {@link RateSheetActivatedPublisher} whenever
 * a rate sheet transitions to ACTIVE status. Downstream consumers (notification-service)
 * use this event to notify borrowers whose quotes are no longer priced against the
 * active rate sheet.
 *
 * <p>This record is the canonical event schema for routing key {@code RATE_SHEET_ACTIVATED}
 * on the {@code rate-sheet.events} exchange. Schema version 1.
 */
public record RateSheetActivatedMessage(
        Long rateSheetId,
        String investorId,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt
) {
}
