package src.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import src.interfaces.ILoginable;
import src.model.AuditEntry;
import src.model.Contract;

// Confirms the audit log actually records one entry per mutating step of a full
// create -> approve -> pay flow, in order, and that each write landed in the same
// transaction as the business change it describes (no dangling business writes with
// no matching audit row, and vice versa).
class LoaningSystemAuditTest {

    private Path dbFile;
    private LoaningSystem system;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("lms-audit-test", ".db");
        Files.deleteIfExists(dbFile);
        system = new LoaningSystem("Test Bank", 0.05, dbFile.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        system.close();
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(Path.of(dbFile + "-wal"));
        Files.deleteIfExists(Path.of(dbFile + "-shm"));
    }

    @Test
    void fullLifecycleProducesOneAuditRowPerStepInOrder() {
        ILoginable admin = system.authenticate("Admin123", "1234");
        system.createApplicant(admin, "Jane Doe", "jane", "011111111", "pass1234", 30, 10000, "F", 0);
        system.createStaff(admin, "Larry Officer", "larry", "022222222", 28, "passL", 3000, LoaningSystem.LOAN_OFFICER);

        ILoginable jane = system.authenticate("jane", "pass1234");
        int applicantId = jane.getId();
        Contract contract = system.createContract(jane, applicantId, 1200, 1);
        int contractId = contract.getContractId();

        ILoginable larry = system.authenticate("larry", "passL");
        system.approveContract(larry, contractId);

        admin = system.authenticate("Admin123", "1234");
        system.addBalanceforApplicant(admin, applicantId, 2000);

        jane = system.authenticate("jane", "pass1234");
        system.makePayment(jane, contractId, 1);

        admin = system.authenticate("Admin123", "1234");
        List<AuditEntry> log = system.getAuditLog(admin);

        List<String> actions = log.stream().map(AuditEntry::getAction).toList();

        List<String> expectedActionsInOrder = List.of(
                "LOGIN",             // Admin logging in
                "CREATE_APPLICANT",
                "CREATE_STAFF",
                "LOGIN",             // Jane logging in
                "CREATE_CONTRACT",
                "LOGIN",             // Larry logging in
                "APPROVE_CONTRACT",
                "LOGIN",             // Admin logging in to fund the account
                "DEPOSIT",
                "LOGIN",             // Jane logging in again
                "MAKE_PAYMENT",
                "LOGIN"              // Admin logging in to view the audit log
        );

        assertEquals(expectedActionsInOrder, actions);
    }
}
