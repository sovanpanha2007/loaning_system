package src.model;

import src.interfaces.ILoginable;
import src.controller.LoaningSystem;
import src.util.PasswordUtil;


public class Applicant  implements ILoginable {

    // ===== Fields =====
    private String name;
    private String userName;
    private String phoneNumber;
    private double accountBalance;
    private String password;
    private String gender;
    private boolean active;
    private static int indexID = 1;
    private int applicantId;
    private int salary;
    private  int age;

    // ===== Constructor =====
    public Applicant(String name,String username , String phoneNumber,String password, String gender, int salary, int age) {
        setName(name);
        setPassword(password);
        setGender(gender);
        setAge(age);
        setSalary(salary);
        setUsername(username);
        setPhoneNumber(phoneNumber);
        this.applicantId = indexID++;
        this.active=true;
        this.accountBalance=0;
    }
  @Override
    public String getName() {
        return name;
    }
  @Override 
  public String getUsername(){
    return userName;
  }
  @Override
  public String getPhoneNumber(){
    return phoneNumber;
  }


  public double getBalance(){
     return accountBalance;
  }
  
    public int getSalary() {
        return salary;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public int getId() {
        return applicantId;
    }

    public void setName(String name) {
        this.name=name;

    }
    @Override
     public void setPassword(String password) {
        this.password = PasswordUtil.hash(password);
    }

    @Override 
    public void setUsername(String username){
        this.userName=username;  
    }

    @Override
    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber=phoneNumber;
    }

    public void setGender(String gender) {
        String regex = "^[MF]$";
        if (gender.matches(regex)) {
            this.gender = gender;
            return;
        }
      throw new IllegalArgumentException("Invalid gender. Gender should be 'M' or 'F'.");
    }

    protected void setBalance(double amount){
         this.accountBalance=amount;
    }

    public void setSalary(int salary) {
        if (salary > 0) {
            this.salary = salary;
            return;
        }
        throw new IllegalArgumentException("Invalid salary. Salary should be a positive integer.");
    }

    public void setAge(int age) {
        if (age >= 18 && age <= 65) {
            this.age= age;
            return;
        } 
      throw new IllegalArgumentException("Invalid age. Age should be between 18 and 65.");  
    }

    public boolean deductBalance(double amount){
        if(getBalance() < amount){
              return false;
        }
        double newAmount = getBalance() - amount;
        this.setBalance(newAmount);
        return true;
    }

@Override
    public boolean checkPassword(String password){
       if(password==null){
        return false;
       }
       return PasswordUtil.matches(password, this.password);
    }
@Override
     public boolean isActive(){
        return  active;
    }

 @Override
    public boolean can(String action){
           switch (action) {
            case LoaningSystem.SET_NEW_NAME : return true;
            case LoaningSystem.SET_NEW_PASSWORD: return true;
            case LoaningSystem.CREATE_CONTRACT : return true;
            case LoaningSystem.MAKE_PAYMENT : return true;
            case LoaningSystem.VIEW_OWN_CONTRACT : return true;
            case LoaningSystem.VIEW_OWN_SCHEDULE : return true;
            case LoaningSystem.VIEW_OWN_BALANCE : return true;
            default : return false;
        }
    }
    
     
   


    @Override
    public String toString() {
        return "Name: " + name + " , Username: "+ userName + " , Phone number: " + phoneNumber + ", Id: " + applicantId + ", gender: " + gender + ", salary: " + salary + ", age: " + age;
    }

    public boolean equals(Applicant applicant2) {
        if (applicant2 == null) {
            return false;
        }
        if (this.name.equals(applicant2.name) && this.applicantId == applicant2.applicantId) {
            return true;
        }
        return false;
    }
}