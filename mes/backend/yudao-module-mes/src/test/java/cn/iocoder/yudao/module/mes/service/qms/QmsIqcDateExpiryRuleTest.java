package cn.iocoder.yudao.module.mes.service.qms;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QmsIqcDateExpiryRuleTest {

    private static final LocalDate EVALUATION_DATE = LocalDate.of(2026, 7, 29);

    @Test
    void shouldKeepBoundaryDateOk() {
        assertFalse(QmsIqcServiceImpl.isDateExpired(
                LocalDate.of(2026, 7, 22), EVALUATION_DATE, 7));
    }

    @Test
    void shouldMarkDateNgAfterExpiryDays() {
        assertTrue(QmsIqcServiceImpl.isDateExpired(
                LocalDate.of(2026, 7, 21), EVALUATION_DATE, 7));
    }

    @Test
    void shouldAllowOnlyEvaluationDateWhenExpiryDaysIsZero() {
        assertFalse(QmsIqcServiceImpl.isDateExpired(
                EVALUATION_DATE, EVALUATION_DATE, 0));
        assertTrue(QmsIqcServiceImpl.isDateExpired(
                EVALUATION_DATE.minusDays(1), EVALUATION_DATE, 0));
    }
}
