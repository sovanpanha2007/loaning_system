package src.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ContractMoneyTest {

    private Applicant applicant() {
        return new Applicant("John Doe", "john", "0123456789", "pass1234",
                "M", BigDecimal.valueOf(2000), 25, BigDecimal.ZERO);
    }

    @Test
    void totalAmountReflectsReducingBalanceInterestWithNoFloatDrift() {
        Contract contract = new Contract(applicant(), BigDecimal.valueOf(500), 1, 0.05);

        assertEquals(new BigDecimal("500.00"), contract.getPrincipalAmount());
        // Interest accrues on the shrinking balance each month, not on the full
        // principal for the whole year, so total cost is well under 500 * 1.05.
        assertEquals(new BigDecimal("513.63"), contract.getTotalAmount());
    }

    @Test
    void effectiveAprIsMeaningfullyLowerThanNominalRateForAReducingBalanceLoan() {
        Contract contract = new Contract(applicant(), BigDecimal.valueOf(500), 1, 0.05);

        // The borrower only owes the full $500 for the first month, not the whole year, so
        // total interest paid (13.63) annualized against principal is well under the 5%
        // nominal rate — this is exactly the gap the effective APR disclosure exists to show.
        assertEquals(new BigDecimal("0.0273"), contract.getEffectiveApr());
    }

    @Test
    void reducingBalanceScheduleEndsAtExactlyZero() {
        Contract contract = new Contract(applicant(), BigDecimal.valueOf(1200), 1, 0.12);
        PaymentSchedule schedule = new PaymentSchedule(contract);

        assertEquals(new BigDecimal("106.62"), schedule.getPaymentAmount(1));
        assertEquals(new BigDecimal("106.60"), schedule.getPaymentAmount(12));
        assertEquals(new BigDecimal("1279.42"), contract.getTotalAmount());
    }
}
