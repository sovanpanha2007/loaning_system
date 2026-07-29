package src.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import src.controller.LoaningSystem;

public class LoanOfficer extends Staff {
   private BigDecimal maxApprovalLimit;

  public LoanOfficer(String name ,String userName , String phoneNumber, int age ,String password, double salary, BigDecimal maxApprovalLimit){
     super(name,userName,phoneNumber ,age, password);
     setSalary(salary);
     setPosition(LoaningSystem.LOAN_OFFICER);
     setMaxApprovalLimit(maxApprovalLimit);
  }
public BigDecimal getMaxApprovalLimit() {
        return maxApprovalLimit;
    }

public void setMaxApprovalLimit(BigDecimal maxApprovalLimit) {
    if(maxApprovalLimit == null || maxApprovalLimit.compareTo(BigDecimal.ZERO) <= 0){
        System.out.println("Error: max approval limit must be greater than 0.");
        return;
    }
    this.maxApprovalLimit = maxApprovalLimit.setScale(2, RoundingMode.HALF_UP);
  }



private boolean checkLoanOfficerApprovalLimit(BigDecimal amount){
    return amount.compareTo(maxApprovalLimit) <= 0;
}

private boolean checkApplicantSalaryAndAge(BigDecimal amount, Applicant applicant, double maxDebtToIncomeRatio){
    return applicant.canBorrow(BigDecimal.ZERO, amount, maxDebtToIncomeRatio) && applicant.getAge() >= 18;
}





@Override
    public boolean can(String action) {
        switch (action) {
            case LoaningSystem.CREATE_CONTRACT: return true;
            case LoaningSystem.APPROVE_LOAN:    return true;
            case LoaningSystem.REJECT_LOAN:     return true;
            case LoaningSystem.ADD_COSIGNER:    return true;
            default: return super.can(action);
        }
    }

    @Override
    public String toString() {
        return super.toString() +
               " | Position: Loan Officer" +
               " | Salary: " + getSalary() +
               " | Max Approval: $" + maxApprovalLimit;
    }



    
    @Override
    public boolean canContractApprove(Staff staff, Contract contract, LoaningSystem system){
        LoanOfficer officer = (LoanOfficer) staff;
        if(!officer.checkApplicantSalaryAndAge(contract.getPrincipalAmount(),contract.getApplicant(), system.getMaxDebtToIncomeRatio())){
            system.setLastMessage("REJECTED : Applicant's Salary not high enough");
            contract.setStatus(Contract.REJECTED);
            contract.setApprovingOfficer(officer);
            return false;
        }
        if(!officer.checkLoanOfficerApprovalLimit(contract.getPrincipalAmount())){
            system.setLastMessage("FORWARDED : Loan officer approval limit is not high enough");
            contract.setStatus(Contract.FORWARDED);
            contract.setApprovingOfficer(officer);
            return false;
        }
            contract.setStatus(Contract.APPROVED);
            contract.setApprovingOfficer(officer);
            system.setLastMessage("Contract #" + contract.getContractId() + " approved by " + officer.getName());
            return true;
    }
            
}
