package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.lead.model.MortgageLead;
import com.jaycodesx.mortgage.lead.repository.MortgageLeadRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
    CommandLineRunner seedLeads(MortgageLeadRepository mortgageLeadRepository) {
        return args -> {
            if (mortgageLeadRepository.count() > 0) {
                return;
            }

            mortgageLeadRepository.saveAll(List.of(
                    createLead(101L, 201L, "NEW", "PUBLIC_QUOTE_FUNNEL"),
                    createLead(102L, 202L, "CONTACTED", "REFINED_QUOTE"),
                    createLead(103L, 203L, "QUALIFIED", "ADMIN_WORKSPACE")
            ));
        };
    }

    private MortgageLead createLead(
            Long loanQuoteId,
            Long borrowerQuoteProfileId,
            String leadStatus,
            String leadSource
    ) {
        MortgageLead lead = new MortgageLead();
        lead.setLoanQuoteId(loanQuoteId);
        lead.setBorrowerQuoteProfileId(borrowerQuoteProfileId);
        lead.setLeadStatus(leadStatus);
        lead.setLeadSource(leadSource);
        return lead;
    }
}
