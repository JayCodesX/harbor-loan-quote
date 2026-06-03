package com.jaycodesx.mortgage.lead.metrics;

import com.jaycodesx.mortgage.infrastructure.admin.LeadMetricsResponseDto;
import com.jaycodesx.mortgage.infrastructure.admin.MetricSliceDto;
import com.jaycodesx.mortgage.lead.model.MortgageLead;
import com.jaycodesx.mortgage.lead.repository.MortgageLeadRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeadCatalogMetricsService {

    private final MortgageLeadRepository mortgageLeadRepository;

    public LeadCatalogMetricsService(MortgageLeadRepository mortgageLeadRepository) {
        this.mortgageLeadRepository = mortgageLeadRepository;
    }

    public LeadMetricsResponseDto getSnapshot() {
        List<MortgageLead> leads = mortgageLeadRepository.findAll();
        Map<String, Long> statusDistribution = leads.stream()
                .collect(Collectors.groupingBy(MortgageLead::getLeadStatus, Collectors.counting()));
        Map<String, Long> sourceDistribution = leads.stream()
                .collect(Collectors.groupingBy(MortgageLead::getLeadSource, Collectors.counting()));

        return new LeadMetricsResponseDto(
                leads.size(),
                statusDistribution.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new MetricSliceDto(entry.getKey(), entry.getValue()))
                        .toList(),
                sourceDistribution.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new MetricSliceDto(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }
}
