package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.pricing.dto.PricingProductAdminRequestDto;
import com.jaycodesx.mortgage.pricing.model.PricingProduct;
import com.jaycodesx.mortgage.pricing.repository.PricingProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricingProductAdminServiceTest {

    @Test
    void createsPricingProduct() {
        PricingProductRepository repository = mock(PricingProductRepository.class);
        PricingCacheService cache = mock(PricingCacheService.class);
        when(repository.save(any(PricingProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricingProductAdminService service = new PricingProductAdminService(repository, cache);
        var response = service.create(new PricingProductAdminRequestDto("VA", "VA Fixed", new BigDecimal("5.8750"), true));

        assertThat(response.programCode()).isEqualTo("VA");
        assertThat(response.productName()).isEqualTo("VA Fixed");
        // Base-rate change must invalidate the pricing cache so quotes re-price immediately.
        verify(cache, times(1)).evictAll();
    }

    @Test
    void updateEvictsPricingCache() {
        PricingProductRepository repository = mock(PricingProductRepository.class);
        PricingCacheService cache = mock(PricingCacheService.class);
        PricingProduct existing = new PricingProduct();
        existing.setProgramCode("CONVENTIONAL");
        existing.setProductName("Conventional 30");
        existing.setBaseRate(new BigDecimal("6.2500"));
        existing.setActive(true);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(PricingProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricingProductAdminService service = new PricingProductAdminService(repository, cache);
        service.update(1L, new PricingProductAdminRequestDto("CONVENTIONAL", "Conventional 30", new BigDecimal("6.2000"), true));

        verify(cache, times(1)).evictAll();
    }

    @Test
    void deleteEvictsPricingCache() {
        PricingProductRepository repository = mock(PricingProductRepository.class);
        PricingCacheService cache = mock(PricingCacheService.class);
        when(repository.existsById(1L)).thenReturn(true);

        PricingProductAdminService service = new PricingProductAdminService(repository, cache);
        service.delete(1L);

        verify(repository).deleteById(1L);
        verify(cache, times(1)).evictAll();
    }

    @Test
    void listsProducts() {
        PricingProductRepository repository = mock(PricingProductRepository.class);
        PricingCacheService cache = mock(PricingCacheService.class);
        PricingProduct product = new PricingProduct();
        product.setProgramCode("FHA");
        product.setProductName("FHA Streamline");
        product.setBaseRate(new BigDecimal("6.2500"));
        product.setActive(true);
        when(repository.findAll()).thenReturn(List.of(product));

        PricingProductAdminService service = new PricingProductAdminService(repository, cache);
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void throwsWhenUpdatingMissingProduct() {
        PricingProductRepository repository = mock(PricingProductRepository.class);
        PricingCacheService cache = mock(PricingCacheService.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        PricingProductAdminService service = new PricingProductAdminService(repository, cache);
        assertThatThrownBy(() -> service.update(99L, new PricingProductAdminRequestDto("FHA", "Test", new BigDecimal("6.0"), true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        // A failed lookup must not touch the cache.
        verify(cache, never()).evictAll();
    }
}
