package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.pricing.model.LlpaAdjustment;
import com.jaycodesx.mortgage.pricing.model.PricingAdjustmentRule;
import com.jaycodesx.mortgage.pricing.model.PricingProduct;
import com.jaycodesx.mortgage.pricing.model.RateSheet;
import com.jaycodesx.mortgage.pricing.repository.LlpaAdjustmentRepository;
import com.jaycodesx.mortgage.pricing.repository.PricingAdjustmentRuleRepository;
import com.jaycodesx.mortgage.pricing.repository.PricingProductRepository;
import com.jaycodesx.mortgage.pricing.repository.RateSheetRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class PricingDataSeeder implements CommandLineRunner {

    private final PricingProductRepository pricingProductRepository;
    private final RateSheetRepository rateSheetRepository;
    private final PricingAdjustmentRuleRepository pricingAdjustmentRuleRepository;
    private final LlpaAdjustmentRepository llpaAdjustmentRepository;

    public PricingDataSeeder(
            PricingProductRepository pricingProductRepository,
            RateSheetRepository rateSheetRepository,
            PricingAdjustmentRuleRepository pricingAdjustmentRuleRepository,
            LlpaAdjustmentRepository llpaAdjustmentRepository
    ) {
        this.pricingProductRepository = pricingProductRepository;
        this.rateSheetRepository = rateSheetRepository;
        this.pricingAdjustmentRuleRepository = pricingAdjustmentRuleRepository;
        this.llpaAdjustmentRepository = llpaAdjustmentRepository;
    }

    @Override
    public void run(String... args) {
        if (pricingProductRepository.count() == 0) {
            pricingProductRepository.saveAll(List.of(
                    product("CONVENTIONAL", "Conventional 30 Fixed", "6.2500"),
                    product("FHA", "FHA 30 Fixed", "6.0500"),
                    product("VA", "VA 30 Fixed", "5.9500"),
                    product("JUMBO", "Jumbo 30 Fixed", "6.5500")
            ));
        }
        if (rateSheetRepository.count() == 0) {
            rateSheetRepository.saveAll(List.of(
                    rateSheet("CONVENTIONAL", "PRIMARY_RESIDENCE", "606", "0.0200"),
                    rateSheet("CONVENTIONAL", "PRIMARY_RESIDENCE", "941", "0.0800"),
                    rateSheet("FHA", "PRIMARY_RESIDENCE", "606", "0.0100"),
                    rateSheet("VA", "PRIMARY_RESIDENCE", "606", "-0.0200"),
                    rateSheet("JUMBO", "PRIMARY_RESIDENCE", "941", "0.1200")
            ));
        }
        if (llpaAdjustmentRepository.count() == 0) {
            seedFannieMaeLlpaAdjustments();
        }
        if (pricingAdjustmentRuleRepository.count() == 0) {
            pricingAdjustmentRuleRepository.saveAll(List.of(
                    rule("TERM", "15", "-0.5500"),
                    rule("TERM", "20", "-0.2000"),
                    rule("DOWN_PAYMENT", "GE_20", "-0.2000"),
                    rule("DOWN_PAYMENT", "GE_10", "0.0000"),
                    rule("DOWN_PAYMENT", "GE_5", "0.1800"),
                    rule("DOWN_PAYMENT", "LT_5", "0.3200"),
                    rule("CREDIT_SCORE", "GE_760", "-0.3500"),
                    rule("CREDIT_SCORE", "GE_720", "-0.1500"),
                    rule("CREDIT_SCORE", "GE_680", "0.0500"),
                    rule("CREDIT_SCORE", "GE_640", "0.2200"),
                    rule("CREDIT_SCORE", "LT_640", "0.4500"),
                    rule("DTI", "GT_43", "0.2500"),
                    rule("DTI", "GT_36", "0.1200"),
                    rule("DTI", "LT_28", "-0.1000"),
                    rule("FLAG", "FIRST_TIME_BUYER", "-0.0500"),
                    rule("FLAG", "VA_ELIGIBLE", "-0.1000")
            ));
        }
    }

    /**
     * Seeds Fannie Mae LLPA adjustments based on publicly available single-family pricing matrices.
     * Only non-zero adjustments are seeded — zero-adjustment rows have no effect on pricing.
     * Effective date is set to epoch start to represent "always effective from the beginning".
     */
    private void seedFannieMaeLlpaAdjustments() {
        LocalDateTime epoch = LocalDateTime.of(2024, 1, 1, 0, 0);

        llpaAdjustmentRepository.saveAll(List.of(
                // --- Credit Score (applied to all conventional products) ---
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 740, \"max\": 759}", "0.2500", epoch),
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 720, \"max\": 739}", "0.5000", epoch),
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 700, \"max\": 719}", "0.7500", epoch),
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 680, \"max\": 699}", "1.0000", epoch),
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 660, \"max\": 679}", "1.2500", epoch),
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 640, \"max\": 659}", "1.5000", epoch),
                llpa("FANNIE_MAE", null, "CREDIT_SCORE", "{\"max\": 639}",               "2.0000", epoch),

                // --- LTV ---
                llpa("FANNIE_MAE", null, "LTV", "{\"max\": 60}",              "-0.2500", epoch),
                llpa("FANNIE_MAE", null, "LTV", "{\"min\": 70.01, \"max\": 75}", "0.2500", epoch),
                llpa("FANNIE_MAE", null, "LTV", "{\"min\": 75.01, \"max\": 80}", "0.5000", epoch),
                llpa("FANNIE_MAE", null, "LTV", "{\"min\": 80.01, \"max\": 85}", "0.7500", epoch),
                llpa("FANNIE_MAE", null, "LTV", "{\"min\": 85.01, \"max\": 90}", "1.0000", epoch),
                llpa("FANNIE_MAE", null, "LTV", "{\"min\": 90.01, \"max\": 95}", "1.2500", epoch),
                llpa("FANNIE_MAE", null, "LTV", "{\"min\": 95.01}",              "1.5000", epoch),

                // --- Occupancy ---
                llpa("FANNIE_MAE", null, "OCCUPANCY", "{\"equals\": \"SECOND_HOME\"}", "1.0000", epoch),
                llpa("FANNIE_MAE", null, "OCCUPANCY", "{\"equals\": \"INVESTMENT\"}",  "2.0000", epoch),

                // --- Loan Purpose ---
                llpa("FANNIE_MAE", null, "LOAN_PURPOSE", "{\"equals\": \"RATE_TERM_REFI\"}", "0.2500", epoch),
                llpa("FANNIE_MAE", null, "LOAN_PURPOSE", "{\"equals\": \"CASH_OUT_REFI\"}",  "0.3750", epoch),

                // --- Property Type ---
                llpa("FANNIE_MAE", null, "PROPERTY_TYPE", "{\"equals\": \"CONDO\"}",       "0.3750", epoch),
                llpa("FANNIE_MAE", null, "PROPERTY_TYPE", "{\"equals\": \"MANUFACTURED\"}", "0.5000", epoch),
                llpa("FANNIE_MAE", null, "PROPERTY_TYPE", "{\"equals\": \"MULTI_UNIT\"}",   "0.7500", epoch)
        ));
    }

    private LlpaAdjustment llpa(String investorId, String productType, String category,
                                 String conditionJson, String priceAdjustment, LocalDateTime effectiveAt) {
        LlpaAdjustment adj = new LlpaAdjustment();
        adj.setInvestorId(investorId);
        adj.setProductType(productType);
        adj.setAdjustmentCategory(category);
        adj.setConditionJson(conditionJson);
        adj.setPriceAdjustment(new BigDecimal(priceAdjustment));
        adj.setEffectiveAt(effectiveAt);
        adj.setActive(true);
        return adj;
    }

    private PricingProduct product(String code, String name, String rate) {
        PricingProduct product = new PricingProduct();
        product.setProgramCode(code);
        product.setProductName(name);
        product.setBaseRate(new BigDecimal(rate));
        product.setActive(true);
        return product;
    }

    private RateSheet rateSheet(String code, String propertyUse, String zipPrefix, String adjustment) {
        RateSheet sheet = new RateSheet();
        sheet.setProgramCode(code);
        sheet.setPropertyUse(propertyUse);
        sheet.setZipPrefix(zipPrefix);
        sheet.setZipAdjustment(new BigDecimal(adjustment));
        sheet.setActive(true);
        return sheet;
    }

    private PricingAdjustmentRule rule(String type, String key, String adjustment) {
        PricingAdjustmentRule rule = new PricingAdjustmentRule();
        rule.setRuleType(type);
        rule.setRuleKey(key);
        rule.setAdjustment(new BigDecimal(adjustment));
        rule.setActive(true);
        return rule;
    }
}
