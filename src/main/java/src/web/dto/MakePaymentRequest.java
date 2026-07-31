package src.web.dto;

import jakarta.validation.constraints.DecimalMin;

public record MakePaymentRequest(@DecimalMin(value = "0.0", inclusive = false) double amount) {
}
