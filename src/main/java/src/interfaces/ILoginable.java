package src.interfaces;



public interface ILoginable {
     String getName();
     String getUsername();
     String getPhoneNumber();
     void setPassword(String password);
     void setUsername(String username);
     void setPhoneNumber(String phoneNumber);
     boolean checkPassword(String password);
     boolean isActive();
     boolean can(String action);
     String toString();
     int getId(); 
}
