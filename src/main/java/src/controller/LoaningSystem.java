package src.controller;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.InputMismatchException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import src.dao.ApplicantDao;
import src.dao.AuditDao;
import src.dao.ContractDao;
import src.dao.DataAccessException;
import src.dao.Database;
import src.dao.PaymentScheduleDao;
import src.dao.StaffDao;
import src.interfaces.ILoginable;
import src.model.Applicant;
import src.model.AuditEntry;
import src.model.Contract;
import src.model.CreditCommittee;
import src.model.LoanOfficer;
import src.model.Manager;
import src.model.Payment;
import src.model.PaymentSchedule;
import src.model.Staff;

public class LoaningSystem {

    public static final String CREATE_STAFF     = "CREATE_STAFF";
    public static final String CREATE_APPLICANT = "CREATE_APPLICANT";
    public static final String CREATE_CONTRACT  = "CREATE_CONTRACT";
    public static final String APPROVE_LOAN     = "APPROVE_LOAN";
    public static final String REJECT_LOAN      = "REJECT_LOAN";
    public static final String ADD_COSIGNER     = "ADD_COSIGNER";
    public static final String SET_NEW_APVL     = "SET_NEW_APVL";
    public static final String SET_NEW_REQV     = "SET_NEW_REQV";
    public static final String SET_NEW_DTI      = "SET_NEW_DTI";

    public static final String MANAGER          = "MANAGER";
    public static final String LOAN_OFFICER     = "LOANOFFICER";
    public static final String CREDIT_COMMITTEE  = "CREDITCOMMITTEE";

    public static final String SET_NEW_NAME = "SET_NEW_NAME";
    public static final String SET_NEW_PASSWORD = "SET_NEW_PASSWORD";
    public static final String MAKE_PAYMENT = "MAKE_PAYMENT";
    public static final String VIEW_OWN_CONTRACT = "VIEW_OWN_CONTRACT";
    public static final String VIEW_OWN_SCHEDULE = "VIEW_OWN_SCHEDULE";
    public static final String VIEW_OWN_BALANCE = "VIEW_OWN_BALANCE";

    public static final String ADD_BALANCE = "ADD_BALANCE";
    public static final String CHECK_DELINQUENCY = "CHECK_DELINQUENCY";
    public static final String VIEW_AUDIT_LOG = "VIEW_AUDIT_LOG";

    private static final String DEFAULT_DB_FILE = "loaning_system.db";
    private static final BigDecimal LATE_FEE = new BigDecimal("25.00");
    private static final int MAX_CONSECUTIVE_LATE_PAYMENTS = 3;

    private String bankName;
    private double currentInterestRate = 0.05;
    private int requiredCommitteeVotes = 2;
    private double maxDebtToIncomeRatio = 0.40;

    private final Connection connection;
    private final StaffDao staffDao;
    private final ApplicantDao applicantDao;
    private final ContractDao contractDao;
    private final PaymentScheduleDao scheduleDao;
    private final AuditDao auditDao;

    private ILoginable loggedInUser;
    private String lastMessage;

    // Guards createContract/approveContract/makePayment's check-then-write sequences so two
    // threads can't both pass a business-rule check (borrowing limit, "not already approved",
    // "not already paid") before either one's write lands — a race that plain DB transactions
    // don't close on their own since the check and the write are separate statements.
    private final ReentrantLock writeLock = new ReentrantLock();

    public LoaningSystem(String bankName, double currentInterestRate) {
        this(bankName, currentInterestRate, DEFAULT_DB_FILE);
    }

