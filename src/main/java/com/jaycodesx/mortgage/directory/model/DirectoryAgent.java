package com.jaycodesx.mortgage.directory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(
    name = "directory_agents",
    indexes = {
        @Index(name = "idx_directory_agents_state_county", columnList = "state_code, county_name")
    }
)
public class DirectoryAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "county_name", nullable = false, length = 100)
    private String countyName;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "license_number", nullable = false, length = 24)
    private String licenseNumber;

    @Column(nullable = false, length = 50)
    private String specialty;

    @Column(name = "avg_response_hours", nullable = false)
    private Integer avgResponseHours;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "ranking_score")
    private Integer rankingScore;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean active = true;

    @Column(name = "nmls_id", length = 20)
    private String nmlsId;

    @Column(length = 100)
    private String languages;

    @Column(name = "website_url", length = 200)
    private String websiteUrl;

    @Column(length = 100)
    private String city;

    public Long getId() { return id; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public Integer getAvgResponseHours() { return avgResponseHours; }
    public void setAvgResponseHours(Integer avgResponseHours) { this.avgResponseHours = avgResponseHours; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Integer getRankingScore() { return rankingScore; }
    public void setRankingScore(Integer rankingScore) { this.rankingScore = rankingScore; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNmlsId() { return nmlsId; }
    public void setNmlsId(String nmlsId) { this.nmlsId = nmlsId; }
    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
