package com.jaycodesx.mortgage.borrower.service;

import com.jaycodesx.mortgage.borrower.dto.BorrowerCreateRequestDto;
import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.repository.BorrowerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private BorrowerService borrowerService;

    @Test
    void createBorrowerMapsRequestAndSaves() {
        Borrower saved = new Borrower(1L, "Jay", "Stone", "jay@jaycodesx.dev", 740);
        when(borrowerRepository.save(any(Borrower.class))).thenReturn(saved);

        Borrower result = borrowerService.createBorrower(
                new BorrowerCreateRequestDto("Jay", "Stone", "jay@jaycodesx.dev", 740)
        );

        ArgumentCaptor<Borrower> captor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("jay@jaycodesx.dev");
        assertThat(result.getId()).isEqualTo(1L);
    }
}
