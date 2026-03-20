package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.quote.dto.LoanQuoteResponseDto;
import com.jaycodesx.mortgage.quote.repository.LoanQuoteRepository;
import com.jaycodesx.mortgage.quote.service.LoanQuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataSeederTest {

    @Test
    void seedsQuotesWhenQuotesAreMissing() throws Exception {
        DataSeeder dataSeeder = new DataSeeder();
        LoanQuoteRepository loanQuoteRepository = org.mockito.Mockito.mock(LoanQuoteRepository.class);
        LoanQuoteService loanQuoteService = org.mockito.Mockito.mock(LoanQuoteService.class);

        when(loanQuoteRepository.count()).thenReturn(0L);
        when(loanQuoteService.createPublicQuote(any())).thenReturn(new LoanQuoteResponseDto(
                11L, null, "session-seed", "COMPLETED", false, "PUBLIC", "ESTIMATED", false, false,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "60614", "CONVENTIONAL",
                "PRIMARY_RESIDENCE", 30, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "Market Estimate", "next", null, null, null
        ));

        CommandLineRunner runner = dataSeeder.seedData(loanQuoteRepository, loanQuoteService);
        runner.run();

        verify(loanQuoteService, times(3)).createPublicQuote(any());
        verify(loanQuoteService, times(2)).refineQuote(any(), any());
    }

    @Test
    void skipsWhenQuotesAlreadyExist() throws Exception {
        DataSeeder dataSeeder = new DataSeeder();
        LoanQuoteRepository loanQuoteRepository = org.mockito.Mockito.mock(LoanQuoteRepository.class);
        LoanQuoteService loanQuoteService = org.mockito.Mockito.mock(LoanQuoteService.class);

        when(loanQuoteRepository.count()).thenReturn(1L);

        CommandLineRunner runner = dataSeeder.seedData(loanQuoteRepository, loanQuoteService);
        runner.run();

        verifyNoInteractions(loanQuoteService);
    }
}
