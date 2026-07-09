package com.jaycodesx.mortgage.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers our @Tool-annotated objects with Spring AI.
 *
 * The MCP server auto-configuration looks for ToolCallbackProvider beans on
 * startup and publishes every @Tool method they contain over the MCP protocol.
 * MethodToolCallbackProvider scans the given objects for @Tool methods and
 * builds the callbacks (schema + invoker) for each one.
 *
 * To add more tool classes later, pass them all here:
 *   .toolObjects(pricingTools, productTools, llpaTools)
 */
@Configuration
@EnableConfigurationProperties(McpSecurityProperties.class)
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider mortgageTools(MortgagePricingTools pricingTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(pricingTools)
                .build();
    }
}
