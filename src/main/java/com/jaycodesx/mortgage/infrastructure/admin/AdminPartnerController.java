package com.jaycodesx.mortgage.infrastructure.admin;

import com.jaycodesx.mortgage.directory.model.DirectoryAgent;
import com.jaycodesx.mortgage.directory.model.DirectoryLender;
import com.jaycodesx.mortgage.directory.repository.DirectoryAgentRepository;
import com.jaycodesx.mortgage.directory.repository.DirectoryLenderRepository;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;

@RestController
@RequestMapping("/admin")
public class AdminPartnerController {

    private final UserTokenAuthorizationService userTokenAuthorizationService;
    private final DirectoryAgentRepository agentRepository;
    private final DirectoryLenderRepository lenderRepository;

    public AdminPartnerController(
            UserTokenAuthorizationService userTokenAuthorizationService,
            DirectoryAgentRepository agentRepository,
            DirectoryLenderRepository lenderRepository
    ) {
        this.userTokenAuthorizationService = userTokenAuthorizationService;
        this.agentRepository = agentRepository;
        this.lenderRepository = lenderRepository;
    }

    // ── Agents ──────────────────────────────────────────────────────────────

    @GetMapping("/agents")
    public Mono<ResponseEntity<AdminPartnerPageResponseDto>> listAgents(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String countyName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return adminCall(auth, () -> {
            if (stateCode == null || stateCode.isBlank() || countyName == null || countyName.isBlank()) {
                return ResponseEntity.ok(new AdminPartnerPageResponseDto(List.of(), 0, 0, page, size));
            }
            PageRequest pageable = PageRequest.of(page, size, Sort.by("firstName", "lastName"));
            Page<DirectoryAgent> result = agentRepository.findByStateCodeAndCountyName(stateCode, countyName, pageable);
            return ResponseEntity.ok(new AdminPartnerPageResponseDto(
                    result.getContent().stream().map(this::toAgentDto).toList(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    page,
                    size
            ));
        });
    }

    @PostMapping("/agents")
    public Mono<ResponseEntity<AdminPartnerResponseDto>> createAgent(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody AdminPartnerRequestDto request
    ) {
        return adminCall(auth, () -> {
            DirectoryAgent agent = applyToAgent(new DirectoryAgent(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(toAgentDto(agentRepository.save(agent)));
        });
    }

    @PutMapping("/agents/{id}")
    public Mono<ResponseEntity<AdminPartnerResponseDto>> updateAgent(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody AdminPartnerRequestDto request
    ) {
        return adminCall(auth, () -> {
            DirectoryAgent agent = agentRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
            applyToAgent(agent, request);
            return ResponseEntity.ok(toAgentDto(agentRepository.save(agent)));
        });
    }

    @DeleteMapping("/agents/{id}")
    public Mono<ResponseEntity<Void>> deleteAgent(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id
    ) {
        return adminCall(auth, () -> {
            agentRepository.deleteById(id);
            return ResponseEntity.<Void>noContent().build();
        });
    }

    @PostMapping("/agents/sync")
    public Mono<ResponseEntity<List<AdminPartnerResponseDto>>> syncAgents(
            @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        return adminCall(auth, () -> ResponseEntity.ok(
                agentRepository.findAll().stream().map(this::toAgentDto).toList()
        ));
    }

    // ── Lenders ─────────────────────────────────────────────────────────────

    @GetMapping("/lenders")
    public Mono<ResponseEntity<AdminPartnerPageResponseDto>> listLenders(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String countyName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return adminCall(auth, () -> {
            if (stateCode == null || stateCode.isBlank() || countyName == null || countyName.isBlank()) {
                return ResponseEntity.ok(new AdminPartnerPageResponseDto(List.of(), 0, 0, page, size));
            }
            PageRequest pageable = PageRequest.of(page, size, Sort.by("institutionName"));
            Page<DirectoryLender> result = lenderRepository.findByStateCodeAndCountyName(stateCode, countyName, pageable);
            return ResponseEntity.ok(new AdminPartnerPageResponseDto(
                    result.getContent().stream().map(this::toLenderDto).toList(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    page,
                    size
            ));
        });
    }

    @PostMapping("/lenders")
    public Mono<ResponseEntity<AdminPartnerResponseDto>> createLender(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody AdminPartnerRequestDto request
    ) {
        return adminCall(auth, () -> {
            DirectoryLender lender = applyToLender(new DirectoryLender(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(toLenderDto(lenderRepository.save(lender)));
        });
    }

    @PutMapping("/lenders/{id}")
    public Mono<ResponseEntity<AdminPartnerResponseDto>> updateLender(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody AdminPartnerRequestDto request
    ) {
        return adminCall(auth, () -> {
            DirectoryLender lender = lenderRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lender not found"));
            applyToLender(lender, request);
            return ResponseEntity.ok(toLenderDto(lenderRepository.save(lender)));
        });
    }

    @DeleteMapping("/lenders/{id}")
    public Mono<ResponseEntity<Void>> deleteLender(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id
    ) {
        return adminCall(auth, () -> {
            lenderRepository.deleteById(id);
            return ResponseEntity.<Void>noContent().build();
        });
    }

    @PostMapping("/lenders/sync")
    public Mono<ResponseEntity<List<AdminPartnerResponseDto>>> syncLenders(
            @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        return adminCall(auth, () -> ResponseEntity.ok(
                lenderRepository.findAll().stream().map(this::toLenderDto).toList()
        ));
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private AdminPartnerResponseDto toAgentDto(DirectoryAgent a) {
        return new AdminPartnerResponseDto(
                a.getId(),
                a.getFirstName() + " " + a.getLastName(),
                a.getCompanyName(),
                a.getEmail(),
                a.getPhone(),
                a.getStateCode(),
                a.getCountyName(),
                a.getCity(),
                a.getSpecialty(),
                a.getLicenseNumber(),
                a.getNmlsId(),
                a.getRankingScore(),
                a.getAvgResponseHours(),
                a.getLanguages(),
                a.getWebsiteUrl(),
                a.isActive()
        );
    }

    private AdminPartnerResponseDto toLenderDto(DirectoryLender l) {
        return new AdminPartnerResponseDto(
                l.getId(),
                l.getInstitutionName(),
                l.getInstitutionName(),
                l.getEmail(),
                l.getPhone(),
                l.getStateCode(),
                l.getCountyName(),
                l.getCity(),
                l.getLoanTypes(),
                l.getLicenseNumber(),
                l.getNmlsId(),
                l.getRankingScore(),
                l.getAvgSlaHours(),
                l.getLanguages(),
                l.getWebsiteUrl(),
                l.isActive()
        );
    }

    private DirectoryAgent applyToAgent(DirectoryAgent agent, AdminPartnerRequestDto req) {
        String displayName = req.displayName() != null ? req.displayName().trim() : "";
        String[] parts = displayName.split(" ", 2);
        agent.setFirstName(parts[0]);
        agent.setLastName(parts.length > 1 ? parts[1] : "");
        agent.setCompanyName(req.companyName());
        agent.setEmail(req.email());
        agent.setPhone(req.phone());
        agent.setStateCode(req.stateCode());
        agent.setCountyName(req.countyName());
        agent.setCity(req.city());
        agent.setSpecialty(req.specialty());
        agent.setLicenseNumber(req.licenseNumber());
        agent.setNmlsId(req.nmlsId());
        agent.setRankingScore(req.rankingScore());
        agent.setAvgResponseHours(req.responseSlaHours() != null ? req.responseSlaHours() : 4);
        agent.setRating(req.rankingScore() != null
                ? BigDecimal.valueOf(req.rankingScore() / 20.0).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.valueOf(4.50));
        agent.setLanguages(req.languages());
        agent.setWebsiteUrl(req.websiteUrl());
        agent.setActive(req.active());
        return agent;
    }

    private DirectoryLender applyToLender(DirectoryLender lender, AdminPartnerRequestDto req) {
        String name = req.displayName() != null ? req.displayName().trim() : req.companyName();
        lender.setInstitutionName(name);
        String contactName = req.companyName() != null ? req.companyName() : name;
        lender.setContactName(contactName);
        lender.setEmail(req.email());
        lender.setPhone(req.phone());
        lender.setStateCode(req.stateCode());
        lender.setCountyName(req.countyName());
        lender.setCity(req.city());
        lender.setLoanTypes(req.specialty());
        lender.setLicenseNumber(req.licenseNumber());
        lender.setNmlsId(req.nmlsId());
        lender.setRankingScore(req.rankingScore());
        lender.setAvgSlaHours(req.responseSlaHours() != null ? req.responseSlaHours() : 4);
        lender.setRating(req.rankingScore() != null
                ? BigDecimal.valueOf(req.rankingScore() / 20.0).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.valueOf(4.50));
        lender.setLanguages(req.languages());
        lender.setWebsiteUrl(req.websiteUrl());
        lender.setActive(req.active());
        lender.setMinCreditScore(620);
        return lender;
    }

    // ── Auth helper ──────────────────────────────────────────────────────────

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
