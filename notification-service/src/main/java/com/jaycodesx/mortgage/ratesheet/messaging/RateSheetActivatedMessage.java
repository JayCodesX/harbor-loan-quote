package com.jaycodesx.mortgage.ratesheet.messaging;

import java.time.LocalDateTime;

/**
 * Event payload consumed from the {@code rate-sheet.activated} queue per ADR-0049.
 *
 * <p>This record is notification-service's local representation of the event published by
 * pricing-service. Services are independent — each service owns its own representation of
 * the event schema. Cross-service deserialization is handled by Jackson with
 * {@code TypePrecedence.INFERRED} so the pricing-service {@code __TypeId__} header is ignored.
 *
 * <p>Schema version 1. Matches the {@code RateSheetActivatedMessage} published by pricing-service.
 */
public record RateSheetActivatedMessage(
        Long rateSheetId,
        String investorId,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt
) {
}
