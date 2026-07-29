package src.controller;

import java.util.InputMismatchException;
import java.util.Scanner;
import src.interfaces.ILoginable;
import src.model.Applicant;
import src.model.Staff;

public class Main {

    static Scanner scanner = new Scanner(System.in);
    static LoaningSystem system = new LoaningSystem("KH Bank", 0.05);
    static boolean loggin;
    static int isUser = -1;

    private static void printBackHint() {
        System.out.println("  (type 'back' at any prompt to return to the main menu)");
    }

    public static void main(String[] args) {

        System.out.println("==========================================");
        System.out.println("       WELCOME TO KH BANK LOANING SYSTEM ");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntNoBack("Enter your choice: ");

            try {
                validateChoice(choice, loggin);

                switch (choice) {
                    case 0:  System.out.println("Exiting. Goodbye!"); running = false; break;
                    case 1:  handleLogin();            break;
                    case 2:  handleLogout();           break;
                    case 3:  handleCreateStaff();      break;
                    case 4:  handleCreateApplicant();  break;
                    case 5:  handleCreateContract();   break;
                    case 6:  handleApproveContract();  break;
                    case 7:  handleRejectContract();   break;
                    case 8:  handleAddCoSigner();      break;
                    case 9:  handleDeactivateStaff();  break;
                    case 10: system.printStaffs();     break;
                    case 11: system.printApplicants(); break;
                    case 12: if (isUser == 1) { system.printMyContract(); } else { system.printContracts(); } break;
                    case 13: handleSetNewName();       break;
                    case 14: handleSetNewPassword();   break;
                    case 15: system.viewMyProfile();   break;
                    case 16: handleViewPaymentSchedule(); break;
                    case 17: handleMakePayment(); break;
                    case 18 : system.viewMyBalance(); break;
                    case 19: handleAddBalanceForApplicant(); break;
                    case 20: handleSetApprovalLimit();       break;
                    case 21: handleSetRequiredVotes();       break;
                    case 22: system.checkDelinquency();      break;
                    case 23: system.printAuditLog();         break;
                    case 24: handleSetMaxDti();              break;
                }

            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            } catch (BackActionException e) {
                System.out.println(e.getMessage());
            }
        }

