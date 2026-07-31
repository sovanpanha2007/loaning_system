package src.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import src.interfaces.ILoginable;
import src.model.Contract;
import src.model.Payment;
import src.model.PaymentSchedule;

class LoaningSystemDelinquencyTest {

    private Path dbFile;
    private LoaningSystem system;
    private ILoginable admin;
    private ILoginable jane;
    private int contractId;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("lms-delinquency-test", ".db");
        Files.deleteIfExists(dbFile);
        system = new LoaningSystem("Test Bank", 0.05, dbFile.toString());

        admin = system.authenticate("Admin123", "1234");
        system.createApplicant(admin, "Jane Doe", "jane", "011111111", "pass1234", 30, 10000, "F", 0);
        jane = system.authenticate("jane", "pass1234");
        Contract contract = system.createContract(jane, jane.getId(), 1200, 1);
        contractId = contract.getContractId();

        system.createStaff(admin, "Larry Officer", "larry", "022222222", 28, "passL", 3000, LoaningSystem.LOAN_OFFICER);
        ILoginable larry = system.authenticate("larry", "passL");
        system.approveContract(larry, contractId);
    }

    @AfterEach
    void tearDown() throws Exception {
        system.close();
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(Path.of(dbFile + "-wal"));
        Files.deleteIfExists(Path.of(dbFile + "-shm"));
    }

    @Test
    void overduePaymentGetsFlaggedLateWithFee() {
        // Month 1's due date is ~1 month from contract creation; checking "as of" 2 months
        // out means it's unambiguously overdue.
        LoaningSystem.DelinquencyResult result = system.checkDelinquency(admin, LocalDate.now().plusMonths(2));

        assertEquals(1, result.getFlaggedLate());
        assertEquals(0, result.getDefaulted());

        PaymentSchedule schedule = system.getMySchedule(jane, contractId);
        Payment month1 = schedule.getPayment(1);
        assertTrue(month1.isLate(), "expected month 1 to be flagged late");
        assertEquals(new BigDecimal("25.00"), month1.getLateFee());
    }

    @Test
    void threeConsecutiveMissedMonthsDefaultsTheContract() {
        // Far enough out that months 1, 2, and 3 are all overdue at once.
        LoaningSystem.DelinquencyResult result = system.checkDelinquency(admin, LocalDate.now().plusMonths(4));

        assertEquals(1, result.getDefaulted());

        Contract contract = system.getAllContracts().stream()
                .filter(c -> c.getContractId() == contractId)
                .findFirst()
                .orElseThrow();
        assertEquals(Contract.DEFAULTED, contract.getStatus());
    }

    @Test
    void payingOnTimeNeverGetsFlagged() {
        LoaningSystem.DelinquencyResult result = system.checkDelinquency(admin, LocalDate.now());

        assertEquals(0, result.getFlaggedLate());
        assertEquals(0, result.getDefaulted());
    }
}
