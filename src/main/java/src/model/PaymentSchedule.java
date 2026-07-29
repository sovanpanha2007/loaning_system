package src.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentSchedule {

    private int scheduleId;
    private Contract contract;
    private ArrayList<Payment> payments;

    public PaymentSchedule(Contract contract) {
        this.contract = contract;
        this.payments = new ArrayList<>();
        generatePayments();
    }

    // Assigned by PaymentScheduleDao once the row is inserted (or when hydrating from an
    // existing row) — ids are database-generated now, not a static in-process counter.
    public void setId(int id){
        this.scheduleId=id;
    }

    // Used by PaymentScheduleDao to hand back the rows it already persisted when
    // hydrating a schedule from the database, instead of regenerating the amortization table.
    public List<Payment> getPayments(){
        return payments;
    }

    public void setPayments(List<Payment> payments){
        this.payments = new ArrayList<>(payments);
    }

    private void generatePayments() {
        int totalMonths = contract.getDuration() * 12;
        LocalDate anchor = LocalDate.now();
        List<Amortization.Entry> entries = Amortization.schedule(contract.getPrincipalAmount(), contract.getInterestRate(), totalMonths);

        for (int i = 0; i < entries.size(); i++) {
            Amortization.Entry entry = entries.get(i);
            int monthNumber = i + 1;
            payments.add(new Payment(monthNumber, entry.getPayment(), entry.getInterestPortion(),
                    entry.getPrincipalPortion(), entry.getRemainingBalance(), anchor.plusMonths(monthNumber)));
        }
    }

    public Payment getPayment(int monthNumber){
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).getMonthNumber() == monthNumber) {
                return payments.get(i);
            }
        }
        return null;
    }

    public BigDecimal getPaymentAmount(int monthNumber){
        Payment payment = getPayment(monthNumber);
        return payment == null ? null : payment.getTotalDue();
    }

    public Payment getNextUnpaidPayment() {
        for (Payment p : payments) {
            if (!p.isPaid()) return p;
        }
        return null;
    }

    // The outcome of applyPayment: how much of the offered amount actually got used
    // (never more than what was owed across the schedule) and which months it touched.
    public static final class PaymentResult {
        private final BigDecimal amountApplied;
        private final List<Integer> monthsTouched;

        PaymentResult(BigDecimal amountApplied, List<Integer> monthsTouched) {
            this.amountApplied = amountApplied;
            this.monthsTouched = monthsTouched;
        }

        public BigDecimal getAmountApplied() { return amountApplied; }
        public List<Integer> getMonthsTouched() { return monthsTouched; }
    }

    // Applies a payment starting at the oldest unpaid month, rolling any leftover forward
    // into subsequent months. Stops as soon as a month isn't fully covered, so payments can
    // never skip ahead while an earlier month is still open. If the amount exceeds everything
    // left owed on the whole schedule, the excess is simply never applied (amountApplied will
    // be less than what was offered) — the caller is responsible for not losing that leftover.
    public PaymentResult applyPayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        BigDecimal remainingToApply = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalApplied = BigDecimal.ZERO;
        List<Integer> touched = new ArrayList<>();

        for (Payment p : payments) {
            if (remainingToApply.compareTo(BigDecimal.ZERO) <= 0) break;
            if (p.isPaid()) continue;

            BigDecimal toApply = remainingToApply.min(p.getRemainingDue());
            if (toApply.compareTo(BigDecimal.ZERO) <= 0) continue;

            p.applyPayment(toApply);
            totalApplied = totalApplied.add(toApply);
            remainingToApply = remainingToApply.subtract(toApply);
            touched.add(p.getMonthNumber());

            if (!p.isPaid()) break; // this month wasn't fully covered — can't roll further
        }

        return new PaymentResult(totalApplied.setScale(2, RoundingMode.HALF_UP), touched);
    }

    // Marks every unpaid, past-due payment as LATE and applies the flat fee, returning
    // the ones that just flipped so the caller can persist and report them.
    public List<Payment> checkForOverduePayments(LocalDate asOf, BigDecimal lateFee) {
        List<Payment> newlyLate = new ArrayList<>();
        for (Payment p : payments) {
            if (!p.isPaid() && !p.isLate() && p.getDueDate().isBefore(asOf)) {
                p.markLate(lateFee);
                newlyLate.add(p);
            }
        }
        return newlyLate;
    }

    // Payments are required to be paid in order, so unpaid+late payments always form a
    // contiguous run starting right after the last paid month — this is simply its length.
    public int getConsecutiveLateCount() {
        int count = 0;
        for (Payment p : payments) {
            if (p.isLate()) count++;
        }
        return count;
    }

    public void printSchedule() {
        System.out.println("\n===== Payment Schedule #" + scheduleId + " =====");
        System.out.println("Contract ID : " + contract.getContractId());
        System.out.println("Applicant   : " + contract.getApplicant().getName());
        System.out.println("Total Amount: $" + String.format("%.2f", contract.getTotalAmount()));
        System.out.println("------------------------------------------");
        System.out.printf("%-8s %-12s %-10s %-10s %-10s %-10s %-8s%n", "Month", "Due", "Interest", "Principal", "Balance", "Owed", "Status");
        for (int i =getPaidCount() ; i < payments.size(); i++) {
            Payment p = payments.get(i);
            String status = p.isLate() ? "LATE +$" + p.getLateFee() : (p.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "");
            System.out.printf("%-8d %-12s $%-9s $%-9s $%-9s $%-9s %-8s%n", p.getMonthNumber(), p.getDueDate(),
                    p.getInterestPortion(), p.getPrincipalPortion(), p.getRemainingBalance(), p.getRemainingDue(), status);
        }
        System.out.println("------------------------------------------");
        System.out.println("Paid    : " + getPaidCount() + " months");
        System.out.println("Remaining: " + getRemainingCount() + " months");
        System.out.println("==========================================");
    }

    public int getPaidCount() {
        int count = 0;
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).isPaid()) count++;
        }
        return count;
    }

    public int getRemainingCount() {
        return payments.size() - getPaidCount();
    }

    public boolean isFullyPaid() {
        return getRemainingCount() == 0;
    }

    public int getScheduleId()      { return scheduleId; }
    public Contract getContract()   { return contract; }
}