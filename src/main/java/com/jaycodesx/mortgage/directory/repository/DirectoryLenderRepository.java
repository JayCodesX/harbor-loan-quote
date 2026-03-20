package com.jaycodesx.mortgage.directory.repository;

import com.jaycodesx.mortgage.directory.model.DirectoryLender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectoryLenderRepository extends JpaRepository<DirectoryLender, Long> {

    List<DirectoryLender> findByStateCodeAndCountyName(String stateCode, String countyName);

    Page<DirectoryLender> findByStateCodeAndCountyName(String stateCode, String countyName, Pageable pageable);
}
