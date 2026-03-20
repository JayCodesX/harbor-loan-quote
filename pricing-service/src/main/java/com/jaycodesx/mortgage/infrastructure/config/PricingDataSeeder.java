package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.pricing.model.PricingAdjustmentRule;
import com.jaycodesx.mortgage.pricing.model.PricingProduct;
import com.jaycodesx.mortgage.pricing.model.RateSheet;
import com.jaycodesx.mortgage.pricing.repository.PricingAdjustmentRuleRepository;
import com.jaycodesx.mortgage.pricing.repository.PricingProductRepository;
import com.jaycodesx.mortgage.pricing.repository.RateSheetRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class PricingDataSeeder implements CommandLineRunner {

    private final PricingProductRepository pricingProductRepository;
    private final RateSheetRepository rateSheetRepository;
    private final PricingAdjustmentRuleRepository pricingAdjustmentRuleRepository;

    public PricingDataSeeder(
            PricingProductRepository pricingProductRepository,
            RateSheetRepository rateSheetRepository,
            PricingAdjustmentRuleRepository pricingAdjustmentRuleRepository
    ) {
        this.pricingProductRepository = pricingProductRepository;
        this.rateSheetRepository = rateSheetRepository;
        this.pricingAdjustmentRuleRepository = pricingAdjustmentRuleRepository;
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
