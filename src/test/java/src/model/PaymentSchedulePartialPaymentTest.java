package src.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

// Principal 1200 / 5% / 1 year: months 1 and 2 both owe 102.73, whole schedule costs 1232.73.
class PaymentSchedulePartialPaymentTest {

    private PaymentSchedule schedule() {
        Applicant applicant = new Applicant("Jane Doe", "jane", "0123456789", "pass1234",
                "F", BigDecimal.valueOf(10000), 30, BigDecimal.ZERO);
        Contract contract = new Contract(applicant, BigDecimal.valueOf(1200), 1, 0.05);
        return new PaymentSchedule(contract);
    }

    @Test
    void partialPaymentLeavesMonthUnpaidWithReducedRemainingDue() {
        PaymentSchedule schedule = schedule();

        PaymentSchedule.PaymentResult result = schedule.applyPayment(BigDecimal.valueOf(50));

        assertEquals(new BigDecimal("50.00"), result.getAmountApplied());
        assertEquals(List.of(1), result.getMonthsTouched());
        assertFalse(schedule.getPayment(1).isPaid());
        assertEquals(new BigDecimal("52.73"), schedule.getPayment(1).getRemainingDue());
    }

    @Test
    void repeatedPartialPaymentsEventuallyPayOffTheMonth() {
        PaymentSchedule schedule = schedule();

        schedule.applyPayment(BigDecimal.valueOf(50));
        assertFalse(schedule.getPayment(1).isPaid());

        PaymentSchedule.PaymentResult second = schedule.applyPayment(new BigDecimal("52.73"));

        assertEquals(new BigDecimal("52.73"), second.getAmountApplied());
        assertEquals(List.of(1), second.getMonthsTouched());
        assertTrue(schedule.getPayment(1).isPaid());
        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getPayment(1).getRemainingDue());
    }

    @Test
    void overpaymentRollsForwardIntoTheNextMonth() {
        PaymentSchedule schedule = schedule();

        PaymentSchedule.PaymentResult result = schedule.applyPayment(BigDecimal.valueOf(150));

        assertEquals(new BigDecimal("150.00"), result.getAmountApplied());
        assertEquals(List.of(1, 2), result.getMonthsTouched());
        assertTrue(schedule.getPayment(1).isPaid());
        assertFalse(schedule.getPayment(2).isPaid());
        // 150 - 102.73 (month 1) = 47.27 rolled into month 2, leaving 102.73 - 47.27 = 55.46 due.
        assertEquals(new BigDecimal("55.46"), schedule.getPayment(2).getRemainingDue());
    }

    @Test
    void largeOverpaymentPaysOffTheWholeScheduleWithoutLosingTheLeftover() {
        PaymentSchedule schedule = schedule();

        PaymentSchedule.PaymentResult result = schedule.applyPayment(BigDecimal.valueOf(2000));

        // Only what was actually owed (1232.73) gets applied — the remaining 767.27 the
        // borrower offered is never silently absorbed; it's the caller's job to leave it
        // in the applicant's balance.
        assertEquals(new BigDecimal("1232.73"), result.getAmountApplied());
        assertTrue(result.getAmountApplied().compareTo(BigDecimal.valueOf(2000)) < 0);
        assertTrue(schedule.isFullyPaid());
        assertEquals(12, result.getMonthsTouched().size());
    }
}
