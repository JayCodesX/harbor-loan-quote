package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.pricing.dto.PricingProductAdminRequestDto;
import com.jaycodesx.mortgage.pricing.dto.PricingProductAdminResponseDto;
import com.jaycodesx.mortgage.pricing.model.PricingProduct;
import com.jaycodesx.mortgage.pricing.repository.PricingProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PricingProductAdminService {

    private final PricingProductRepository pricingProductRepository;
    private final PricingCacheService pricingCacheService;

    public PricingProductAdminService(PricingProductRepository pricingProductRepository,
                                      PricingCacheService pricingCacheService) {
        this.pricingProductRepository = pricingProductRepository;
        this.pricingCacheService = pricingCacheService;
    }

    public List<PricingProductAdminResponseDto> findAll() {
        return pricingProductRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PricingProductAdminResponseDto create(PricingProductAdminRequestDto request) {
        PricingProduct product = new PricingProduct();
        apply(product, request);
        PricingProductAdminResponseDto response = toResponse(pricingProductRepository.save(product));
        // Base rates feed live quote pricing — bust the Redis cache so the change is
        // visible immediately rather than after the 5-10 min TTL (matches RateSheetService).
        pricingCacheService.evictAll();
        return response;
    }

    @Transactional
    public PricingProductAdminResponseDto update(Long id, PricingProductAdminRequestDto request) {
        PricingProduct product = pricingProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing product not found"));
        apply(product, request);
        PricingProductAdminResponseDto response = toResponse(pricingProductRepository.save(product));
        pricingCacheService.evictAll();
        return response;
    }

    @Transactional
    public void delete(Long id) {
        if (!pricingProductRepository.existsById(id)) {
            throw new IllegalArgumentException("Pricing product not found");
        }
        pricingProductRepository.deleteById(id);
        pricingCacheService.evictAll();
    }

    private void apply(PricingProduct product, PricingProductAdminRequestDto request) {
        product.setProgramCode(request.programCode());
        product.setProductName(request.productName());
        product.setBaseRate(request.baseRate());
        product.setActive(request.active());
    }

    private PricingProductAdminResponseDto toResponse(PricingProduct product) {
        return new PricingProductAdminResponseDto(
                product.getId(),
                product.getProgramCode(),
                product.getProductName(),
                product.getBaseRate(),
                product.isActive()
        );
    }
}
