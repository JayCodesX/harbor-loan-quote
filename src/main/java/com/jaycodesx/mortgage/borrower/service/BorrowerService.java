package com.jaycodesx.mortgage.borrower.service;

import com.jaycodesx.mortgage.borrower.dto.BorrowerCreateRequestDto;
import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.repository.BorrowerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;

    public BorrowerService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    public Borrower createBorrower(BorrowerCreateRequestDto request) {
        Borrower borrower = new Borrower();
        borrower.setFirstName(request.firstName());
        borrower.setLastName(request.lastName());
        borrower.setEmail(request.email());
        borrower.setCreditScore(request.creditScore());
        return borrowerRepository.save(borrower);
    }

    public List<Borrower> findAll() {
        return borrowerRepository.findAll();
    }

    public Optional<Borrower> findById(Long id) {
        return borrowerRepository.findById(id);
    }

    public boolean borrowerExists(Long id) {
        return borrowerRepository.existsById(id);
    }
}
