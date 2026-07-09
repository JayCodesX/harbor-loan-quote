package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.RateChangeAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateChangeAuditRepository extends JpaRepository<RateChangeAudit, Long> {

    boolean existsByRateSheetId(Long rateSheetId);
}
