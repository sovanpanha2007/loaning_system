package src.web.dto;

import java.math.BigDecimal;

import src.model.Payment;

public record PaymentResponse(
        int monthNumber, BigDecimal amount, BigDecimal interestPortion, BigDecimal principalPortion,
        BigDecimal remainingBalance, String dueDate, boolean paid, String paidDate,
        boolean late, BigDecimal lateFee, BigDecimal amountPaid, BigDecimal remainingDue, BigDecimal totalDue) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getMonthNumber(), p.getAmount(), p.getInterestPortion(), p.getPrincipalPortion(),
                p.getRemainingBalance(), p.getDueDate().toString(), p.isPaid(), p.getPaidDate(),
                p.isLate(), p.getLateFee(), p.getAmountPaid(), p.getRemainingDue(), p.getTotalDue());
    }
}
