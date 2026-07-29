package src.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoaningSystemDelinquencyTest {

    private Path dbFile;
    private LoaningSystem system;
    private int applicantId;
    private int contractId;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("lms-delinquency-test", ".db");
        Files.deleteIfExists(dbFile);
        system = new LoaningSystem("Test Bank", 0.05, dbFile.toString());

        system.login("Admin123", "1234");
        system.createApplicant("Jane Doe", "jane", "011111111", "pass1234", 30, 10000, "F", 0);
        system.login("jane", "pass1234");
        applicantId = system.getLoggedInUser().getId();
        system.createContract(applicantId, 1200, 1);
        contractId = parseTrailingId(system.getLastMessage());

        system.login("Admin123", "1234");
        system.createStaff("Larry Officer", "larry", "022222222", 28, "passL", 3000, LoaningSystem.LOAN_OFFICER);
        system.login("larry", "passL");
        system.approveContract(contractId);
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
        system.login("Admin123", "1234");

        // Month 1's due date is ~1 month from contract creation; checking "as of" 2 months
        // out means it's unambiguously overdue.
        system.checkDelinquency(LocalDate.now().plusMonths(2));

        assertTrue(system.getLastMessage().contains("1 payment(s) newly flagged late"));

        String schedule = captureSchedule();
        assertTrue(schedule.contains("LATE +$25.00"), "expected month 1 to show the late fee:\n" + schedule);
    }

    @Test
    void threeConsecutiveMissedMonthsDefaultsTheContract() {
        system.login("Admin123", "1234");

        // Far enough out that months 1, 2, and 3 are all overdue at once.
        system.checkDelinquency(LocalDate.now().plusMonths(4));

        assertTrue(system.getLastMessage().contains("1 contract(s) defaulted"));

        String contracts = captureContracts();
        assertTrue(contracts.contains("Status: DEFAULTED"), "expected contract to show DEFAULTED:\n" + contracts);
    }

    @Test
    void payingOnTimeNeverGetsFlagged() {
        system.login("Admin123", "1234");

        system.checkDelinquency(LocalDate.now());

        assertEquals("Delinquency check complete: 0 payment(s) newly flagged late, 0 contract(s) defaulted.", system.getLastMessage());
    }

    private int parseTrailingId(String message) {
        Matcher m = Pattern.compile("(\\d+)$").matcher(message.trim());
        if (!m.find()) {
            throw new IllegalStateException("Could not find a trailing id in: " + message);
        }
        return Integer.parseInt(m.group(1));
    }

    private String captureSchedule() {
        return capture(() -> {
            system.login("jane", "pass1234");
            system.printMySchedule(contractId);
        });
    }

    private String captureContracts() {
        return capture(() -> system.printContracts());
    }

    private String capture(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return buffer.toString();
    }
}
