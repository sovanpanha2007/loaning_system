package src.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateApplicantRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z ]+$", message = "must contain only letters") String name,
        @NotBlank String username,
        @NotBlank @Pattern(regexp = "^0[0-9]{8,9}$", message = "must be 9-10 digits starting with 0") String phoneNumber,
        @NotBlank @Size(min = 4, message = "must be at least 4 characters") String password,
        @Min(18) @Max(65) int age,
        @Min(0) int income,
        @Pattern(regexp = "^[MF]$", message = "must be M or F") String gender,
        @DecimalMin(value = "0.0", message = "cannot be negative") double existingExternalDebt) {
}
