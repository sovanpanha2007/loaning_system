package src.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import src.interfaces.ILoginable;
import src.model.Contract;

// Proves LoaningSystem's writeLock actually closes the check-then-write races it exists
// for: without it, concurrent threads can each pass a business-rule check before any of
// them has written, letting the borrowing cap be exceeded or a payment be deducted twice.
// Since LoaningSystem is now a stateless, thread-safe service (no more per-caller session
// field), these tests call it exactly the way concurrent web requests sharing one singleton
// instance would.
class LoaningSystemConcurrencyTest {

    private Path dbFile;
    private LoaningSystem system;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("lms-concurrency-test", ".db");
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
    void concurrentCreateContractNeverLetsTotalBorrowedExceedTheCap() throws InterruptedException {
        ILoginable admin = system.authenticate("Admin123", "1234");
        system.createApplicant(admin, "Jane Doe", "jane", "011111111", "pass1234", 30, 10000, "F", 0);
        ILoginable jane = system.authenticate("jane", "pass1234");
        int applicantId = jane.getId();

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        List<Boolean> outcomes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    system.createContract(jane, applicantId, 600, 1);
                    outcomes.add(true);
                } catch (IllegalArgumentException e) {
                    outcomes.add(false);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // Applicant's max borrowable is 10000 * 0.40 = 4000; at $600/contract, exactly 6 can fit
        // (3600 < 4000) and the 7th would push it to 4200 >= 4000. That result only holds if
        // the check-then-write sequence is serialized — otherwise more than 6 could slip through.
        long successes = outcomes.stream().filter(Boolean::booleanValue).count();
        assertEquals(6, successes);
    }

    @Test
    void concurrentPayoffAttemptsNeverDeductMoreThanWhatWasOwed() throws Exception {
        ILoginable admin = system.authenticate("Admin123", "1234");
        system.createApplicant(admin, "Jane Doe", "jane", "011111111", "pass1234", 30, 10000, "F", 0);
        ILoginable jane = system.authenticate("jane", "pass1234");
        int applicantId = jane.getId();

        Contract contract = system.createContract(jane, applicantId, 1200, 1);
        int contractId = contract.getContractId();

        system.createStaff(admin, "Larry Officer", "larry", "022222222", 28, "passL", 3000, LoaningSystem.LOAN_OFFICER);
        ILoginable larry = system.authenticate("larry", "passL");
        system.approveContract(larry, contractId);

        system.addBalanceforApplicant(admin, applicantId, 2000);

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    // Each thread offers more than the whole loan (1232.73 total) costs, all at
                    // once. Only the first to actually land should have its payment applied —
                    // everyone else must see the contract already fully paid and change nothing.
                    system.makePayment(jane, contractId, 1300);
                } catch (IllegalArgumentException ignored) {
                    // Expected for every thread but the one that actually lands the payoff.
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // Deposited 2000.00; the loan's full payoff cost is 1232.73 (principal 1200 + interest,
        // reducing-balance amortized over 12 months at 5%). If the lock didn't serialize
        // makePayment, more than one of the 8 threads could have applied a payoff concurrently,
        // deducting far more than the loan actually cost.
        assertEquals(new BigDecimal("767.27"), system.getMyBalance(jane));
    }
}
