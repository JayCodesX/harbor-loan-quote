package com.jaycodesx.mortgage.consent.repository;

import com.jaycodesx.mortgage.consent.model.ConsentAuditLog;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Append-only repository for consent audit log entries.
 *
 * Extends {@link Repository} directly rather than {@link org.springframework.data.jpa.repository.JpaRepository}
 * so that only explicitly declared methods are available. Delete and update operations are
 * intentionally absent — consent records are legal evidence and must never be removed or
 * modified. Any attempt to add delete* or update* methods to this interface must be
 * reviewed by a compliance owner before merging.
 */
public interface ConsentAuditLogRepository extends Repository<ConsentAuditLog, Long> {

    ConsentAuditLog save(ConsentAuditLog entry);

    List<ConsentAuditLog> findByLoanQuoteIdOrderByRecordedAtAsc(Long loanQuoteId);

    List<ConsentAuditLog> findByBorrowerQuoteProfileIdOrderByRecordedAtAsc(Long borrowerQuoteProfileId);
}
