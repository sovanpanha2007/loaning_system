package src.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record CreateContractRequest(
        @Min(1) int applicantId,
        @DecimalMin(value = "0.0", inclusive = false) double amount,
        @Min(1) int duration) {
}
