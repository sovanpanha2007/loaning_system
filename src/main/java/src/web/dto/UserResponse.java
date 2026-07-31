package src.web.dto;

import src.interfaces.ILoginable;
import src.model.Applicant;
import src.model.CreditCommittee;
import src.model.LoanOfficer;
import src.model.Manager;

public record UserResponse(int id, String name, String username, String role) {

    public static UserResponse from(ILoginable user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), role(user));
    }

    private static String role(ILoginable user) {
        if (user instanceof Manager) return "MANAGER";
        if (user instanceof LoanOfficer) return "LOAN_OFFICER";
        if (user instanceof CreditCommittee) return "CREDIT_COMMITTEE";
        if (user instanceof Applicant) return "APPLICANT";
        return "UNKNOWN";
    }
}