        scanner.close();
        system.close();
    }


    private static void validateChoice(int choice, boolean loggin) {
        if (!loggin) {
            if (choice != 0 && choice != 1) {
                throw new InputMismatchException("Error : Invalid Choice");
            }
        } else {
            if (choice == 1) {
                throw new InputMismatchException("Error : Invalid Choice");
            }
            if (isUser == 1) {
                if (choice != 12 && choice != 13 && choice != 14 && choice != 15 && choice != 2 && choice != 0 && choice!=16 && choice !=17  && choice!=18) {
                    throw new InputMismatchException("Error : Invalid Choice");
                }
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n==========================================");
        System.out.println("                 MAIN MENU               ");
        System.out.println("==========================================");

        if (!loggin) {
            System.out.println("  [1] Login");

        } else if (isUser == 1) {
            System.out.println("  APPLICATIONS");
            System.out.println("  [12] View My Applications");
            System.out.println("  [16] View My Payment Schedule");
            System.out.println("  [17] MAKE PAYMENT");
            System.out.println("  [18] VIEW BALANCE");


            System.out.println("------------------------------------------");
            System.out.println("  PROFILE");
            System.out.println("  [13] Change Name");
            System.out.println("  [14] Change Password");
            System.out.println("  [15] View My Profile");


            System.out.println("------------------------------------------");
            System.out.println("  [2] Logout");

        } else {
            System.out.println("  MANAGER ACTIONS");
            System.out.println("  [3] Create Staff");
            System.out.println("  [4] Create Applicant");
            System.out.println("  [19] ADD BALANCE TO APPLICANT ACCOUNT");
            System.out.println("  [20] Set Loan Officer Approval Limit");
            System.out.println("  [21] Set Required Committee Votes");
            System.out.println("  [22] Run Delinquency Check");
            System.out.println("  [24] Set Max Debt-to-Income Ratio");
            System.out.println("------------------------------------------");
            System.out.println("  LOAN OFFICER ACTIONS");
            System.out.println("  [5] Create Contract");
            System.out.println("  [6] Approve Contract");
            System.out.println("  [7] Reject Contract");
            System.out.println("  [8] Add Co-Signer");
            System.out.println("------------------------------------------");
            System.out.println("  ADMIN ACTIONS");
            System.out.println("  [9] Deactivate Staff");
            System.out.println("------------------------------------------");
            System.out.println("  VIEW");
            System.out.println("  [10] Print All Staffs");
            System.out.println("  [11] Print All Applicants");
            System.out.println("  [12] Print All Contracts");
            System.out.println("  [23] View Audit Log");
            System.out.println("------------------------------------------");
            System.out.println("  PROFILE");
            System.out.println("  [13] Change Username");
            System.out.println("  [14] Change Password");
            System.out.println("  [15] View My Profile");
            System.out.println("------------------------------------------");
            System.out.println("  [2] Logout");
        }

        System.out.println("------------------------------------------");
        System.out.println("  [0] Exit");
        System.out.println("==========================================");
    
}
private static void handleMakePayment() {
    boolean validInput = false;
    while (!validInput) {
        try {
            System.out.println("\n--- MAKE PAYMENT ---");
            system.printMyContract();
            int contractId  = readInt("Enter contract ID: ");
            system.printMySchedule(contractId);
            double amount = readDouble("Enter amount to pay: ");
            system.makePayment(contractId, amount);
            validInput = true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}

    private static void handleLogin() {
        printBackHint();
        while (!loggin) {
            try {
                System.out.println("\n--- LOGIN ---");
                String username = readUsername("Enter username: ");
                String password = readPassword("Enter password: ");
                system.login(username, password);
                loggin = true;
                validateUser();
            } catch (BackActionException e) {
                System.out.println(e.getMessage());
                return;                          
            } catch (LogginException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void validateUser() {
        ILoginable user = system.getLoggedInUser();
        if (user instanceof Applicant) {
            isUser = 1;
        } else if (user instanceof Staff) {
            isUser = 0;
        }
    }

    private static void handleAddBalanceForApplicant(){
        boolean validInput=false;
        while(!validInput){
             try {
                system.printApplicants();
             int applicantId=readInt("Enter applicant id: ");
             int amount=readInt("Enter amount will be add: ");
             system.addBalanceforApplicant(applicantId, amount);
             validInput=true;
                
             } catch ( BackActionException e) {
                 System.out.println(e.getMessage());
                 return;
             }
             catch ( InputMismatchException e){
                 System.out.println(e.getMessage());
             }
        }

    }

    private static void handleSetApprovalLimit() {
        printBackHint();
        try {
            System.out.println("\n--- SET LOAN OFFICER APPROVAL LIMIT ---");
            system.printStaffs();
            int staffId = readInt("Enter Loan Officer staff ID: ");
            double newLimit = readDouble("Enter new approval limit: ");
            system.setNewApprovalLimit(staffId, newLimit);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleSetRequiredVotes() {
        printBackHint();
        try {
            System.out.println("\n--- SET REQUIRED COMMITTEE VOTES ---");
            int votes = readInt("Enter required number of committee votes: ");
            system.setNewRequiredVotes(votes);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleSetMaxDti() {
        printBackHint();
        try {
            System.out.println("\n--- SET MAX DEBT-TO-INCOME RATIO ---");
            double ratio = readDouble("Enter new max debt-to-income ratio (e.g. 0.40 for 40%): ");
            system.setNewMaxDebtToIncomeRatio(ratio);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleSetNewName() {
        printBackHint();
        boolean validInput = false;
        while (!validInput) {
            try {
                System.out.println("\n--- SET NEW NAME ---");
                String username    = readUsername("Enter your name: ");
                String password    = readPassword("Enter your password: ");
                String newUsername = readNewUsername("Enter your new name: ");
                system.setNewUserName(username, newUsername, password);
                validInput = true;
            } catch (BackActionException e) {
                System.out.println(e.getMessage());
                return;                         
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    private static void handleViewPaymentSchedule() {
    System.out.println("\n--- MY PAYMENT SCHEDULE ---");
    system.printMyContract();
    int contractId = readInt("Enter contract ID: ");
    system.printMySchedule(contractId);
}

    private static void handleSetNewPassword() {
        printBackHint();
        boolean validInput = false;
        while (!validInput) {
            try {
                System.out.println("\n--- SET NEW PASSWORD ---");
                String name               = readUsername("Enter your name: ");
                String password           = readPassword("Enter your password: ");
                String newPassword        = readPassword("Enter your new password: ");
                String confirmNewPassword = readPassword("Enter your new password again: ");
                if (!newPassword.equals(confirmNewPassword)) {
                    throw new InputMismatchException("Error : mismatch confirmation password");
                }
                system.setNewPassword(name, password, newPassword);
                validInput = true;
            } catch (BackActionException e) {
                System.out.println(e.getMessage());
                return;                          
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void handleLogout() {
        System.out.println("\n--- LOGOUT ---");
        system.logout();
        loggin = false;
        isUser = -1;
    }

    private static void handleCreateStaff() {
        printBackHint();
        boolean validInput = false;
        while (!validInput) {
            try {
                System.out.println("\n--- CREATE STAFF ---");
                String name        = readName("Enter name: ");
                String userName    = readNewUsername("Enter username: ");
                String phoneNumber = readPhoneNumber("Enter your phone number: ");
                int    age         = readAge("Enter age: ");
                String password    = readPassword("Enter password: ");
                double salary      = readDouble("Enter salary: ");
                String position    = readPosition("Select Position");
                system.createStaff(name, userName, phoneNumber, age, password, salary, position);
                validInput = true;
            } catch (BackActionException e) {
                System.out.println(e.getMessage());
                return;                         
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void handleCreateApplicant() {
        printBackHint();
        System.out.println("\n--- CREATE APPLICANT ---");
        boolean validInput = false;
        while (!validInput) {
            try {
                String name        = readName("Enter name: ");
                String userName    = readNewUsername("Enter username: ");
                String phoneNumber = readPhoneNumber("Enter your phone number: ");
                String password    = readPassword("Enter password: ");
                int    age         = readAge("Enter age: ");
                int    income      = readInt("Enter income: ");
                double existingExternalDebt = readDouble("Enter existing external debt (0 if none): ");
                String gender      = readGender("Select Gender");
                system.createApplicant(name, userName, phoneNumber, password, age, income, gender, existingExternalDebt);
                validInput = true;
            } catch (BackActionException e) {
                System.out.println(e.getMessage());
                return;                        
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void handleCreateContract() {
        printBackHint();
        boolean validInput = false;
        while (!validInput) {
            try {
                System.out.println("\n--- CREATE CONTRACT ---");
                system.printApplicants();
                int    applicantId = readInt("Enter applicant ID: ");
                double amount      = readDouble("Enter loan amount: ");
                int    duration    = readInt("Enter duration (years): ");
                system.createContract(applicantId, amount, duration);
                validInput = true;
            }
             catch (BackActionException e) {
                System.out.println(e.getMessage());
                return;                        
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    

    private static void handleApproveContract() {
        printBackHint();
        try {
            System.out.println("\n--- APPROVE CONTRACT ---");
            system.printContracts();
            int contractId = readInt("Enter contract ID to approve: ");
            system.approveContract(contractId);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleRejectContract() {
        printBackHint();
        try {
            System.out.println("\n--- REJECT CONTRACT ---");
            system.printContracts();
            int contractId = readInt("Enter contract ID to reject: ");
            system.rejectContract(contractId);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleAddCoSigner() {
        printBackHint();
        try {
            System.out.println("\n--- ADD CO-SIGNER ---");
            system.printContracts();
            int contractId = readInt("Enter contract ID: ");
            system.printStaffs();
            int staffId = readInt("Enter staff ID to add as co-signer: ");
            system.addCoSigner(contractId, staffId);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleDeactivateStaff() {
        printBackHint();
        try {
            System.out.println("\n--- DEACTIVATE STAFF ---");
            system.printStaffs();
            int staffId = readInt("Enter staff ID to deactivate: ");
            system.deactivateStaff(staffId);
        } catch (BackActionException e) {
            System.out.println(e.getMessage());
        }
    }


    private static void checkBack(String input) {
        if (input.trim().equalsIgnoreCase("back")) {
            throw new BackActionException();
        }
    }

    private static String readUsername(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine();
                checkBack(input);               
                if (input.isEmpty()) {
                    throw new InputMismatchException("Error: Input cannot be empty. Please try again.");
                }
                return input;
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String readNewUsername(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine();
                checkBack(input);                
                if (input.isEmpty()) {
                    throw new InputMismatchException("Error: Input cannot be empty. Please try again.");
                }
                if (!system.checkIfUsernameAvailable(input)) {
                    throw new InputMismatchException("Error: Username already exists.");
                }
                return input;
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String readPhoneNumber(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine();
                checkBack(input);                
                if (input.isEmpty()) {
                    throw new InputMismatchException("Error: Input cannot be empty. Please try again.");
                }
                if (!input.matches("^0[0-9]{8,9}$")) {
                    throw new InputMismatchException("Error: Invalid phone number. Must be 9–10 digits starting with 0.");
                }
                if (!system.checkIfPhoneNumerAvailable(input)) {
                    throw new InputMismatchException("Error: Phone number already exists.");
                }
                return input;
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String readName(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine().trim();
                checkBack(input);                
                if (input.isEmpty()) {
                    throw new InputMismatchException("Input cannot be empty.");
                }
                if (!input.matches("^[a-zA-Z ]+$")) {
                    throw new InputMismatchException("Input must contain only letters.");
                }
                return input;
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage()); 
            }
        }
    }

    private static int readIntNoBack(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value < 0 || value > 10000) {
                    throw new NumberFormatException();
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String raw = scanner.nextLine().trim();
                checkBack(raw);                  
                int value = Integer.parseInt(raw);
                if (value < 0 || value > 10000) {
                    throw new NumberFormatException("Range: 0 – 10 000");
                }
                return value;
                                   
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    private static int readAge(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String raw = scanner.nextLine().trim();
                checkBack(raw);                  
                int value = Integer.parseInt(raw);
                if (value < 18 || value > 65) {
                    throw new InputMismatchException("You must be at least 18 and at most 65 years old.");
                }
                return value;
            }
             catch (InputMismatchException | NumberFormatException e) {
                System.out.println(e.getMessage());
            }
        }
    }


    private static String readPassword(String prompt) {
        while (true) {
            System.out.println(prompt);
            try {
                String password = scanner.nextLine().trim();
                checkBack(password);             
                if (password.length() < 4) {
                    throw new InputMismatchException("Password cannot be under 4 characters.");
                }
                return password;
            } catch (InputMismatchException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String raw = scanner.nextLine().trim();
                checkBack(raw);                  
                double value = Double.parseDouble(raw);
                if (value < 0 || value > 10000) {
                    throw new NumberFormatException("Range: 0 – 10 000");
                }
                return value;
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
            catch (BackActionException e){
                System.out.println(e.getMessage());
            }
        }
    }

    private static String readGender(String prompt) {
        System.out.println(prompt);
        System.out.println("  [1] Male");
        System.out.println("  [2] Female");
        while (true) {
            int choice = readInt("Enter choice: "); 
            switch (choice) {
                case 1: return "M";
                case 2: return "F";
                default: System.out.println("Invalid choice. Please select 1 or 2.");
            }
        }
    }

    private static String readPosition(String prompt) {
        System.out.println(prompt);
        System.out.println("  [1] Manager");
        System.out.println("  [2] LoanOfficer");
        System.out.println("  [3] CreditCommittee");
        while (true) {
            int choice = readInt("Enter choice: "); 
            switch (choice) {
                case 1: return LoaningSystem.MANAGER;
                case 2: return LoaningSystem.LOAN_OFFICER;
                case 3: return LoaningSystem.CREDIT_COMMITTEE;
                default: System.out.println("Invalid choice. Please select 1 – 3.");
            }
        }
    }
}



