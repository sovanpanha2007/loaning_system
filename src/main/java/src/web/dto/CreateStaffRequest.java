package src.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z ]+$", message = "must contain only letters") String name,
        @NotBlank String username,
        @NotBlank @Pattern(regexp = "^0[0-9]{8,9}$", message = "must be 9-10 digits starting with 0") String phoneNumber,
        @Min(18) @Max(65) int age,
        @NotBlank @Size(min = 4, message = "must be at least 4 characters") String password,
        @DecimalMin(value = "0.0", inclusive = false) double salary,
        @NotBlank String position) {
}
