package src.web.dto;

import java.math.BigDecimal;

import src.model.LoanOfficer;
import src.model.Manager;
import src.model.Staff;

public record StaffResponse(
        int id, String name, String username, String phoneNumber, int age,
        double salary, String position, boolean active,
        BigDecimal maxApprovalLimit, Integer accessLevel) {

    public static StaffResponse from(Staff s) {
        BigDecimal maxApprovalLimit = s instanceof LoanOfficer lo ? lo.getMaxApprovalLimit() : null;
        Integer accessLevel = s instanceof Manager m ? m.getAccessLevel() : null;
        return new StaffResponse(s.getId(), s.getName(), s.getUsername(), s.getPhoneNumber(), s.getAge(),
                s.getSalary(), s.getPosition(), s.isActive(), maxApprovalLimit, accessLevel);
    }
}