    public LoaningSystem(String bankName, double currentInterestRate, String dbFilePath) {
        setBankName(bankName);
        setCurrentInterestRate(currentInterestRate);

        try {
            this.connection = Database.connect(dbFilePath);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
        this.staffDao = new StaffDao(connection);
        this.applicantDao = new ApplicantDao(connection);
        this.contractDao = new ContractDao(connection, applicantDao, staffDao);
        this.scheduleDao = new PaymentScheduleDao(connection, contractDao);
        this.auditDao = new AuditDao(connection);

        this.loggedInUser = null;
        this.lastMessage   = "";

        seedDefaultAdminIfEmpty();
    }

    public String getBankName()                  { return bankName; }
    public double getCurrentInterestRate()        { return currentInterestRate; }
    public int getRequiredCommitteeVotes(){ return requiredCommitteeVotes; }
    public double getMaxDebtToIncomeRatio(){ return maxDebtToIncomeRatio; }
    public String getLastMessage()               { return lastMessage; }
    public boolean isLoggedIn()             { return loggedInUser != null; }
    public ILoginable getLoggedInUser()             { return loggedInUser; }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public void viewMyProfile(){
        System.out.println(loggedInUser.toString());
    }

    public void viewMyBalance(){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.VIEW_OWN_BALANCE)) return;

        Applicant applicant = findApplicantById(loggedInUser.getId());

