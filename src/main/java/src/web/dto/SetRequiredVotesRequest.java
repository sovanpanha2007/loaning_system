package src.web.dto;

import jakarta.validation.constraints.Min;

public record SetRequiredVotesRequest(@Min(1) int votes) {
}
