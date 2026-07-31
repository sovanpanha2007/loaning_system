package src.web.dto;

import src.controller.LoaningSystem;

public record DelinquencyResponse(int flaggedLate, int defaulted) {

    public static DelinquencyResponse from(LoaningSystem.DelinquencyResult result) {
        return new DelinquencyResponse(result.getFlaggedLate(), result.getDefaulted());
    }
}
