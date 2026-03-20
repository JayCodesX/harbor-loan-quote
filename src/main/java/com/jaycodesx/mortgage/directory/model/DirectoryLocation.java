package com.jaycodesx.mortgage.directory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "directory_locations", indexes = {
        @Index(name = "idx_directory_locations_state_code", columnList = "state_code")
})
public class DirectoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "county_name", nullable = false, length = 100)
    private String countyName;

    public DirectoryLocation() {
    }

    public DirectoryLocation(String stateCode, String countyName) {
        this.stateCode = stateCode;
        this.countyName = countyName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
}
