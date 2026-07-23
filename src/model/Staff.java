package src.model;

import src.controller.LoaningSystem;
import src.interfaces.ILoginable;
import src.interfaces.IStaff;
import src.util.PasswordUtil;
    
public abstract class Staff implements IStaff , ILoginable{

    private String name;
    private String userName;
    private String phoneNumber;
    private int age;
    private String password;
    private  static  int staffIndexID = 1;
    private int staffId;
    private boolean active;
    private double salary;
    private String position;

    public Staff(String name,String userName , String phoneNumber, int age , String password) {
        setName(name);
        setPassword(password);
        setAge(age);
        setUsername(userName);
        setPhoneNumber(phoneNumber);
        setNextStaffId();
        this.active=true;
        this.salary=0;
        this.position="Staff";
        System.out.println("Staff Constructor run Successfully");


    }


 @Override 
  public String getUsername(){
    return userName;
  }
  @Override
  public String getPhoneNumber(){
    return phoneNumber;
  }

    
    public String getName(){
        return name;
    }
  @Override
    public int getId(){
        return staffId;
    }
    public int getAge(){
        return age;
    }

    public double getSalary() {
        return salary;
    }

    public String getPosition() {
        return position;
    }

    public boolean isActive(){
        return  active;
    }

    public boolean checkPassword(String password){
       if(password==null){
        return false;
       }
       return PasswordUtil.matches(password, this.password);
    }

    // ===== Setters with validation =====


    public void setNextStaffId(){
        this.staffId=staffIndexID++;
    }
    public void setName(String name) {
        this.name=name;
    }

    public void setUsername(String username){
        this.userName=username;
    }
    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber=phoneNumber;
    }

    public void setAge(int age){
         if(age < 18 || age > 65){
         throw new IllegalArgumentException("Invalid age. Age should be over 18 and under 65");
          }else {
            this.age=age;
         }
    }

    public void setPassword(String password) {
        this.password = PasswordUtil.hash(password);
}

public void setSalary(double salary){
    if(salary <= 0){
       throw new IllegalArgumentException("Error: Salary must be greater than 0."); 
    }
    this.salary = salary;
}

public void setPosition(String position){
    if(position == null || position.isBlank()){
        throw new IllegalArgumentException("Error: Position cannot be empty.");
    }
    this.position = position;
}


public void setActive(boolean c){
    this.active=c;
}
   

    @Override
    public String toString() {
        return "Staff ID: " + staffId + ", Name: " + name + " , Username: " + userName + " , Phone number:" + phoneNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Staff)) return false;
        Staff s1 = (Staff) obj;
        return s1.name.equals(this.name) && s1.staffId == this.staffId;
    }
    @Override
    public void setNewName(String name){
         setName(name);
    }
    @Override
    public boolean can(String action){
           switch (action) {
            case LoaningSystem.SET_NEW_NAME : return true;
            case LoaningSystem.SET_NEW_PASSWORD: return true;
            default : return false;

        }
    }


}