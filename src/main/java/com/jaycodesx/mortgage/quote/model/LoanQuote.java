package com.jaycodesx.mortgage.quote.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_quotes")
public class LoanQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(length = 128)
    private String userId;

    @Column(length = 64)
    private String sessionId;

    @Column(length = 128)
    private String requestFingerprint;

    @Column(length = 20)
    private String processingStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal homePrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal downPayment;

    @Column(precision = 19, scale = 2)
    private BigDecimal financedAmount;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 40)
    private String loanProgram;

    @Column(nullable = false, length = 40)
    private String propertyUse;

    @Column(nullable = false)
    private Integer termYears;

    @Column(precision = 7, scale = 4)
    private BigDecimal estimatedRate;

    @Column(precision = 7, scale = 4)
    private BigDecimal estimatedApr;

    @Column(precision = 19, scale = 2)
    private BigDecimal estimatedMonthlyPayment;

    @Column(precision = 19, scale = 2)
    private BigDecimal estimatedCashToClose;

    @Column(length = 40)
    private String qualificationTier;

    @Column(nullable = false, length = 20)
    private String quoteStage;

    @Column(nullable = false, length = 20)
    private String quoteStatus;

    @Column(nullable = false)
    private boolean leadCaptured;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public BigDecimal getHomePrice() { return homePrice; }
    public void setHomePrice(BigDecimal homePrice) { this.homePrice = homePrice; }
    public BigDecimal getDownPayment() { return downPayment; }
    public void setDownPayment(BigDecimal downPayment) { this.downPayment = downPayment; }
    public BigDecimal getFinancedAmount() { return financedAmount; }
    public void setFinancedAmount(BigDecimal financedAmount) { this.financedAmount = financedAmount; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getLoanProgram() { return loanProgram; }
    public void setLoanProgram(String loanProgram) { this.loanProgram = loanProgram; }
    public String getPropertyUse() { return propertyUse; }
    public void setPropertyUse(String propertyUse) { this.propertyUse = propertyUse; }
    public Integer getTermYears() { return termYears; }
    public void setTermYears(Integer termYears) { this.termYears = termYears; }
    public BigDecimal getEstimatedRate() { return estimatedRate; }
    public void setEstimatedRate(BigDecimal estimatedRate) { this.estimatedRate = estimatedRate; }
    public BigDecimal getEstimatedApr() { return estimatedApr; }
    public void setEstimatedApr(BigDecimal estimatedApr) { this.estimatedApr = estimatedApr; }
    public BigDecimal getEstimatedMonthlyPayment() { return estimatedMonthlyPayment; }
    public void setEstimatedMonthlyPayment(BigDecimal estimatedMonthlyPayment) { this.estimatedMonthlyPayment = estimatedMonthlyPayment; }
    public BigDecimal getEstimatedCashToClose() { return estimatedCashToClose; }
    public void setEstimatedCashToClose(BigDecimal estimatedCashToClose) { this.estimatedCashToClose = estimatedCashToClose; }
    public String getQualificationTier() { return qualificationTier; }
    public void setQualificationTier(String qualificationTier) { this.qualificationTier = qualificationTier; }
    public String getQuoteStage() { return quoteStage; }
    public void setQuoteStage(String quoteStage) { this.quoteStage = quoteStage; }
    public String getQuoteStatus() { return quoteStatus; }
    public void setQuoteStatus(String quoteStatus) { this.quoteStatus = quoteStatus; }
    public boolean isLeadCaptured() { return leadCaptured; }
    public void setLeadCaptured(boolean leadCaptured) { this.leadCaptured = leadCaptured; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
