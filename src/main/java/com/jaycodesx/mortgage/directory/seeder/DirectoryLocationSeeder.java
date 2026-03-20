package com.jaycodesx.mortgage.directory.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaycodesx.mortgage.directory.model.DirectoryLocation;
import com.jaycodesx.mortgage.directory.repository.DirectoryLocationRepository;
import com.jaycodesx.mortgage.directory.service.DirectoryCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class DirectoryLocationSeeder {

    private static final Logger log = LoggerFactory.getLogger(DirectoryLocationSeeder.class);

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
    CommandLineRunner seedLocations(
            DirectoryLocationRepository repository,
            DirectoryCacheService cacheService,
            ObjectMapper objectMapper
    ) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Directory locations already seeded ({} rows), skipping.", repository.count());
                return;
            }

            log.info("Seeding directory locations from classpath resource...");
            ClassPathResource resource = new ClassPathResource("data/us-states-counties.json");
            try (InputStream is = resource.getInputStream()) {
                List<Map<String, Object>> data = objectMapper.readValue(is, new TypeReference<>() {});
                List<DirectoryLocation> rows = new ArrayList<>();
                for (Map<String, Object> entry : data) {
                    String stateCode = (String) entry.get("stateCode");
                    @SuppressWarnings("unchecked")
                    List<String> counties = (List<String>) entry.get("counties");
                    for (String county : counties) {
                        rows.add(new DirectoryLocation(stateCode, county));
                    }
                }
                repository.saveAll(rows);
                cacheService.evict();
                log.info("Seeded {} directory location rows across {} states.", rows.size(), data.size());
            } catch (Exception ex) {
                log.error("Failed to seed directory locations: {}", ex.getMessage(), ex);
            }
        };
    }
}
