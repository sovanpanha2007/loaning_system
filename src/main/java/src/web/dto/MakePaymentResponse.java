package src.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record MakePaymentResponse(BigDecimal amountApplied, List<Integer> monthsTouched, BigDecimal remainingBalance) {
}
