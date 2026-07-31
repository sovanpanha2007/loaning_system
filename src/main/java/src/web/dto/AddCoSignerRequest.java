package src.web.dto;

import jakarta.validation.constraints.Min;

public record AddCoSignerRequest(@Min(1) int staffId) {
}
