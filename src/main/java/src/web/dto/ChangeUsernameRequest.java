package src.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeUsernameRequest(@NotBlank String username, @NotBlank String password, @NotBlank String newUsername) {
}
