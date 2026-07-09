package com.jaycodesx.mortgage.mcp;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security + rate-limiting gateway for the agent-facing MCP endpoints (ADR-0053).
 *
 * <p>This filter guards ONLY the MCP transport paths ({@code /sse} and
 * {@code /mcp/**}); {@link #shouldNotFilter} returns true for everything else, so
 * the rest of pricing-service is untouched. That scoping is deliberate: the MCP
 * surface is the one exposed to autonomous agents and needs its own boundary,
 * separate from the internal service-token flow.
 *
 * <p>Two guarantees, in order:
 * <ol>
 *   <li><b>Authentication</b> — the caller must present a known key in the
 *       {@code X-API-Key} header, or the request is rejected 401. API keys (not
 *       Harbor's RSA service tokens) are the pragmatic credential for an
 *       agent boundary: an agent framework holds a static key, not a JWT it must
 *       mint.</li>
 *   <li><b>Rate limiting</b> — each key gets its own Bucket4j token bucket
 *       ({@code requestsPerMinute} tokens, refilled continuously). An LLM agent in
 *       a retry loop can otherwise hammer the pricing engine; over-limit calls get
 *       429 without touching the DB or cache.</li>
 * </ol>
 *
 * <p>The bucket store is an in-JVM {@link ConcurrentHashMap} — correct for a
 * single node. A multi-node deployment would move buckets to a shared store
 * (Bucket4j has a Redis backend, and Harbor already runs Redis); noted as future
 * work in ADR-0053 rather than built prematurely.
 */
@Component
public class McpGatewayFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-API-Key";
    private static final int SC_TOO_MANY_REQUESTS = 429;
    private static final Logger log = LoggerFactory.getLogger(McpGatewayFilter.class);

    private final McpSecurityProperties properties;
    private final Set<String> validKeys;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public McpGatewayFilter(McpSecurityProperties properties) {
        this.properties = properties;
        this.validKeys = Set.copyOf(properties.apiKeys());
        if (properties.enabled() && this.validKeys.isEmpty()) {
            log.warn("MCP gateway is enabled but no API keys are configured — "
                    + "all agent requests will be rejected with 401.");
        }
    }

    /**
     * Guard only the MCP transport. When the gateway is disabled (local dev), skip
     * everything. The SSE handshake is {@code GET /sse}; the JSON-RPC message
     * channel is {@code POST /mcp/message} — both must pass the same boundary.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.equals("/sse") || path.startsWith("/mcp/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        // 1. Authenticate.
        if (apiKey == null || !validKeys.contains(apiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
            return;
        }

        // 2. Rate-limit, one bucket per key. tryConsume is non-blocking: it either
        //    takes a token and proceeds, or returns false and we shed the request.
        Bucket bucket = buckets.computeIfAbsent(apiKey, key -> newBucket());
        if (!bucket.tryConsume(1)) {
            response.setStatus(SC_TOO_MANY_REQUESTS);
            response.setContentType("text/plain");
            response.getWriter().write("Rate limit exceeded. Try again shortly.");
            return;
        }

        chain.doFilter(request, response);
    }

    private Bucket newBucket() {
        int perMinute = properties.requestsPerMinute();
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
