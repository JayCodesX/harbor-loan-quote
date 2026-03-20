package com.jaycodesx.mortgage.directory.dto;

import java.util.List;

public record LocationResponseDto(String stateCode, List<String> counties) {
}
