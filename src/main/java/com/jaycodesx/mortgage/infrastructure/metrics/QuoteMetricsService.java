package com.jaycodesx.mortgage.infrastructure.metrics;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class QuoteMetricsService {

    private static final String QUOTES_STARTED_KEY = "metrics:quotes:started";
    private static final String QUOTE_REFINEMENTS_KEY = "metrics:quotes:refinements-requested";
    private static final String QUOTES_DEDUPED_KEY = "metrics:quotes:deduped";
    private static final String QUOTES_COMPLETED_KEY = "metrics:quotes:completed";
    private static final String QUOTES_FAILED_KEY = "metrics:quotes:failed";
    private static final String LEADS_CREATED_KEY = "metrics:leads:created";
    private static final String PRICING_DURATION_SUM_KEY = "metrics:timing:pricing:sum-ms";
    private static final String PRICING_DURATION_COUNT_KEY = "metrics:timing:pricing:count";
    private static final String LEAD_DURATION_SUM_KEY = "metrics:timing:lead:sum-ms";
    private static final String LEAD_DURATION_COUNT_KEY = "metrics:timing:lead:count";
    private static final String QUOTE_SESSIONS_KEY = "metrics:sessions:quotes";
    private static final String AUTHENTICATED_SESSIONS_KEY = "metrics:sessions:authenticated";
    private static final String REFINED_SESSIONS_KEY = "metrics:sessions:refined";
    private static final String LEAD_SESSIONS_KEY = "metrics:sessions:lead-converted";
    private static final String AUTH_REGISTRATIONS_KEY = "metrics:auth:registrations";
    private static final String AUTH_LOGINS_KEY = "metrics:auth:logins";
    private static final String PRICING_RESULT_MESSAGES_DEDUPED_KEY = "metrics:messaging:pricing-result:deduped";
    private static final String LEAD_RESULT_MESSAGES_DEDUPED_KEY = "metrics:messaging:lead-result:deduped";
    private static final String PRICING_RESULT_DLQ_PUBLISHES_KEY = "metrics:messaging:pricing-result:dlq-publishes";
    private static final String LEAD_RESULT_DLQ_PUBLISHES_KEY = "metrics:messaging:lead-result:dlq-publishes";

    private final StringRedisTemplate redisTemplate;

    public QuoteMetricsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordQuoteStarted(Long quoteId, String sessionId) {
        increment(QUOTES_STARTED_KEY);
        markPricingQueued(quoteId);
        addToSet(QUOTE_SESSIONS_KEY, sessionId);
    }

    public void recordQuoteRefinementRequested(Long quoteId, String sessionId) {
        increment(QUOTE_REFINEMENTS_KEY);
        markPricingQueued(quoteId);
        markLeadQueued(quoteId);
        addToSet(QUOTE_SESSIONS_KEY, sessionId);
        addToSet(REFINED_SESSIONS_KEY, sessionId);
    }

    public void recordQuoteDeduped() {
        increment(QUOTES_DEDUPED_KEY);
    }

    public void recordSessionAuthenticated(String sessionId, String authEventType) {
        addToSet(AUTHENTICATED_SESSIONS_KEY, sessionId);
        if ("REGISTER".equalsIgnoreCase(authEventType)) {
            increment(AUTH_REGISTRATIONS_KEY);
            return;
        }
        increment(AUTH_LOGINS_KEY);
    }

    public void recordLeadCreated(Long quoteId) {
        increment(LEADS_CREATED_KEY);
        String startValue = redisTemplate.opsForValue().get(leadStartKey(quoteId));
        if (startValue == null) {
            return;
        }
        long duration = Math.max(0L, System.currentTimeMillis() - Long.parseLong(startValue));
        redisTemplate.opsForValue().increment(LEAD_DURATION_SUM_KEY, duration);
        redisTemplate.opsForValue().increment(LEAD_DURATION_COUNT_KEY);
        redisTemplate.delete(leadStartKey(quoteId));
    }

    public void recordLeadCaptured(String sessionId) {
        addToSet(LEAD_SESSIONS_KEY, sessionId);
    }

    public void recordPricingResultMessageDeduped() {
        increment(PRICING_RESULT_MESSAGES_DEDUPED_KEY);
    }

    public void recordLeadResultMessageDeduped() {
        increment(LEAD_RESULT_MESSAGES_DEDUPED_KEY);
    }

    public void recordPricingResultDlqPublish() {
        increment(PRICING_RESULT_DLQ_PUBLISHES_KEY);
    }

    public void recordLeadResultDlqPublish() {
        increment(LEAD_RESULT_DLQ_PUBLISHES_KEY);
    }

    public QuoteMetricsResponseDto getSnapshot() {
        long sessionsWithQuotes = getSetSize(QUOTE_SESSIONS_KEY);
        long authenticatedSessions = getSetSize(AUTHENTICATED_SESSIONS_KEY);
        long sessionsWithRefinements = getSetSize(REFINED_SESSIONS_KEY);
        long leadConvertedSessions = getSetSize(LEAD_SESSIONS_KEY);

        return new QuoteMetricsResponseDto(
                getLong(QUOTES_STARTED_KEY),
                getLong(QUOTE_REFINEMENTS_KEY),
                getLong(QUOTES_DEDUPED_KEY),
                getLong(QUOTES_COMPLETED_KEY),
                getLong(QUOTES_FAILED_KEY),
                getLong(LEADS_CREATED_KEY),
                average(PRICING_DURATION_SUM_KEY, PRICING_DURATION_COUNT_KEY),
                average(LEAD_DURATION_SUM_KEY, LEAD_DURATION_COUNT_KEY),
                sessionsWithQuotes,
                authenticatedSessions,
                sessionsWithRefinements,
                leadConvertedSessions,
                getLong(AUTH_REGISTRATIONS_KEY),
                getLong(AUTH_LOGINS_KEY),
                percentage(authenticatedSessions, sessionsWithQuotes),
                percentage(sessionsWithRefinements, authenticatedSessions),
                percentage(leadConvertedSessions, sessionsWithQuotes),
                getLong(PRICING_RESULT_MESSAGES_DEDUPED_KEY),
                getLong(LEAD_RESULT_MESSAGES_DEDUPED_KEY),
                getLong(PRICING_RESULT_DLQ_PUBLISHES_KEY),
                getLong(LEAD_RESULT_DLQ_PUBLISHES_KEY)
        );
    }

    private void markPricingQueued(Long quoteId) {
        redisTemplate.opsForValue().set(pricingStartKey(quoteId), String.valueOf(System.currentTimeMillis()));
    }

    private void markLeadQueued(Long quoteId) {
        redisTemplate.opsForValue().set(leadStartKey(quoteId), String.valueOf(System.currentTimeMillis()));
    }

    private long getLong(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(Long::parseLong)
                .orElse(0L);
    }

    private long getSetSize(String key) {
        return Optional.ofNullable(redisTemplate.opsForSet().size(key)).orElse(0L);
    }

    private long average(String sumKey, String countKey) {
        long count = getLong(countKey);
        if (count == 0) {
            return 0L;
        }
        return getLong(sumKey) / count;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    private void addToSet(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        SetOperations<String, String> setOperations = redisTemplate.opsForSet();
        setOperations.add(key, value);
    }

    private String pricingStartKey(Long quoteId) {
        return "metrics:quote:pricing:start:" + quoteId;
    }

    private String leadStartKey(Long quoteId) {
        return "metrics:quote:lead:start:" + quoteId;
    }
}
