package com.jaycodesx.mortgage.quote.repository;

import com.jaycodesx.mortgage.quote.model.LoanQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanQuoteRepository extends JpaRepository<LoanQuote, Long> {

    Optional<LoanQuote> findTopBySessionIdOrderByIdDesc(String sessionId);

    List<LoanQuote> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<LoanQuote> findTopByUserIdOrderByUpdatedAtDesc(String userId);

    List<LoanQuote> findBySessionIdAndUserIdIsNull(String sessionId);
}
