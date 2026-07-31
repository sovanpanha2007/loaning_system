package src.web.dto;

import java.math.BigDecimal;

import src.model.Applicant;

public record ApplicantResponse(
        int id, String name, String username, String phoneNumber, String gender,
        BigDecimal salary, int age, BigDecimal existingExternalDebt, BigDecimal accountBalance, boolean active) {

    public static ApplicantResponse from(Applicant a) {
        return new ApplicantResponse(a.getId(), a.getName(), a.getUsername(), a.getPhoneNumber(), a.getGender(),
                a.getSalary(), a.getAge(), a.getExistingExternalDebt(), a.getBalance(), a.isActive());
    }
}
