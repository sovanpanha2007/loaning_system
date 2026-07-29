package src.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Payment {
    private int monthId;
    private boolean isPaid;
    private BigDecimal amount;
    private BigDecimal interestPortion;
    private BigDecimal principalPortion;
    private BigDecimal remainingBalance;
    private LocalDate dueDate;
    private String paidDate;
    private boolean late;
    private BigDecimal lateFee;
    private BigDecimal amountPaid;

    public Payment(int monthId, BigDecimal amount, BigDecimal interestPortion, BigDecimal principalPortion,
                   BigDecimal remainingBalance, LocalDate dueDate){
        this.monthId=monthId;
        this.amount=amount.setScale(2, RoundingMode.HALF_UP);
        this.interestPortion=interestPortion.setScale(2, RoundingMode.HALF_UP);
        this.principalPortion=principalPortion.setScale(2, RoundingMode.HALF_UP);
        this.remainingBalance=remainingBalance.setScale(2, RoundingMode.HALF_UP);
        this.dueDate=dueDate;
        isPaid=false;
        this.paidDate=null;
        this.late=false;
        this.lateFee=BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.amountPaid=BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public int getMonthNumber() {
        return monthId;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getInterestPortion() {
        return interestPortion;
    }

    public BigDecimal getPrincipalPortion() {
        return principalPortion;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getPaidDate() {
        return paidDate;
    }

    public boolean isLate() {
        return late;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    // What the applicant actually owes for this month: the scheduled payment plus
    // whatever late fee has accrued from missing its due date.
    public BigDecimal getTotalDue() {
        return amount.add(lateFee);
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    // What's left to fully cover this month, after any partial payments already applied.
    public BigDecimal getRemainingDue() {
        return getTotalDue().subtract(amountPaid);
    }

    public void markLate(BigDecimal lateFee) {
        this.late = true;
        this.lateFee = lateFee.setScale(2, RoundingMode.HALF_UP);
    }

    // Applies a (possibly partial) payment toward this month. The caller — PaymentSchedule —
    // is responsible for never offering more than getRemainingDue(), since a single month's
    // payment can't reach into what's owed on a later month.
    public void applyPayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        if (amount.compareTo(getRemainingDue()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed the remaining amount due for this month");
        }
        this.amountPaid = amountPaid.add(amount).setScale(2, RoundingMode.HALF_UP);
        if (amountPaid.compareTo(getTotalDue()) >= 0) {
            isPaid = true;
            paidDate = LocalDate.now().toString();
        }
    }

    // Used by PaymentScheduleDao to restore paid/paidDate exactly as stored, instead of
    // re-stamping paidDate with today's date the way applyPayment does for a fresh payment.
    public void hydratePaidState(boolean isPaid, String paidDate){
        this.isPaid=isPaid;
        this.paidDate=paidDate;
    }

    // Used by PaymentScheduleDao to restore late/lateFee exactly as stored.
    public void hydrateLateState(boolean late, BigDecimal lateFee){
        this.late=late;
        this.lateFee=lateFee.setScale(2, RoundingMode.HALF_UP);
    }

    // Used by PaymentScheduleDao to restore partial-payment progress exactly as stored.
    public void hydrateAmountPaid(BigDecimal amountPaid){
        this.amountPaid=amountPaid.setScale(2, RoundingMode.HALF_UP);
    }

}
