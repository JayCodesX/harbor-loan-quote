package com.jaycodesx.mortgage.directory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaycodesx.mortgage.directory.dto.LocationResponseDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class DirectoryCacheService {

    private static final String LOCATIONS_KEY = "directory:locations";
    private static final Duration LOCATIONS_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DirectoryCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<LocationResponseDto>> getLocations() {
        String payload = redisTemplate.opsForValue().get(LOCATIONS_KEY);
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, new TypeReference<List<LocationResponseDto>>() {}));
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(LOCATIONS_KEY);
            return Optional.empty();
        }
    }

    public void cacheLocations(List<LocationResponseDto> locations) {
        try {
            redisTemplate.opsForValue().set(LOCATIONS_KEY, objectMapper.writeValueAsString(locations), LOCATIONS_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize directory locations for cache", ex);
        }
    }

    public void evict() {
        redisTemplate.delete(LOCATIONS_KEY);
    }
}
