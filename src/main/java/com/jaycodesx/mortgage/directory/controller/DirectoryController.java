package com.jaycodesx.mortgage.directory.controller;

import com.jaycodesx.mortgage.directory.dto.DirectoryAgentResponseDto;
import com.jaycodesx.mortgage.directory.dto.DirectoryLenderResponseDto;
import com.jaycodesx.mortgage.directory.dto.LocationResponseDto;
import com.jaycodesx.mortgage.directory.model.DirectoryAgent;
import com.jaycodesx.mortgage.directory.model.DirectoryLender;
import com.jaycodesx.mortgage.directory.repository.DirectoryAgentRepository;
import com.jaycodesx.mortgage.directory.repository.DirectoryLenderRepository;
import com.jaycodesx.mortgage.directory.service.DirectoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/directory")
public class DirectoryController {

    private final DirectoryService directoryService;
    private final DirectoryAgentRepository agentRepository;
    private final DirectoryLenderRepository lenderRepository;

    public DirectoryController(
            DirectoryService directoryService,
            DirectoryAgentRepository agentRepository,
            DirectoryLenderRepository lenderRepository
    ) {
        this.directoryService = directoryService;
        this.agentRepository = agentRepository;
        this.lenderRepository = lenderRepository;
    }

    @GetMapping("/locations")
    public Mono<ResponseEntity<List<LocationResponseDto>>> getLocations() {
        return Mono.just(ResponseEntity.ok(directoryService.getLocations()));
    }

    @GetMapping("/agents")
    public Mono<ResponseEntity<List<DirectoryAgentResponseDto>>> getAgents(
            @RequestParam String stateCode,
            @RequestParam String countyName
    ) {
        List<DirectoryAgentResponseDto> agents = agentRepository
                .findByStateCodeAndCountyName(stateCode, countyName)
                .stream()
                .map(DirectoryController::toAgentDto)
                .toList();
        return Mono.just(ResponseEntity.ok(agents));
    }

    @GetMapping("/lenders")
    public Mono<ResponseEntity<List<DirectoryLenderResponseDto>>> getLenders(
            @RequestParam String stateCode,
            @RequestParam String countyName
    ) {
        List<DirectoryLenderResponseDto> lenders = lenderRepository
                .findByStateCodeAndCountyName(stateCode, countyName)
                .stream()
                .map(DirectoryController::toLenderDto)
                .toList();
        return Mono.just(ResponseEntity.ok(lenders));
    }

    private static DirectoryAgentResponseDto toAgentDto(DirectoryAgent a) {
        return new DirectoryAgentResponseDto(
                a.getId(),
                a.getStateCode(),
                a.getCountyName(),
                a.getFirstName(),
                a.getLastName(),
                a.getCompanyName(),
                a.getEmail(),
                a.getPhone(),
                a.getLicenseNumber(),
                a.getNmlsId(),
                a.getSpecialty(),
                a.getAvgResponseHours(),
                a.getRating()
        );
    }

    private static DirectoryLenderResponseDto toLenderDto(DirectoryLender l) {
        return new DirectoryLenderResponseDto(
                l.getId(),
                l.getStateCode(),
                l.getCountyName(),
                l.getInstitutionName(),
                l.getContactName(),
                l.getEmail(),
                l.getPhone(),
                l.getLicenseNumber(),
                l.getNmlsId(),
                l.getLoanTypes(),
                l.getMinCreditScore(),
                l.getAvgSlaHours(),
                l.getRating()
        );
    }
}
