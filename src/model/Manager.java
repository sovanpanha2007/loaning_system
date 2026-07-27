package src.model;

import src.controller.LoaningSystem;

public class Manager extends Staff {
     private int accessLevel;

    public Manager(String name,String userName , String phoneNumber , int age , String password , double salary, int accessLevel) {
        super(name,userName,phoneNumber ,age, password);
        setSalary(salary);
        setAccessLevel(accessLevel);
        setPosition(LoaningSystem.MANAGER);
    }

    public int getAccessLevel(){
        return accessLevel;
    }

    protected void setAccessLevel(int accessLevel){
        if(accessLevel < 1 || accessLevel > 3){
            System.out.println("Error : access level is not in range ");
            return;
        }
        this.accessLevel=accessLevel;

    }

    public void setBalanceApplicant(Applicant applicant , double amount){
        double newAmount= applicant.getBalance() +amount;
         applicant.setBalance(newAmount);
    } 




    @Override
    public boolean can(String action) {
        switch (action) {
            case LoaningSystem.CREATE_STAFF:     return true;
            case LoaningSystem.CREATE_APPLICANT: return true;
            case LoaningSystem.SET_NEW_APVL: return true;
            case LoaningSystem.SET_NEW_REQV: return true;
            case LoaningSystem.ADD_BALANCE : return true;
            default: return super.can(action);
        }
    }

    public void Hello(){
        System.out.println("Manager");
    }

    @Override
    public String toString() {
        return super.toString() +
               " | Position: Manager" +
               " | Salary: " + getSalary();
    }

    @Override
    public boolean canContractApprove( Staff staff ,Contract contract, LoaningSystem system){
         system.setLastMessage("Error: Manager cannot approve contract");
         return false;
    }
}