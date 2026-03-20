package com.jaycodesx.mortgage.directory.service;

import com.jaycodesx.mortgage.directory.dto.LocationResponseDto;
import com.jaycodesx.mortgage.directory.repository.DirectoryLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DirectoryService {

    private final DirectoryLocationRepository repository;
    private final DirectoryCacheService cacheService;

    public DirectoryService(DirectoryLocationRepository repository, DirectoryCacheService cacheService) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    public List<LocationResponseDto> getLocations() {
        return cacheService.getLocations().orElseGet(() -> {
            List<String> stateCodes = repository.findDistinctStateCodes();
            List<LocationResponseDto> locations = stateCodes.stream()
                    .map(stateCode -> {
                        List<String> counties = repository
                                .findByStateCodeOrderByCountyNameAsc(stateCode)
                                .stream()
                                .map(loc -> loc.getCountyName())
                                .toList();
                        return new LocationResponseDto(stateCode, counties);
                    })
                    .toList();
            cacheService.cacheLocations(locations);
            return locations;
        });
    }
}
