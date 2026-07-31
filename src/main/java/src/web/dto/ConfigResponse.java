package src.web.dto;

import src.controller.LoaningSystem;

public record ConfigResponse(String bankName, double interestRate, int requiredCommitteeVotes, double maxDebtToIncomeRatio) {

    public static ConfigResponse from(LoaningSystem system) {
        return new ConfigResponse(system.getBankName(), system.getCurrentInterestRate(),
                system.getRequiredCommitteeVotes(), system.getMaxDebtToIncomeRatio());
    }
}