         System.out.println("Your balance : " + applicant.getBalance() + "$");
    }

    public void addBalanceforApplicant(int applicantId , int amount){
         if(!requireLogin()) return;
         if(!requirePermission(ADD_BALANCE)) return;

         Applicant applicant = findApplicantById(applicantId);
         if(applicant == null){
            setLastMessage("Error : Invalid id applicant not found");
            return;
         }
         Manager manager = (Manager) loggedInUser;
         manager.setBalanceApplicant(applicant, BigDecimal.valueOf(amount));
         // No real payment rail exists to integrate here, so this stays a manually-recorded
         // entry — but it's now a traceable ledger entry (actor, amount, timestamp) via the
         // audit log instead of a bare, unexplained balance mutation.
         transaction(() -> {
             applicantDao.update(applicant);
             auditDao.record(loggedInUser, "DEPOSIT", "APPLICANT", applicantId, "Amount: $" + amount + ", recorded by " + loggedInUser.getName());
         });
         setLastMessage("Successfully add money into " + applicant.getName() + " balance");
    }

    public void setBankName(String bankName) {
        if (isBlank(bankName)) {
            this.bankName = "Default Bank";
            return;
        }
        this.bankName = bankName.trim();
    }

    public void setCurrentInterestRate(double rate) {
        if (rate <= 0 || rate >= 1) {
            System.out.println("Error: Interest rate must be between 0 and 1 (e.g. 0.05 for 5%).");
            return;
        }
        this.currentInterestRate = rate;
    }

    public void setLastMessage(String msg) {
        this.lastMessage = msg;
        System.out.println(msg);
    }

    private void seedDefaultAdminIfEmpty() {
        List<Staff> existing = sql(staffDao::findAll);
        if (!existing.isEmpty()) {
            return;
        }
        Staff admin = new Manager("Admin","Admin123","default",18,"1234", 5000,2);
        sql(() -> staffDao.insert(admin));
        setLastMessage("System ready. Default admin account seeded.");
    }

    public void printMyContract(){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.VIEW_OWN_CONTRACT)) return;

      List<Contract> myContracts = sql(() -> contractDao.findByApplicantId(loggedInUser.getId()));
      for (Contract contract : myContracts) {
          System.out.println(contract.toString());
      }
      if (myContracts.isEmpty()) {
          setLastMessage("Error : Contract not found");
      }
    }

    public void login(String name, String password) {
        if (isBlank(name) || isBlank(password)) {
            setLastMessage("Error: Name or password cannot be empty.");
            return;
        }

        Staff s = sql(() -> staffDao.findByUsername(name.trim()));
        if (s != null) {
            if (!s.isActive()) {
                throw new LogginException("Error : User account is inactive");
            }
            if (!s.checkPassword(password)) {
                throw new LogginException("Error : Wrong Password");
            }
            loggedInUser = s;
            sql(() -> auditDao.record(s, "LOGIN", null, null, null));
            setLastMessage("Login success. Welcome " + s.getName() + "!");
            return;
        }

        Applicant a = sql(() -> applicantDao.findByUsername(name.trim()));
        if (a != null) {
            if (!a.isActive()) {
                throw new LogginException("Error : User account is inactive");
            }
            if (!a.checkPassword(password)) {
                throw new LogginException("Error : Wrong Password");
            }
            loggedInUser = a;
            sql(() -> auditDao.record(a, "LOGIN", null, null, null));
            setLastMessage("Login success. Welcome " + a.getName() + "!");
            return;
        }
        throw new LogginException("Error : User not found");
    }

    public void logout() {
        if (loggedInUser== null) {
            setLastMessage("No staff is currently logged in.");
            return;
        }
        setLastMessage("Goodbye " + loggedInUser.getName() + ". Logged out successfully.");
        loggedInUser = null;
    }

    public void createStaff(String name,String userName , String phoneNumber, int age, String password, double salary, String position) {
        if (!requireLogin()) return;
        if (!requirePermission(CREATE_STAFF)) return;

        if (isBlank(name) || isBlank(password)) {
            setLastMessage("Error: Name or password cannot be empty.");
            return;
        }

        if (!checkIfUsernameAvailable(userName)) {
            throw new InputMismatchException("Error : username already taken");
        }
        if (!checkIfPhoneNumerAvailable(phoneNumber)) {
            throw new InputMismatchException("Error : phone number already registered");
        }

        Staff newStaff;
        if (position.equals(LoaningSystem.MANAGER)) {
            newStaff = new Manager(name ,userName,phoneNumber,age,password, salary,1);
        } else if (position.equals(LoaningSystem.LOAN_OFFICER)) {
            newStaff = new LoanOfficer(name ,userName,phoneNumber,age ,password, salary, BigDecimal.valueOf(50000));
        } else if (position.equals(LoaningSystem.CREDIT_COMMITTEE)) {
            newStaff = new CreditCommittee(name ,userName ,phoneNumber,age,password, salary);
        } else {

         throw new IllegalArgumentException("Error: Unknown position '" + position + "'. Use Manager, LoanOfficer, or CreditCommittee.");
        }

        transaction(() -> {
            staffDao.insert(newStaff);
            auditDao.record(loggedInUser, "CREATE_STAFF", "STAFF", newStaff.getId(), newStaff.getName() + " (" + position + ")");
        });
        setLastMessage("Staff created successfully: " + newStaff.getName() + " | Role: " + position);
    }

    public void createApplicant(String name,String userName , String phoneNumber,String password, int age, int income, String gender, double existingExternalDebt) {
        if (!requireLogin()) return;
        if (!requirePermission(CREATE_APPLICANT)) return;


        if (isBlank(name)) {
            setLastMessage("Error: Applicant name cannot be empty.");
            return;
        }
        if (!checkIfUsernameAvailable(userName)) {
            throw new InputMismatchException("Error : username already taken");
        }
        if (!checkIfPhoneNumerAvailable(phoneNumber)) {
            throw new InputMismatchException("Error : phone number already registered");
        }

        Applicant applicant = new Applicant(name,userName,phoneNumber,password, gender, BigDecimal.valueOf(income), age, BigDecimal.valueOf(existingExternalDebt));
        transaction(() -> {
            applicantDao.insert(applicant);
            auditDao.record(loggedInUser, "CREATE_APPLICANT", "APPLICANT", applicant.getId(), applicant.getName());
        });
        setLastMessage("Applicant created successfully: " + name);
    }

    public void createContract(int applicantId, double amount, int duration) {
        if (!requireLogin()) return;
        if (!requirePermission(CREATE_CONTRACT)) return;

        writeLock.lock();
        try {
        Applicant applicant = findApplicantById(applicantId);
        if (applicant == null) {
            throw new IllegalArgumentException("Applicant ID "+ applicantId + " doesn't exist");
        }

        BigDecimal principal = BigDecimal.valueOf(amount);

        List<Contract> existingContracts = sql(() -> contractDao.findByApplicantId(applicantId));
        BigDecimal borrowedAmount = BigDecimal.ZERO;
        for (Contract c : existingContracts) {
            borrowedAmount = borrowedAmount.add(c.getPrincipalAmount());
        }

        if (!applicant.canBorrow(borrowedAmount, principal, maxDebtToIncomeRatio)) {
            throw new IllegalArgumentException("Applicant cannot take more loan. Total debt would exceed "
                    + (maxDebtToIncomeRatio * 100) + "% of salary." +
                    "\n  Existing external debt: " + applicant.getExistingExternalDebt() +
                    "\n  Already borrowed (this bank): " + borrowedAmount +
                    "\n  Requested: " + principal +
                    "\n  Max additional borrowing allowed: " + applicant.getMaxBorrowableAmount(maxDebtToIncomeRatio));
        }

        Contract contract = new Contract(applicant, principal, duration, currentInterestRate);
        if (loggedInUser instanceof Staff) {
            contract.setDraftingOfficer((Staff) loggedInUser);
        }
        transaction(() -> {
            contractDao.insert(contract);
            auditDao.record(loggedInUser, "CREATE_CONTRACT", "CONTRACT", contract.getContractId(),
                    "Principal: " + principal + ", Applicant: " + applicant.getId());
        });
        setLastMessage("Contract created successfully. ID: " + contract.getContractId());
        } finally {
            writeLock.unlock();
        }
    }

    public void approveContract(int contractId) {
        if (!requireLogin()) return;
        if (!requirePermission(APPROVE_LOAN)) return;

        writeLock.lock();
        try {
        Contract contract = findContractById(contractId);
        if (contract == null) {
            setLastMessage("Error: Contract not found.");
            return;
        }
        if (contract.isApproved()) {
            setLastMessage("Error: Contract is already approved.");
            return;
        }
        if (!contract.getStatus().equals("PENDING") && !contract.getStatus().equals("FORWARDED")) {
            setLastMessage("Error: Contract cannot be approved at status: " + contract.getStatus());
            return;
        }
        Staff officer = (Staff) loggedInUser;
        boolean approved = officer.canContractApprove(officer, contract, this);
        // Derived from what this call actually did, not just the contract's final status —
        // a committee vote that doesn't reach quorum leaves status completely unchanged (it
        // could already have been FORWARDED by an earlier loan officer), so reading status
        // alone would misreport a mere vote as a fresh FORWARD_CONTRACT/REJECT_CONTRACT.
        String auditAction;
        if (approved) {
            auditAction = "APPROVE_CONTRACT";
        } else if (officer instanceof CreditCommittee) {
            auditAction = "VOTE_CONTRACT";
        } else if (contract.getStatus().equals(Contract.REJECTED)) {
            auditAction = "REJECT_CONTRACT";
        } else if (contract.getStatus().equals(Contract.FORWARDED)) {
            auditAction = "FORWARD_CONTRACT";
        } else {
            auditAction = "VOTE_CONTRACT";
        }

        transaction(() -> {
            contractDao.save(contract);
            if (officer instanceof CreditCommittee) {
                contractDao.addCommitteeVote(contract.getContractId(), officer.getId());
            }
            auditDao.record(officer, auditAction, "CONTRACT", contract.getContractId(), "by " + officer.getName());
            if (approved) {
                PaymentSchedule schedule = new PaymentSchedule(contract);
                scheduleDao.insert(schedule);
                setLastMessage("Contract #" + contract.getContractId() + " approved by " + officer.getPosition() + ": " + officer.getName() + " generate schedule for payment : " + schedule.getScheduleId());
            }
        });
        } finally {
            writeLock.unlock();
        }
    }


    public void makePayment(int contractId , double amount){
       if(!requireLogin()) return;
       if(!requirePermission(LoaningSystem.MAKE_PAYMENT)) return;

       writeLock.lock();
       try {
       PaymentSchedule schedule = findScheduleByContractId(contractId);
       if(schedule==null){
        setLastMessage("Error: No payment schedule found for this contract.");
        return;
       }

       Applicant applicant = (Applicant) loggedInUser;
       if(schedule.getContract().getApplicant().getId()!= applicant.getId()){
         setLastMessage("Error: This is not your contract.");
         return;
       }
       if(schedule.isFullyPaid()){
        setLastMessage("Error: This contract is already fully paid.");
        return;
       }

       BigDecimal requested = BigDecimal.valueOf(amount);
       if(requested.compareTo(BigDecimal.ZERO) <= 0){
        setLastMessage("Error: Payment amount must be greater than 0.");
        return;
       }
       if(requested.compareTo(applicant.getBalance()) > 0){
        setLastMessage("Error: Insufficient balance to make this payment.");
        return;
       }

       PaymentSchedule.PaymentResult result = schedule.applyPayment(requested);
       applicant.deductBalance(result.getAmountApplied());
       boolean fullyPaid = schedule.isFullyPaid();
       if (fullyPaid) {
           schedule.getContract().setStatus("CLOSED");
       }

       transaction(() -> {
           for (int month : result.getMonthsTouched()) {
               Payment p = schedule.getPayment(month);
               scheduleDao.updatePaymentProgress(schedule.getScheduleId(), month, p.getAmountPaid(), p.isPaid(), p.getPaidDate());
           }
           applicantDao.update(applicant);
           auditDao.record(applicant, "MAKE_PAYMENT", "CONTRACT", contractId,
                   "Amount: $" + result.getAmountApplied() + ", months: " + result.getMonthsTouched());
           if (fullyPaid) {
               contractDao.save(schedule.getContract());
           }
       });

       setLastMessage("Payment of $" + result.getAmountApplied() + " applied, covering month(s): " + result.getMonthsTouched()
               + ". Remaining balance: " + applicant.getBalance());
       if (fullyPaid) {
           setLastMessage("Congratulations! Loan fully paid. Contract CLOSED.");
       }
       } finally {
           writeLock.unlock();
       }
    }


    public void printMySchedule(int contractId){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.VIEW_OWN_SCHEDULE)) return;

         PaymentSchedule schedule = findScheduleByContractId(contractId);
       if(schedule==null){
        setLastMessage("Error: No payment schedule found for this contract.");
        return;
       }

       Applicant applicant = (Applicant) loggedInUser;
       if(schedule.getContract().getApplicant().getId()!= applicant.getId()){
         setLastMessage("Error: This is not your contract.");
         return;
       }


       schedule.printSchedule();
    }

    public void checkDelinquency() {
        checkDelinquency(LocalDate.now());
    }

    // Package-visible overload so tests can drive delinquency checks against a fixed
    // date instead of depending on the wall clock.
    void checkDelinquency(LocalDate asOf) {
        if (!requireLogin()) return;
        if (!requirePermission(CHECK_DELINQUENCY)) return;

        writeLock.lock();
        try {
            List<PaymentSchedule> schedules = sql(scheduleDao::findAll);
            int flaggedLate = 0;
            int defaulted = 0;

            for (PaymentSchedule schedule : schedules) {
                Contract contract = schedule.getContract();
                if (!contract.getStatus().equals(Contract.APPROVED)) {
                    continue;
                }

                List<Payment> newlyLate = schedule.checkForOverduePayments(asOf, LATE_FEE);
                if (newlyLate.isEmpty()) {
                    continue;
                }

                flaggedLate += newlyLate.size();
                boolean justDefaulted = schedule.getConsecutiveLateCount() >= MAX_CONSECUTIVE_LATE_PAYMENTS;
                if (justDefaulted) {
                    contract.setStatus(Contract.DEFAULTED);
                    defaulted++;
                }

                transaction(() -> {
                    for (Payment p : newlyLate) {
                        scheduleDao.markLate(schedule.getScheduleId(), p.getMonthNumber(), p.getLateFee());
                    }
                    if (justDefaulted) {
                        contractDao.save(contract);
                        auditDao.record(loggedInUser, "CONTRACT_DEFAULTED", "CONTRACT", contract.getContractId(),
                                schedule.getConsecutiveLateCount() + " consecutive late payments");
                    }
                });

                System.out.println("Contract #" + contract.getContractId() + ": " + newlyLate.size()
                        + " month(s) newly overdue (+" + LATE_FEE + " fee each)"
                        + (justDefaulted ? " — contract DEFAULTED (" + schedule.getConsecutiveLateCount() + " consecutive late payments)" : ""));
            }

            setLastMessage("Delinquency check complete: " + flaggedLate + " payment(s) newly flagged late, " + defaulted + " contract(s) defaulted.");
        } finally {
            writeLock.unlock();
        }
    }


    public void rejectContract(int contractId) {
        if (!requireLogin()) return;
        if (!requirePermission(REJECT_LOAN)) return;

        Contract contract = findContractById(contractId);
        if (contract == null) {
            setLastMessage("Error: Contract not found.");
            return;
        }
        if (contract.isApproved()) {
            setLastMessage("Error: Cannot reject an already approved contract.");
            return;
        }

        contract.setStatus("REJECTED");
        transaction(() -> {
            contractDao.save(contract);
            auditDao.record(loggedInUser, "REJECT_CONTRACT", "CONTRACT", contractId, "by " + loggedInUser.getName());
        });
        setLastMessage("Contract #" + contractId + " rejected by: " + loggedInUser.getName());
    }

    // ===== Add CoSigner =====
    public void addCoSigner(int contractId, int staffId) {
        if (!requireLogin()) return;
        if (!requirePermission(ADD_COSIGNER)) return;

        Contract contract = findContractById(contractId);
        if (contract == null) {
            setLastMessage("Error: Contract not found.");
            return;
        }

        Staff signer = findStaffById(staffId);
        if (signer == null) {
            setLastMessage("Error: Staff not found.");
            return;
        }

        if (!(signer instanceof CreditCommittee)) {
            setLastMessage("Error: Only CreditCommittee  can co-sign a contract.");
            return;
        }

        boolean added = contract.addCoSigner(signer);
        if (added) {
            final Staff finalSigner = signer;
            transaction(() -> {
                contractDao.addCoSigner(contractId, finalSigner.getId());
                auditDao.record(loggedInUser, "ADD_COSIGNER", "CONTRACT", contractId, finalSigner.getName());
            });
            setLastMessage("Co-signer added successfully: " + signer.getName());
        } else {
            setLastMessage("Error: Could not add co-signer.");
        }
    }
    public void deactivateStaff(int staffId) {
        if (!requireLogin()) return;
        if (!requirePermission(CREATE_STAFF)) return;

        Staff staff = findStaffById(staffId);
        if (staff == null) {
            setLastMessage("Error: Staff not found.");
            return;
        }

        staff.setActive(false);
        transaction(() -> {
            staffDao.update(staff);
            auditDao.record(loggedInUser, "DEACTIVATE_STAFF", "STAFF", staffId, staff.getName());
        });
        setLastMessage("Staff deactivated: " + staff.getName());
    }


    public void setNewApprovalLimit(int loanOfficerId , double newAmount){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.SET_NEW_APVL)) return;

        Staff staff=findStaffById(loanOfficerId);
        if(staff==null){
            setLastMessage("Error : No staff found");
            return;
        }

        if(!(staff instanceof LoanOfficer)){
            setLastMessage("Error : staff is not a Loan Officerr");
            return;
        }

        LoanOfficer officer = (LoanOfficer) staff;
        officer.setMaxApprovalLimit(BigDecimal.valueOf(newAmount));
        transaction(() -> {
            staffDao.update(officer);
            auditDao.record(loggedInUser, "SET_APPROVAL_LIMIT", "STAFF", loanOfficerId, "New limit: $" + newAmount);
        });
        setLastMessage("Sucessfully set new approval limit for "+ officer.getName());

    }

    public void setNewRequiredVotes(int votes){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.SET_NEW_REQV)) return;

        if(votes <= 0){
            setLastMessage("Error : Required votes must be at least 1");
            return;
        }

        this.requiredCommitteeVotes = votes;
        sql(() -> auditDao.record(loggedInUser, "SET_REQUIRED_VOTES", null, null, "New required votes: " + votes));
        setLastMessage("Successfully set required committee votes to " + votes);
    }

    public void setNewMaxDebtToIncomeRatio(double ratio){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.SET_NEW_DTI)) return;

        if(ratio <= 0 || ratio > 1){
            setLastMessage("Error : Debt-to-income ratio must be between 0 and 1 (e.g. 0.40 for 40%)");
            return;
        }

        this.maxDebtToIncomeRatio = ratio;
        sql(() -> auditDao.record(loggedInUser, "SET_MAX_DTI", null, null, "New max debt-to-income ratio: " + ratio));
        setLastMessage("Successfully set max debt-to-income ratio to " + (ratio * 100) + "%");
    }

    public void setNewUserName(String username ,String newUsername ,  String password){
        if(!requireLogin())  return;
        if(!requirePermission(LoaningSystem.SET_NEW_NAME)) return;


        if(loggedInUser.getUsername().equalsIgnoreCase(username) && loggedInUser.checkPassword(password)){

                if (!checkIfUsernameAvailable(newUsername)) {
                    throw new InputMismatchException("Error : username already taken");
                }
                loggedInUser.setUsername(newUsername);
                persistLoggedInUser("CHANGE_USERNAME", "New username: " + newUsername);
                System.out.println("Successfully change username ");
                return;
            }
            throw new InputMismatchException("Error : Authentication failed");
        }

    public void setNewPassword(String name , String password , String  newPassword){
        if(!requireLogin()) return;
        if(!requirePermission(LoaningSystem.SET_NEW_PASSWORD)) return ;

        if(loggedInUser.getUsername().equals(name) && loggedInUser.checkPassword(password)){
                loggedInUser.setPassword(newPassword);
                persistLoggedInUser("CHANGE_PASSWORD", null);
                System.out.println("Successfully change your password");
                return;
        }
        throw new InputMismatchException("Error : Authentication failed");
    }

    private void persistLoggedInUser(String action, String details) {
        transaction(() -> {
            if (loggedInUser instanceof Staff staff) {
                staffDao.update(staff);
            } else if (loggedInUser instanceof Applicant applicant) {
                applicantDao.update(applicant);
            }
            auditDao.record(loggedInUser, action, null, null, details);
        });
    }


    public void printStaffs() {
        List<Staff> staffs = sql(staffDao::findAll);
        System.out.println("\n--- Staffs (" + staffs.size() + ") ---");
        if (staffs.isEmpty()) {
            System.out.println("No staff found.");
            return;
        }
        for (int i = 0; i < staffs.size(); i++) {
            System.out.println((i + 1) + ") " + staffs.get(i));
        }
    }

    public void printApplicants() {
        List<Applicant> applicants = sql(applicantDao::findAll);
        System.out.println("\n--- Applicants (" + applicants.size() + ") ---");
        if (applicants.isEmpty()) {
            System.out.println("No applicants found.");
            return;
        }
        for (int i = 0; i < applicants.size(); i++) {
            System.out.println((i + 1) + ") " + applicants.get(i));
        }
    }

    public void printContracts() {
        List<Contract> contracts = sql(contractDao::findAll);
        System.out.println("\n--- Contracts (" + contracts.size() + ") ---");
        if (contracts.isEmpty()) {
            System.out.println("No contracts found.");
            return;
        }
        for (int i = 0; i < contracts.size(); i++) {
            System.out.println((i + 1) + ") " + contracts.get(i));
        }
    }

    public void printAuditLog() {
        if (!requireLogin()) return;
        if (!requirePermission(VIEW_AUDIT_LOG)) return;

        List<AuditEntry> entries = sql(auditDao::findAll);
        System.out.println("\n--- Audit Log (" + entries.size() + ") ---");
        if (entries.isEmpty()) {
            System.out.println("No audit entries found.");
            return;
        }
        for (AuditEntry entry : entries) {
            System.out.println(entry);
        }
    }

    public void printAuditLogForContract(int contractId) {
        if (!requireLogin()) return;
        if (!requirePermission(VIEW_AUDIT_LOG)) return;

        List<AuditEntry> entries = sql(() -> auditDao.findBySubject("CONTRACT", contractId));
        System.out.println("\n--- Audit Log for Contract #" + contractId + " (" + entries.size() + ") ---");
        if (entries.isEmpty()) {
            System.out.println("No audit entries found for this contract.");
            return;
        }
        for (AuditEntry entry : entries) {
            System.out.println(entry);
        }
    }

    // ===== Find Helpers =====
    private Contract findContractById(int contractId) {
        return sql(() -> contractDao.findById(contractId));
    }

    private Applicant findApplicantById(int applicantId) {
        return sql(() -> applicantDao.findById(applicantId));
    }

    private Staff findStaffById(int staffId) {
        return sql(() -> staffDao.findById(staffId));
    }

    private PaymentSchedule findScheduleByContractId(int contractId){
        return sql(() -> scheduleDao.findByContractId(contractId));
    }

    private boolean requireLogin() {
        if (loggedInUser == null) {
            setLastMessage("Action denied: please login first.");
            return false;
        }
        if (!loggedInUser.isActive()) {
            loggedInUser = null;
            setLastMessage("Action denied: User is inactive. Auto logout.");
            return false;
        }
        return true;
    }

    public  boolean checkIfUsernameAvailable(String username){
          boolean taken = sql(() -> staffDao.existsUsername(username)) || sql(() -> applicantDao.existsUsername(username));
          return !taken;
    }

    public  boolean checkIfPhoneNumerAvailable(String phoneNumber){
          boolean taken = sql(() -> staffDao.existsPhoneNumber(phoneNumber)) || sql(() -> applicantDao.existsPhoneNumber(phoneNumber));
          return !taken;
    }



    private boolean requirePermission(String action) {
        if (!loggedInUser.can(action)) {
            setLastMessage("Error: Permission denied for action: " + action);
            return false;
        }
        return true;
    }

    public boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "Bank: " + bankName +
               " | Interest Rate: " + (currentInterestRate * 100) + "%" +
               " | Staffs: " + sql(staffDao::findAll).size() +
               " | Applicants: " + sql(applicantDao::findAll).size() +
               " | Contracts: " + sql(contractDao::findAll).size();
    }

    // ===== SQL plumbing =====

    @FunctionalInterface
    private interface SqlSupplier<T> { T get() throws SQLException; }

    @FunctionalInterface
    private interface SqlAction { void run() throws SQLException; }

    private <T> T sql(SqlSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private void sql(SqlAction action) {
        try {
            action.run();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    // Runs a multi-step write as a single transaction so a failure partway through
    // (e.g. schedule insert failing after the contract row was already updated)
    // can't leave the database in an inconsistent state.
    private void transaction(SqlAction action) {
        try {
            connection.setAutoCommit(false);
            action.run();
            connection.commit();
        } catch (RuntimeException | SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                // Best-effort: the original failure is what matters to the caller.
            }
            if (e instanceof RuntimeException re) throw re;
            throw new DataAccessException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Connection is likely unusable at this point; nothing more we can do here.
            }
        }
    }
}
