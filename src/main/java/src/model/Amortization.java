package src.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// Standard reducing-balance amortization: each month's interest is computed on the
// remaining principal, not on the original loan amount. Shared by Contract (to know the
// total cost up front) and PaymentSchedule (to generate the actual per-month breakdown),
// so the two can never disagree on what the loan actually costs.
public final class Amortization {

    private Amortization() {}

    public static final class Entry {
        private final BigDecimal payment;
        private final BigDecimal interestPortion;
        private final BigDecimal principalPortion;
        private final BigDecimal remainingBalance;

        Entry(BigDecimal payment, BigDecimal interestPortion, BigDecimal principalPortion, BigDecimal remainingBalance) {
            this.payment = payment;
            this.interestPortion = interestPortion;
            this.principalPortion = principalPortion;
            this.remainingBalance = remainingBalance;
        }

        public BigDecimal getPayment()          { return payment; }
        public BigDecimal getInterestPortion()  { return interestPortion; }
        public BigDecimal getPrincipalPortion() { return principalPortion; }
        public BigDecimal getRemainingBalance() { return remainingBalance; }
    }

    public static List<Entry> schedule(BigDecimal principal, double annualRate, int totalMonths) {
        double monthlyRate = annualRate / 12.0;
        BigDecimal monthlyPayment = computeMonthlyPayment(principal, monthlyRate, totalMonths);

        List<Entry> entries = new ArrayList<>();
        BigDecimal remaining = principal.setScale(2, RoundingMode.HALF_UP);

        for (int month = 1; month <= totalMonths; month++) {
            BigDecimal interestPortion = remaining.multiply(BigDecimal.valueOf(monthlyRate)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPortion;
            BigDecimal payment;

            if (month == totalMonths) {
                // Last month absorbs any rounding drift so the balance lands on exactly zero.
                principalPortion = remaining;
                payment = principalPortion.add(interestPortion);
            } else {
                principalPortion = monthlyPayment.subtract(interestPortion);
                payment = monthlyPayment;
            }

            remaining = remaining.subtract(principalPortion);
            entries.add(new Entry(payment, interestPortion, principalPortion, remaining));
        }

        return entries;
    }

    private static BigDecimal computeMonthlyPayment(BigDecimal principal, double monthlyRate, int totalMonths) {
        if (monthlyRate == 0) {
            return principal.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        }
        double factor = monthlyRate * Math.pow(1 + monthlyRate, totalMonths) / (Math.pow(1 + monthlyRate, totalMonths) - 1);
        return principal.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }
}
