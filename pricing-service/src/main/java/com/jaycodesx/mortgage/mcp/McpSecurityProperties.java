package com.jaycodesx.mortgage.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for the agent-facing MCP boundary (ADR-0053).
 *
 * The MCP server is a networked surface that autonomous agents call, so it is
 * secured and rate-limited independently of Harbor's service-to-service token
 * flow (which is for internal callers). These properties drive
 * {@link McpGatewayFilter}.
 *
 * Bound from {@code harbor.mcp.*} in application.yml via constructor binding.
 *
 * @param enabled           when false the gateway filter is bypassed entirely
 *                          (useful for local, unauthenticated exploration)
 * @param apiKeys           the set of accepted API keys; an agent presents one in
 *                          the {@code X-API-Key} header. Injected from env/secret
 *                          in real environments, never committed.
 * @param requestsPerMinute per-key token-bucket capacity and refill rate
 */
@ConfigurationProperties(prefix = "harbor.mcp")
public record McpSecurityProperties(
        boolean enabled,
        List<String> apiKeys,
        int requestsPerMinute
) {
    public McpSecurityProperties {
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 60;
        }
        if (apiKeys == null) {
            apiKeys = List.of();
        }
    }
}
