package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.pricing.model.RateChangeAudit;
import com.jaycodesx.mortgage.pricing.repository.RateChangeAuditRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateChangeAuditListenerTest {

    private static final LocalDateTime EFFECTIVE_AT = LocalDateTime.of(2026, 7, 9, 0, 0);
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 12, 31, 0, 0);

    @Mock
    RateChangeAuditRepository repository;

    @InjectMocks
    RateChangeAuditListener listener;

    private ConsumerRecord<String, RateSheetActivatedMessage> recordFor(long rateSheetId,
                                                                        int partition, long offset) {
        RateSheetActivatedMessage event = new RateSheetActivatedMessage(
                rateSheetId, "FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT);
        return new ConsumerRecord<>("pricing.rate-sheet.activated", partition, offset, "FANNIE_MAE", event);
    }

    @Test
    void newEvent_persistsAuditRowWithKafkaCoordinates() {
        when(repository.existsByRateSheetId(2L)).thenReturn(false);

        listener.onRateChange(recordFor(2L, 0, 7L));

        ArgumentCaptor<RateChangeAudit> captor = ArgumentCaptor.forClass(RateChangeAudit.class);
        verify(repository).save(captor.capture());
        RateChangeAudit saved = captor.getValue();
        assertThat(saved.getRateSheetId()).isEqualTo(2L);
        assertThat(saved.getInvestorId()).isEqualTo("FANNIE_MAE");
        assertThat(saved.getEffectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(saved.getKafkaPartition()).isEqualTo(0);
        assertThat(saved.getKafkaOffset()).isEqualTo(7L);
    }

    @Test
    void duplicateEvent_isSkipped_soReplayIsIdempotent() {
        when(repository.existsByRateSheetId(2L)).thenReturn(true);

        listener.onRateChange(recordFor(2L, 0, 8L));

        verify(repository, never()).save(any());
    }

    @Test
    void concurrentInsert_uniqueViolationIsSwallowed_notPropagated() {
        // exists-check passes, but a racing consumer inserts first: the unique key
        // throws on save. The listener must treat it as "already recorded", not fail.
        when(repository.existsByRateSheetId(2L)).thenReturn(false);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate rate_sheet_id"));

        assertThatCode(() -> listener.onRateChange(recordFor(2L, 0, 9L)))
                .doesNotThrowAnyException();
    }
}
