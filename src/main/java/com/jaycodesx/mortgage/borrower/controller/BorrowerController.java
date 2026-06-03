package com.jaycodesx.mortgage.borrower.controller;

import com.jaycodesx.mortgage.borrower.dto.BorrowerCreateRequestDto;
import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.service.BorrowerService;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final UserTokenAuthorizationService userTokenAuthorizationService;

    public BorrowerController(
            BorrowerService borrowerService,
            UserTokenAuthorizationService userTokenAuthorizationService
    ) {
        this.borrowerService = borrowerService;
        this.userTokenAuthorizationService = userTokenAuthorizationService;
    }

    @PostMapping
    public ResponseEntity<?> createBorrower(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody BorrowerCreateRequestDto request
    ) {
        try {
            userTokenAuthorizationService.requireAuthenticatedUser(authorizationHeader);
            Borrower createdBorrower = borrowerService.createBorrower(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBorrower);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listBorrowers(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            userTokenAuthorizationService.requireAuthenticatedUser(authorizationHeader);
            return ResponseEntity.ok(borrowerService.findAll());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBorrowerById(@PathVariable Long id) {
        return borrowerService.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
