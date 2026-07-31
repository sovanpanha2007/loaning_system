package src.web.dto;

import jakarta.validation.constraints.Min;

public record AddBalanceRequest(@Min(1) int amount) {
}
