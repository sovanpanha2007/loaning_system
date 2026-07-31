package src.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String name,
        @NotBlank String password,
        @NotBlank @Size(min = 4, message = "must be at least 4 characters") String newPassword) {
}
