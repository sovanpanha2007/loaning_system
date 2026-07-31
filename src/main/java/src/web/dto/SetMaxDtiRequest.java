package src.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record SetMaxDtiRequest(@DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0") double ratio) {
}
