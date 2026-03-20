package com.jaycodesx.mortgage.directory.repository;

import com.jaycodesx.mortgage.directory.model.DirectoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DirectoryLocationRepository extends JpaRepository<DirectoryLocation, Long> {

    @Query("SELECT DISTINCT d.stateCode FROM DirectoryLocation d ORDER BY d.stateCode ASC")
    List<String> findDistinctStateCodes();

    List<DirectoryLocation> findByStateCodeOrderByCountyNameAsc(String stateCode);
}
