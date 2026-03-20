package com.jaycodesx.mortgage.directory.repository;

import com.jaycodesx.mortgage.directory.model.DirectoryAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectoryAgentRepository extends JpaRepository<DirectoryAgent, Long> {

    List<DirectoryAgent> findByStateCodeAndCountyName(String stateCode, String countyName);

    Page<DirectoryAgent> findByStateCodeAndCountyName(String stateCode, String countyName, Pageable pageable);
}
