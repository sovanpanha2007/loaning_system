package src.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ApplicantDtiTest {

    private static final double MAX_DTI = 0.40;

    private Applicant applicant(int salary, int existingExternalDebt) {
        return new Applicant("Jane Doe", "jane", "0123456789", "pass1234",
                "F", BigDecimal.valueOf(salary), 30, BigDecimal.valueOf(existingExternalDebt));
    }

    @Test
    void underThresholdWithNoExternalDebtCanBorrow() {
        // salary 10000, cap = 4000; requesting 3999 keeps total debt just under the cap.
        Applicant applicant = applicant(10000, 0);
        assertTrue(applicant.canBorrow(BigDecimal.ZERO, BigDecimal.valueOf(3999), MAX_DTI));
    }

    @Test
    void atThresholdIsRejected() {
        // salary 10000, cap = 4000; requesting exactly 4000 hits the cap, not under it.
        Applicant applicant = applicant(10000, 0);
        assertFalse(applicant.canBorrow(BigDecimal.ZERO, BigDecimal.valueOf(4000), MAX_DTI));
    }

    @Test
    void overThresholdIsRejected() {
        Applicant applicant = applicant(10000, 0);
        assertFalse(applicant.canBorrow(BigDecimal.ZERO, BigDecimal.valueOf(4001), MAX_DTI));
    }

    @Test
    void existingExternalDebtReducesRoomToBorrowFromThisBank() {
        // salary 10000, cap = 4000; 2500 already owed elsewhere leaves only 1500 of headroom.
        Applicant applicant = applicant(10000, 2500);
        assertTrue(applicant.canBorrow(BigDecimal.ZERO, BigDecimal.valueOf(1499), MAX_DTI));
        assertFalse(applicant.canBorrow(BigDecimal.ZERO, BigDecimal.valueOf(1500), MAX_DTI));
    }

    @Test
    void existingExternalDebtAloneCanAlreadyExceedTheCap() {
        // salary 10000, cap = 4000; 5000 of external debt alone already blows the cap,
        // so this bank should refuse to lend anything further.
        Applicant applicant = applicant(10000, 5000);
        assertFalse(applicant.canBorrow(BigDecimal.ZERO, BigDecimal.valueOf(1), MAX_DTI));
    }
}
