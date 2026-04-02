package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.quote.dto.PublicLoanQuoteRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.repository.LoanQuoteRepository;
import com.jaycodesx.mortgage.quote.service.LoanQuoteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedData(
            LoanQuoteRepository loanQuoteRepository,
            LoanQuoteService loanQuoteService
    ) {
        return args -> seedQuotes(loanQuoteRepository, loanQuoteService);
    }

    private void seedQuotes(LoanQuoteRepository loanQuoteRepository, LoanQuoteService loanQuoteService) {
        if (loanQuoteRepository.count() > 0) {
            return;
        }

        Long quoteOneId = loanQuoteService.createPublicQuote(new PublicLoanQuoteRequestDto(
                randomCurrency(320000, 580000),
                randomCurrency(25000, 120000),
                "60614",
                "CONVENTIONAL",
                "PRIMARY_RESIDENCE",
                30
        )).id();

        loanQuoteService.refineQuote(quoteOneId, new QuoteRefinementRequestDto(
                "Jay",
                "Harper",
                "jay.harper@jaycodesx.dev",
                "555-111-0101",
                randomCurrency(98000, 150000),
                randomCurrency(600, 1800),
                randomInt(690, 790),
                randomCurrency(18000, 42000),
                true,
                false,
                true,
                true,
                true,
                "I agree to be contacted by Harbor Mortgage and its lending partners."
        ));

        loanQuoteService.createPublicQuote(new PublicLoanQuoteRequestDto(
                randomCurrency(250000, 430000),
                randomCurrency(12000, 60000),
                "30309",
                "FHA",
                "PRIMARY_RESIDENCE",
                30
        ));

        Long quoteThreeId = loanQuoteService.createPublicQuote(new PublicLoanQuoteRequestDto(
                randomCurrency(450000, 760000),
                randomCurrency(40000, 180000),
                "75205",
                "JUMBO",
                "SECOND_HOME",
                30
        )).id();

        loanQuoteService.refineQuote(quoteThreeId, new QuoteRefinementRequestDto(
                "Jay",
                "Monroe",
                "jay.monroe@jaycodesx.dev",
                "555-111-0103",
                randomCurrency(160000, 245000),
                randomCurrency(1200, 2800),
                randomInt(720, 820),
                randomCurrency(40000, 95000),
                false,
                false,
                true,
                null,
                true,
                "I agree to be contacted by Harbor Mortgage and its lending partners."
        ));
    }

    private int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private BigDecimal randomCurrency(int minInclusive, int maxInclusive) {
        int amount = randomInt(minInclusive, maxInclusive);
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal randomRate(double minInclusive, double maxInclusive) {
        double raw = ThreadLocalRandom.current().nextDouble(minInclusive, maxInclusive);
        return BigDecimal.valueOf(raw).setScale(4, RoundingMode.HALF_UP);
    }
}
