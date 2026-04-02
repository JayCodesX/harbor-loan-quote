package com.jaycodesx.mortgage.infrastructure.admin;

import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Callable;

@RestController
@RequestMapping("/admin")
public class AdminOperationsController {

    private final UserTokenAuthorizationService userTokenAuthorizationService;
    private final AdminMetricsClientService adminMetricsClientService;
    private final AdminReportService adminReportService;

    public AdminOperationsController(
            UserTokenAuthorizationService userTokenAuthorizationService,
            AdminMetricsClientService adminMetricsClientService,
            AdminReportService adminReportService
    ) {
        this.userTokenAuthorizationService = userTokenAuthorizationService;
        this.adminMetricsClientService = adminMetricsClientService;
        this.adminReportService = adminReportService;
    }

    @GetMapping("/products")
    public Mono<ResponseEntity<List<AdminPricingProductResponseDto>>> getProducts(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminCall(authorizationHeader, () -> ResponseEntity.ok(adminMetricsClientService.fetchProducts()));
    }

    @PostMapping("/products")
    public Mono<ResponseEntity<AdminPricingProductResponseDto>> createProduct(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminPricingProductRequestDto request
    ) {
        return adminCall(authorizationHeader, () -> ResponseEntity.status(HttpStatus.CREATED).body(adminMetricsClientService.createProduct(request)));
    }

    @PutMapping("/products/{id}")
    public Mono<ResponseEntity<AdminPricingProductResponseDto>> updateProduct(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @RequestBody AdminPricingProductRequestDto request
    ) {
        return adminCall(authorizationHeader, () -> ResponseEntity.ok(adminMetricsClientService.updateProduct(id, request)));
    }

    @DeleteMapping("/products/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        return adminCall(authorizationHeader, () -> {
            adminMetricsClientService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping("/reports/query")
    public Mono<ResponseEntity<AdminReportResponseDto>> queryReport(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminReportQueryDto query
    ) {
        return adminCall(authorizationHeader, () -> ResponseEntity.ok(adminReportService.runReport(query)));
    }

    @PostMapping("/rate-sheets")
    public Mono<ResponseEntity<AdminRateSheetPublishResponseDto>> publishRateSheet(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminRateSheetPublishRequestDto request
    ) {
        return adminCall(authorizationHeader, () -> ResponseEntity.status(HttpStatus.CREATED).body(adminMetricsClientService.publishRateSheet(request)));
    }

    private <T> Mono<ResponseEntity<T>> adminCall(String authorizationHeader, Callable<ResponseEntity<T>> callable) {
        try {
            userTokenAuthorizationService.requireAdminUser(authorizationHeader);
            return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "Admin role required".equals(ex.getMessage()) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
            return Mono.error(new ResponseStatusException(status, ex.getMessage(), ex));
        }
    }
}
