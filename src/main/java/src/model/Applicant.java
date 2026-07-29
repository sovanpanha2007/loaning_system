package src.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import src.interfaces.ILoginable;
import src.controller.LoaningSystem;
import src.util.PasswordUtil;


public class Applicant  implements ILoginable {

    // ===== Fields =====
    private String name;
    private String userName;
    private String phoneNumber;
    private BigDecimal accountBalance;
    private String password;
    private String gender;
    private boolean active;
    private int applicantId;
    private BigDecimal salary;
    private  int age;
    private BigDecimal existingExternalDebt;

    // ===== Constructor =====
    public Applicant(String name,String username , String phoneNumber,String password, String gender, BigDecimal salary, int age, BigDecimal existingExternalDebt) {
        setName(name);
        setPassword(password);
        setGender(gender);
        setAge(age);
        setSalary(salary);
        setExistingExternalDebt(existingExternalDebt);
        setUsername(username);
        setPhoneNumber(phoneNumber);
        this.active=true;
        this.accountBalance=BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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


  public BigDecimal getBalance(){
     return accountBalance;
  }

    public BigDecimal getSalary() {
        return salary;
    }

    public int getAge() {
        return age;
    }

    public BigDecimal getExistingExternalDebt() {
        return existingExternalDebt;
    }

    public String getGender() {
        return gender;
    }

    public int getId() {
        return applicantId;
    }

    // Assigned by ApplicantDao once the row is inserted (or when hydrating from an existing row) —
    // ids are database-generated now, not a static in-process counter.
    public void setId(int id){
        this.applicantId=id;
    }

    public void setName(String name) {
        this.name=name;

    }
    @Override
     public void setPassword(String password) {
        this.password = PasswordUtil.hash(password);
    }

    // Lets the DAO round-trip an already-hashed password read back from storage
    // without re-hashing it through setPassword(String plaintext).
    public String getPasswordHash(){
        return password;
    }

    public void setPasswordHash(String passwordHash){
        this.password=passwordHash;
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

    public void setBalance(BigDecimal amount){
         this.accountBalance=amount.setScale(2, RoundingMode.HALF_UP);
    }

    public void setSalary(BigDecimal salary) {
        if (salary != null && salary.compareTo(BigDecimal.ZERO) > 0) {
            this.salary = salary.setScale(2, RoundingMode.HALF_UP);
            return;
        }
        throw new IllegalArgumentException("Invalid salary. Salary should be a positive amount.");
    }

    public void setAge(int age) {
        if (age >= 18 && age <= 65) {
            this.age= age;
            return;
        }
      throw new IllegalArgumentException("Invalid age. Age should be between 18 and 65.");
    }

    // Self-reported debt the applicant already carries outside this bank (other loans, credit
    // cards, etc.) — factored into the debt-to-income check so this bank isn't the only lender
    // whose exposure counts against the applicant.
    public void setExistingExternalDebt(BigDecimal existingExternalDebt) {
        if (existingExternalDebt == null || existingExternalDebt.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid existing external debt. It cannot be negative.");
        }
        this.existingExternalDebt = existingExternalDebt.setScale(2, RoundingMode.HALF_UP);
    }

    // Business rule: total debt (existing external debt + what's already borrowed from this
    // bank) may never exceed maxDebtToIncomeRatio of salary.
    public BigDecimal getMaxBorrowableAmount(double maxDebtToIncomeRatio) {
        return salary.multiply(BigDecimal.valueOf(maxDebtToIncomeRatio))
                .subtract(existingExternalDebt)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean canBorrow(BigDecimal alreadyBorrowed, BigDecimal requestedAmount, double maxDebtToIncomeRatio) {
        return alreadyBorrowed.add(requestedAmount).compareTo(getMaxBorrowableAmount(maxDebtToIncomeRatio)) < 0;
    }

    public boolean deductBalance(BigDecimal amount){
        if(getBalance().compareTo(amount) < 0){
              return false;
        }
        this.setBalance(getBalance().subtract(amount));
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

    public void setActive(boolean active){
        this.active=active;
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
        return "Name: " + name + " , Username: "+ userName + " , Phone number: " + phoneNumber + ", Id: " + applicantId + ", gender: " + gender + ", salary: " + salary + ", age: " + age + ", external debt: " + existingExternalDebt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Applicant)) return false;
        Applicant other = (Applicant) obj;
        return this.applicantId == other.applicantId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(applicantId);
    }
}