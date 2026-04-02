package com.jaycodesx.mortgage.pricing.dto;

import java.math.BigDecimal;

/**
 * Input record for a single rate/price pair when publishing a new rate sheet.
 *
 * @param productTermId natural key for the product and term combination,
 *                      e.g. "CONVENTIONAL_30", "FHA_30", "VA_15"
 * @param rate          the note rate at this point on the ladder, e.g. 6.7500
 * @param price         the price in points — negative for lender credits, positive for discount points
 */
public record RateSheetEntryRequest(
        String productTermId,
        BigDecimal rate,
        BigDecimal price
) {
}
