package com.jaycodesx.mortgage.borrower.repository;

import com.jaycodesx.mortgage.borrower.model.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerRepository extends JpaRepository<Borrower, Long> {
}
