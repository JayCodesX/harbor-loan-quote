package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.PricingProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingProductRepository extends JpaRepository<PricingProduct, Long> {

    Optional<PricingProduct> findByProgramCodeAndActiveTrue(String programCode);

    List<PricingProduct> findByActiveTrueOrderByProgramCode();
}
