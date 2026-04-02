package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.service.PricingScenario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void returnsCachedPublicDecisionWhenPresent() throws Exception {
        PricingScenario quote = buildQuote();
        QuotePricingService.QuoteDecision decision = new QuotePricingService.QuoteDecision(
                new BigDecimal("6.0200"),
                new BigDecimal("6.2000"),
                new BigDecimal("420000.00"),
                new BigDecimal("2523.52"),
                new BigDecimal("116025.00"),
                "Market Estimate"
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(new ObjectMapper().writeValueAsString(decision));

        PricingCacheService service = new PricingCacheService(redisTemplate, new ObjectMapper());
        Optional<QuotePricingService.QuoteDecision> result = service.getPublicDecision(quote);

        assertThat(result).contains(decision);
    }

    @Test
    void cachesRefinedDecisionWithTtl() {
        PricingScenario quote = buildQuote();
        QuoteRefinementRequestDto request = new QuoteRefinementRequestDto(
                "Jay", "Stone", "jay@jaycodesx.dev", "555-111-0101",
                new BigDecimal("130000.00"), new BigDecimal("900.00"), 740,
                new BigDecimal("25000.00"), true, false
        );
        QuotePricingService.QuoteDecision decision = new QuotePricingService.QuoteDecision(
                new BigDecimal("5.9500"),
                new BigDecimal("6.1300"),
                new BigDecimal("420000.00"),
                new BigDecimal("2499.16"),
                new BigDecimal("116025.00"),
                "Prime"
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PricingCacheService service = new PricingCacheService(redisTemplate, new ObjectMapper());
        service.cacheRefinedDecision(quote, request, decision);

        verify(valueOperations).set(any(), eq("{\"estimatedRate\":5.9500,\"estimatedApr\":6.1300,\"financedAmount\":420000.00,\"estimatedMonthlyPayment\":2499.16,\"estimatedCashToClose\":116025.00,\"qualificationTier\":\"Prime\"}"), any());
    }

    @Test
    void evictAll_deletesAllPricingKeys() {
        Set<String> keys = Set.of("pricing:public:abc123", "pricing:refined:def456");
        when(redisTemplate.keys("pricing:*")).thenReturn(keys);
        when(redisTemplate.delete(anyCollection())).thenReturn(2L);

        PricingCacheService service = new PricingCacheService(redisTemplate, new ObjectMapper());
        long evicted = service.evictAll();

        assertThat(evicted).isEqualTo(2L);
        verify(redisTemplate).delete(keys);
    }

    @Test
    void evictAll_returnsZero_whenNoCachedKeys() {
        when(redisTemplate.keys("pricing:*")).thenReturn(Set.of());

        PricingCacheService service = new PricingCacheService(redisTemplate, new ObjectMapper());
        long evicted = service.evictAll();

        assertThat(evicted).isZero();
    }

    private PricingScenario buildQuote() {
        return new PricingScenario(
                5L,
                new BigDecimal("525000.00"),
                new BigDecimal("105000.00"),
                "98101",
                "CONVENTIONAL",
                "PRIMARY_RESIDENCE",
                30
        );
    }
}
