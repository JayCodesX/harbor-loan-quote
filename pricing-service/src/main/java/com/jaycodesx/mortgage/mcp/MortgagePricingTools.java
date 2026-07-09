package com.jaycodesx.mortgage.mcp;

import com.jaycodesx.mortgage.pricing.model.PricingProduct;
import com.jaycodesx.mortgage.pricing.repository.PricingProductRepository;
import com.jaycodesx.mortgage.pricing.service.QuotePricingService;
import com.jaycodesx.mortgage.pricing.service.QuotePricingService.QuoteDecision;
import com.jaycodesx.mortgage.quote.service.PricingScenario;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * MCP tools exposing Harbor's mortgage pricing engine to AI agents.
 *
 * Each @Tool method becomes a discoverable, typed capability that any
 * MCP-compatible agent can call. The method is a THIN ADAPTER: it takes the
 * flat arguments an LLM can produce, maps them onto our existing domain type
 * (PricingScenario), delegates to the real business logic (QuotePricingService),
 * and shapes the result back into a clean, agent-friendly record.
 *
 * We do NOT reimplement pricing here. All the real work still lives in
 * QuotePricingService.pricePublicQuote(...). This class only translates.
 */
@Service
public class MortgagePricingTools {

    private final QuotePricingService pricingService;
    private final PricingProductRepository productRepository;

    public MortgagePricingTools(QuotePricingService pricingService,
                                PricingProductRepository productRepository) {
        this.pricingService = pricingService;
        this.productRepository = productRepository;
    }

    /**
     * The @Tool annotation is the contract. Spring AI reads the method
     * signature + these descriptions and generates a JSON schema that the
     * MCP server advertises. The agent's LLM reads that schema to decide
     * when and how to call this tool. The description text is what the LLM
     * "sees", so it is written for the model, not for a human reader.
     */
    @Tool(description = """
            Get an estimated mortgage rate quote (a public market estimate, no personal
            credit data required) for a given property purchase and loan scenario.
            Returns the estimated interest rate, APR, monthly payment, cash to close,
            and a qualification tier label. Use this when a user asks what rate or
            payment they might get for a home purchase.""")
    public RateQuoteResult getRateQuote(
            @ToolParam(description = "Home purchase price in US dollars, e.g. 500000")
            BigDecimal homePrice,

            @ToolParam(description = "Down payment in US dollars, e.g. 100000")
            BigDecimal downPayment,

            @ToolParam(description = "5-digit property ZIP code, e.g. 89101")
            String zipCode,

            @ToolParam(description = "Loan program: one of CONVENTIONAL, FHA, VA")
            String loanProgram,

            @ToolParam(description = "Property use: one of PRIMARY, SECONDARY, INVESTMENT")
            String propertyUse,

            @ToolParam(description = "Loan term in years, typically 15 or 30")
            Integer termYears
    ) {
        // Map the agent's flat arguments onto our existing domain record.
        // quoteId is null: this is an anonymous public estimate, not tied to a saved quote.
        PricingScenario scenario = new PricingScenario(
                null, homePrice, downPayment, zipCode, loanProgram, propertyUse, termYears);

        // Delegate to the REAL pricing engine (unchanged, still cached, still tested).
        QuoteDecision decision = pricingService.pricePublicQuote(scenario);

        // Shape the internal decision into a clean, agent-facing result.
        return new RateQuoteResult(
                decision.estimatedRate(),
                decision.estimatedApr(),
                decision.estimatedMonthlyPayment(),
                decision.estimatedCashToClose(),
                decision.qualificationTier());
    }

    /**
     * A discovery tool. Before an agent can quote intelligently it needs to know
     * which loan programs the lender actually offers. Rather than hard-coding the
     * enum values into the getRateQuote description (which would go stale), this
     * tool returns the live catalog straight from the pricing_products table, so
     * the agent's knowledge of available programs tracks the data, not a comment.
     *
     * Note there are NO @ToolParam arguments: the agent calls this with an empty
     * argument object. Spring AI still advertises it in tools/list with an empty
     * input schema.
     */
    @Tool(description = """
            List the mortgage loan programs Harbor currently offers, with each
            program's code, display name, and current base interest rate. Call this
            first when you need to know which loan programs are available before
            quoting, or when a user asks what loan options exist.""")
    public List<LoanProgramSummary> listLoanPrograms() {
        return productRepository.findByActiveTrueOrderByProgramCode().stream()
                .map(MortgagePricingTools::toSummary)
                .toList();
    }

    /**
     * The single-item counterpart to listLoanPrograms. An agent that already knows
     * a program code (e.g. the user said "the FHA one") can look up just that
     * program's details without pulling the whole catalog.
     *
     * This shows a second tool shape: a tool that can fail to find its target. We
     * return a nullable result rather than throwing, because a clean "not found"
     * is something the LLM can reason about and relay, whereas an exception surfaces
     * to the agent as an opaque tool error.
     */
    @Tool(description = """
            Get the details of a single mortgage loan program by its program code
            (for example CONVENTIONAL, FHA, or VA). Returns the program's code,
            display name, and current base rate, or null if no active program has
            that code. Use listLoanPrograms first if you do not know the exact code.""")
    public LoanProgramSummary getLoanProgramDetails(
            @ToolParam(description = "The loan program code, e.g. CONVENTIONAL, FHA, or VA")
            String programCode
    ) {
        return productRepository.findByProgramCodeAndActiveTrue(programCode)
                .map(MortgagePricingTools::toSummary)
                .orElse(null);
    }

    private static LoanProgramSummary toSummary(PricingProduct product) {
        return new LoanProgramSummary(
                product.getProgramCode(),
                product.getProductName(),
                product.getBaseRate());
    }

    /**
     * Catalog-facing result shape for the two program tools. Kept separate from
     * PricingProduct (the JPA entity) so the agent contract never exposes internal
     * columns like the primary key or the active flag — only what an agent needs.
     */
    public record LoanProgramSummary(
            String programCode,
            String productName,
            BigDecimal baseRate
    ) {
    }

    /**
     * The tool's return shape. Spring AI serializes this record to JSON and
     * includes its structure in the tool schema, so the agent knows exactly
     * what fields come back. A dedicated record (rather than returning the
     * internal QuoteDecision) keeps the agent contract decoupled from internals.
     */
    public record RateQuoteResult(
            BigDecimal estimatedRate,
            BigDecimal estimatedApr,
            BigDecimal estimatedMonthlyPayment,
            BigDecimal estimatedCashToClose,
            String qualificationTier
    ) {
    }
}
