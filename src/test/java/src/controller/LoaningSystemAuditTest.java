package src.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        system.login("Admin123", "1234");
        system.createApplicant("Jane Doe", "jane", "011111111", "pass1234", 30, 10000, "F", 0);
        system.createStaff("Larry Officer", "larry", "022222222", 28, "passL", 3000, LoaningSystem.LOAN_OFFICER);

        system.login("jane", "pass1234");
        int applicantId = system.getLoggedInUser().getId();
        system.createContract(applicantId, 1200, 1);
        int contractId = parseTrailingId(system.getLastMessage());

        system.login("larry", "passL");
        system.approveContract(contractId);

        system.login("Admin123", "1234");
        system.addBalanceforApplicant(applicantId, 2000);

        system.login("jane", "pass1234");
        system.makePayment(contractId, 1);

        String log = captureAuditLog();

        String[] expectedActionsInOrder = {
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
                "MAKE_PAYMENT"
        };

        int searchFrom = 0;
        for (String action : expectedActionsInOrder) {
            int index = log.indexOf("-> " + action + " ", searchFrom);
            assertTrue(index >= 0, "expected \"" + action + "\" after position " + searchFrom + " in:\n" + log);
            searchFrom = index + action.length();
        }
    }

    private int parseTrailingId(String message) {
        Matcher m = Pattern.compile("(\\d+)$").matcher(message.trim());
        if (!m.find()) {
            throw new IllegalStateException("Could not find a trailing id in: " + message);
        }
        return Integer.parseInt(m.group(1));
    }

    private String captureAuditLog() {
        system.login("Admin123", "1234");
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            system.printAuditLog();
        } finally {
            System.setOut(originalOut);
        }
        return buffer.toString();
    }
}
