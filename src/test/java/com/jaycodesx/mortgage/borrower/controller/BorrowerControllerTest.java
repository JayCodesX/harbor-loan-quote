package com.jaycodesx.mortgage.borrower.controller;

import com.jaycodesx.mortgage.borrower.dto.BorrowerCreateRequestDto;
import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.service.BorrowerService;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerControllerTest {

    @Mock
    private BorrowerService borrowerService;
    @Mock
    private UserTokenAuthorizationService userTokenAuthorizationService;

    @InjectMocks
    private BorrowerController borrowerController;

    @Test
    void createBorrowerReturnsCreated() {
        Borrower borrower = new Borrower(1L, "Jay", "Lane", "jay@jaycodesx.dev", 740);
        when(borrowerService.createBorrower(new BorrowerCreateRequestDto("Jay", "Lane", "jay@jaycodesx.dev", 740)))
                .thenReturn(borrower);

        ResponseEntity<?> response = borrowerController.createBorrower(
                "Bearer user-token",
                new BorrowerCreateRequestDto("Jay", "Lane", "jay@jaycodesx.dev", 740)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(borrower);
    }

    @Test
    void listBorrowersReturnsProtectedResults() {
        when(borrowerService.findAll()).thenReturn(List.of(new Borrower(1L, "Jay", "Lane", "jay@jaycodesx.dev", 740)));

        ResponseEntity<?> response = borrowerController.listBorrowers("Bearer user-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getBorrowerByIdReturnsNotFound() {
        when(borrowerService.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = borrowerController.getBorrowerById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
