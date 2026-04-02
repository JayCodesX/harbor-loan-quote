package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.service.PricingScenario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

@Service
public class PricingCacheService {

    private static final Logger log = LoggerFactory.getLogger(PricingCacheService.class);
    private static final String CACHE_KEY_PREFIX = "pricing:*";
    private static final Duration PUBLIC_TTL = Duration.ofMinutes(10);
    private static final Duration REFINED_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PricingCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<QuotePricingService.QuoteDecision> getPublicDecision(PricingScenario scenario) {
        return readDecision(publicKey(scenario));
    }

    public Optional<QuotePricingService.QuoteDecision> getRefinedDecision(PricingScenario scenario, QuoteRefinementRequestDto request) {
        return readDecision(refinedKey(scenario, request));
    }

    public void cachePublicDecision(PricingScenario scenario, QuotePricingService.QuoteDecision decision) {
        writeDecision(publicKey(scenario), decision, PUBLIC_TTL);
    }

    public void cacheRefinedDecision(PricingScenario scenario, QuoteRefinementRequestDto request, QuotePricingService.QuoteDecision decision) {
        writeDecision(refinedKey(scenario, request), decision, REFINED_TTL);
    }

    /**
     * Evicts all cached pricing decisions when a new rate sheet is activated per ADR-0048.
     * Deletes every key matching {@code pricing:*} so the next request re-prices against
     * the new rate sheet instead of returning a stale cached result.
     */
    public long evictAll() {
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX);
        if (keys == null || keys.isEmpty()) {
            log.debug("PricingCacheService.evictAll: no keys to evict");
            return 0L;
        }
        Long deleted = redisTemplate.delete(keys);
        long count = deleted != null ? deleted : 0L;
        log.info("PricingCacheService.evictAll: evicted {} pricing cache entries", count);
        return count;
    }

    private Optional<QuotePricingService.QuoteDecision> readDecision(String key) {
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, QuotePricingService.QuoteDecision.class));
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    private void writeDecision(String key, QuotePricingService.QuoteDecision decision, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(decision), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize cached pricing decision", ex);
        }
    }

    private String publicKey(PricingScenario quote) {
        return "pricing:public:" + hash(String.join("|",
                normalize(quote.loanProgram()),
                normalize(quote.propertyUse()),
                normalize(quote.zipCode()),
                quote.homePrice().toPlainString(),
                quote.downPayment().toPlainString(),
                String.valueOf(quote.termYears())
        ));
    }

    private String refinedKey(PricingScenario quote, QuoteRefinementRequestDto request) {
        return "pricing:refined:" + hash(String.join("|",
                normalize(quote.loanProgram()),
                normalize(quote.propertyUse()),
                normalize(quote.zipCode()),
                quote.homePrice().toPlainString(),
                quote.downPayment().toPlainString(),
                String.valueOf(quote.termYears()),
                String.valueOf(request.creditScore()),
                request.annualIncome().toPlainString(),
                request.monthlyDebts().toPlainString(),
                request.cashReserves().toPlainString(),
                String.valueOf(request.firstTimeBuyer()),
                String.valueOf(request.vaEligible())
        ));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String hash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
