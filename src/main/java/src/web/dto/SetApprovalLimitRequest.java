package src.web.dto;

import jakarta.validation.constraints.DecimalMin;

public record SetApprovalLimitRequest(@DecimalMin(value = "0.0", inclusive = false) double newAmount) {
}
