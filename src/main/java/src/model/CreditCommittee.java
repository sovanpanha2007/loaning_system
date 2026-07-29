package src.model;

import src.controller.LoaningSystem;

public class CreditCommittee extends Staff {

    public CreditCommittee(String name , String userName , String phoneNumber , int age ,String password,
                           double salary) {
        super(name,userName,phoneNumber ,age, password);
        setSalary(salary);
        setPosition(LoaningSystem.CREDIT_COMMITTEE);
    }

    @Override
    public boolean can(String action) {
        switch (action) {
            case LoaningSystem.APPROVE_LOAN: return true;
            case LoaningSystem.ADD_COSIGNER: return true;
            default: return super.can(action);
        }
    }

    @Override
    public String toString() {
        return super.toString() +
               " | Position: Credit Committee" +
               " | Salary: " + getSalary();
    }

    @Override
    public boolean canContractApprove(Staff staff , Contract contract, LoaningSystem system){
        CreditCommittee committee = (CreditCommittee) staff;

        if (!contract.addCommitteeVote(committee.getId())) {
            system.setLastMessage("Error: " + committee.getName()
                    + " has already voted on Contract #" + contract.getContractId());
            return false;
        }

        int required = system.getRequiredCommitteeVotes();
        int current = contract.getCommitteeVoteCount();

        if (current < required) {
            system.setLastMessage("Vote cast by " + committee.getName()
                    + ". Current votes: " + current + "/" + required);
            return false;
        }

        contract.setStatus(Contract.APPROVED);
        contract.setApprovingOfficer(committee);
        system.setLastMessage("Contract #" + contract.getContractId()
                + " approved by committee quorum (" + current + "/" + required + ")");
        return true;
    }
}

