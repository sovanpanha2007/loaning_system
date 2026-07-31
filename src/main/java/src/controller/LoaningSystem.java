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

// Business-logic layer, shared by the CLI (Main) and the web layer. Every method that acts
// on behalf of a user takes that user in explicitly (ILoginable actingUser) rather than
// reading stored session state, and reports failure by throwing instead of printing, so a
// single LoaningSystem instance is safe to share across many callers/threads: it holds no
// per-caller state, and DAOs are always used against a connection borrowed for the duration
// of one call (see sql()/transaction()), never one held open across calls.
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

    private String lastMessage;

    // A single SQLite Connection is not safe for concurrent use from multiple threads, and
    // SQLite itself only ever allows one writer at a time — so rather than pool connections,
    // every DB access (sql()/transaction(), see below) is serialized through this one
    // reentrant lock. createContract/approveContract/makePayment/checkDelinquency additionally
    // hold it across their whole check-then-write sequence (not just the write), so two
    // threads can't both pass a business-rule check (borrowing limit, "not already approved",
    // "not already paid") before either one's write lands; being reentrant, the sql()/
    // transaction() calls made inside those sequences don't deadlock against the outer hold.
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

        this.lastMessage = "";

        seedDefaultAdminIfEmpty();
    }

    public String getBankName()                  { return bankName; }
    public double getCurrentInterestRate()        { return currentInterestRate; }
    public int getRequiredCommitteeVotes(){ return requiredCommitteeVotes; }
    public double getMaxDebtToIncomeRatio(){ return maxDebtToIncomeRatio; }
    public String getLastMessage()               { return lastMessage; }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public ILoginable getMyProfile(ILoginable actingUser) {
        requireLogin(actingUser);
        return actingUser;
    }

    public BigDecimal getMyBalance(ILoginable actingUser) {
        requirePermission(actingUser, VIEW_OWN_BALANCE);
        Applicant applicant = findApplicantById(actingUser.getId());
        return applicant.getBalance();
    }

    public Applicant addBalanceforApplicant(ILoginable actingUser, int applicantId, int amount) {
        requirePermission(actingUser, ADD_BALANCE);

        Applicant applicant = findApplicantById(applicantId);
        if (applicant == null) {
            throw new NotFoundException("Error : Invalid id applicant not found");
        }
        Manager manager = (Manager) actingUser;
        manager.setBalanceApplicant(applicant, BigDecimal.valueOf(amount));
        // No real payment rail exists to integrate here, so this stays a manually-recorded
        // entry — but it's now a traceable ledger entry (actor, amount, timestamp) via the
        // audit log instead of a bare, unexplained balance mutation.
        transaction(() -> {
            applicantDao.update(applicant);
            auditDao.record(actingUser, "DEPOSIT", "APPLICANT", applicantId, "Amount: $" + amount + ", recorded by " + actingUser.getName());
        });
        setLastMessage("Successfully add money into " + applicant.getName() + " balance");
        return applicant;
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
            throw new IllegalArgumentException("Error: Interest rate must be between 0 and 1 (e.g. 0.05 for 5%).");
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

    public List<Contract> getMyContracts(ILoginable actingUser) {
        requirePermission(actingUser, VIEW_OWN_CONTRACT);
        return sql(() -> contractDao.findByApplicantId(actingUser.getId()));
    }

    // Pure lookup + credential check; mutates nothing on this instance. Records a LOGIN
    // audit row on success since that's a fact about what happened, not session state.
    public ILoginable authenticate(String name, String password) {
        if (isBlank(name) || isBlank(password)) {
            throw new IllegalArgumentException("Error: Name or password cannot be empty.");
        }

        Staff s = sql(() -> staffDao.findByUsername(name.trim()));
        if (s != null) {
            if (!s.isActive()) {
                throw new LogginException("Error : User account is inactive");
            }
            if (!s.checkPassword(password)) {
                throw new LogginException("Error : Wrong Password");
            }
            sql(() -> auditDao.record(s, "LOGIN", null, null, null));
            return s;
        }

        Applicant a = sql(() -> applicantDao.findByUsername(name.trim()));
        if (a != null) {
            if (!a.isActive()) {
                throw new LogginException("Error : User account is inactive");
            }
            if (!a.checkPassword(password)) {
                throw new LogginException("Error : Wrong Password");
            }
            sql(() -> auditDao.record(a, "LOGIN", null, null, null));
            return a;
        }
        throw new LogginException("Error : User not found");
    }

    public Staff createStaff(ILoginable actingUser, String name,String userName , String phoneNumber, int age, String password, double salary, String position) {
        requirePermission(actingUser, CREATE_STAFF);

        if (isBlank(name) || isBlank(password)) {
            throw new IllegalArgumentException("Error: Name or password cannot be empty.");
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
            auditDao.record(actingUser, "CREATE_STAFF", "STAFF", newStaff.getId(), newStaff.getName() + " (" + position + ")");
        });
        setLastMessage("Staff created successfully: " + newStaff.getName() + " | Role: " + position);
        return newStaff;
    }

    public Applicant createApplicant(ILoginable actingUser, String name,String userName , String phoneNumber,String password, int age, int income, String gender, double existingExternalDebt) {
        requirePermission(actingUser, CREATE_APPLICANT);

        if (isBlank(name)) {
            throw new IllegalArgumentException("Error: Applicant name cannot be empty.");
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
            auditDao.record(actingUser, "CREATE_APPLICANT", "APPLICANT", applicant.getId(), applicant.getName());
        });
        setLastMessage("Applicant created successfully: " + name);
        return applicant;
    }

    public Contract createContract(ILoginable actingUser, int applicantId, double amount, int duration) {
        requirePermission(actingUser, CREATE_CONTRACT);

        // An Applicant can only ever be requesting a loan for themselves — without this check,
        // any authenticated applicant could pass an arbitrary applicantId (an IDOR) and open a
        // contract against a completely different applicant's DTI capacity. Staff (LoanOfficer)
        // legitimately draft contracts on behalf of any applicant, so this only restricts the
        // Applicant caller, not staff.
        if (actingUser instanceof Applicant && actingUser.getId() != applicantId) {
            throw new IllegalArgumentException("Error: You can only create a contract for yourself.");
        }

        writeLock.lock();
        try {
            Applicant applicant = findApplicantById(applicantId);
            if (applicant == null) {
                throw new NotFoundException("Applicant ID "+ applicantId + " doesn't exist");
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
            if (actingUser instanceof Staff) {
                contract.setDraftingOfficer((Staff) actingUser);
            }
            transaction(() -> {
                contractDao.insert(contract);
                auditDao.record(actingUser, "CREATE_CONTRACT", "CONTRACT", contract.getContractId(),
                        "Principal: " + principal + ", Applicant: " + applicant.getId());
            });
            setLastMessage("Contract created successfully. ID: " + contract.getContractId());
            return contract;
        } finally {
            writeLock.unlock();
        }
    }

    public Contract approveContract(ILoginable actingUser, int contractId) {
        requirePermission(actingUser, APPROVE_LOAN);

        writeLock.lock();
        try {
            Contract contract = findContractById(contractId);
            if (contract == null) {
                throw new NotFoundException("Error: Contract not found.");
            }
            if (contract.isApproved()) {
                throw new IllegalArgumentException("Error: Contract is already approved.");
            }
            if (!contract.getStatus().equals("PENDING") && !contract.getStatus().equals("FORWARDED")) {
                throw new IllegalArgumentException("Error: Contract cannot be approved at status: " + contract.getStatus());
            }
            Staff officer = (Staff) actingUser;
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
            return contract;
        } finally {
            writeLock.unlock();
        }
    }


    public PaymentSchedule.PaymentResult makePayment(ILoginable actingUser, int contractId , double amount){
        requirePermission(actingUser, MAKE_PAYMENT);

        writeLock.lock();
        try {
            PaymentSchedule schedule = findScheduleByContractId(contractId);
            if (schedule == null) {
                throw new NotFoundException("Error: No payment schedule found for this contract.");
            }

            // Re-fetched by ID rather than cast from actingUser: actingUser may be a session
            // principal held across many calls (a CLI's currentUser, or a web session's cached
            // principal), and its cached balance can be stale if money moved since it was
            // fetched — reading/mutating a freshly-loaded row keeps this call correct
            // regardless of how long the caller has been holding onto actingUser.
            Applicant applicant = findApplicantById(actingUser.getId());
            if (applicant == null || schedule.getContract().getApplicant().getId() != applicant.getId()) {
                throw new IllegalArgumentException("Error: This is not your contract.");
            }
            if (schedule.isFullyPaid()) {
                throw new IllegalArgumentException("Error: This contract is already fully paid.");
            }

            BigDecimal requested = BigDecimal.valueOf(amount);
            if (requested.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Error: Payment amount must be greater than 0.");
            }
            if (requested.compareTo(applicant.getBalance()) > 0) {
                throw new IllegalArgumentException("Error: Insufficient balance to make this payment.");
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
            return result;
        } finally {
            writeLock.unlock();
        }
    }


    public PaymentSchedule getMySchedule(ILoginable actingUser, int contractId){
        requirePermission(actingUser, VIEW_OWN_SCHEDULE);

        PaymentSchedule schedule = findScheduleByContractId(contractId);
        if (schedule == null) {
            throw new NotFoundException("Error: No payment schedule found for this contract.");
        }

        if (schedule.getContract().getApplicant().getId() != actingUser.getId()) {
            throw new IllegalArgumentException("Error: This is not your contract.");
        }

        return schedule;
    }

    // Small result holder so checkDelinquency reports what happened without printing.
    public static final class DelinquencyResult {
        private final int flaggedLate;
        private final int defaulted;

        DelinquencyResult(int flaggedLate, int defaulted) {
            this.flaggedLate = flaggedLate;
            this.defaulted = defaulted;
        }

        public int getFlaggedLate() { return flaggedLate; }
        public int getDefaulted() { return defaulted; }
    }

    public DelinquencyResult checkDelinquency(ILoginable actingUser) {
        return checkDelinquency(actingUser, LocalDate.now());
    }

    // Package-visible overload so tests can drive delinquency checks against a fixed
    // date instead of depending on the wall clock.
    DelinquencyResult checkDelinquency(ILoginable actingUser, LocalDate asOf) {
        requirePermission(actingUser, CHECK_DELINQUENCY);

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
                        auditDao.record(actingUser, "CONTRACT_DEFAULTED", "CONTRACT", contract.getContractId(),
                                schedule.getConsecutiveLateCount() + " consecutive late payments");
                    }
                });
            }

            return new DelinquencyResult(flaggedLate, defaulted);
        } finally {
            writeLock.unlock();
        }
    }


    public Contract rejectContract(ILoginable actingUser, int contractId) {
        requirePermission(actingUser, REJECT_LOAN);

        // Held across the whole check-then-write, same as approveContract: without this, a
        // reject could read the contract as not-yet-approved, then — after a concurrent
        // approveContract lands in between — unconditionally overwrite its status back to
        // REJECTED, silently clobbering a completed approval (and its already-generated
        // payment schedule).
        writeLock.lock();
        try {
            Contract contract = findContractById(contractId);
            if (contract == null) {
                throw new NotFoundException("Error: Contract not found.");
            }
            if (contract.isApproved()) {
                throw new IllegalArgumentException("Error: Cannot reject an already approved contract.");
            }

            contract.setStatus("REJECTED");
            transaction(() -> {
                contractDao.save(contract);
                auditDao.record(actingUser, "REJECT_CONTRACT", "CONTRACT", contractId, "by " + actingUser.getName());
            });
            setLastMessage("Contract #" + contractId + " rejected by: " + actingUser.getName());
            return contract;
        } finally {
            writeLock.unlock();
        }
    }

    // ===== Add CoSigner =====
    public Contract addCoSigner(ILoginable actingUser, int contractId, int staffId) {
        requirePermission(actingUser, ADD_COSIGNER);

        Contract contract = findContractById(contractId);
        if (contract == null) {
            throw new NotFoundException("Error: Contract not found.");
        }

        Staff signer = findStaffById(staffId);
        if (signer == null) {
            throw new NotFoundException("Error: Staff not found.");
        }

        if (!(signer instanceof CreditCommittee)) {
            throw new IllegalArgumentException("Error: Only CreditCommittee  can co-sign a contract.");
        }

        boolean added = contract.addCoSigner(signer);
        if (!added) {
            throw new IllegalArgumentException("Error: Could not add co-signer.");
        }

        transaction(() -> {
            contractDao.addCoSigner(contractId, signer.getId());
            auditDao.record(actingUser, "ADD_COSIGNER", "CONTRACT", contractId, signer.getName());
        });
        setLastMessage("Co-signer added successfully: " + signer.getName());
        return contract;
    }

    public Staff deactivateStaff(ILoginable actingUser, int staffId) {
        requirePermission(actingUser, CREATE_STAFF);

        Staff staff = findStaffById(staffId);
        if (staff == null) {
            throw new NotFoundException("Error: Staff not found.");
        }

        staff.setActive(false);
        transaction(() -> {
            staffDao.update(staff);
            auditDao.record(actingUser, "DEACTIVATE_STAFF", "STAFF", staffId, staff.getName());
        });
        setLastMessage("Staff deactivated: " + staff.getName());
        return staff;
    }


    public LoanOfficer setNewApprovalLimit(ILoginable actingUser, int loanOfficerId , double newAmount){
        requirePermission(actingUser, SET_NEW_APVL);

        Staff staff = findStaffById(loanOfficerId);
        if (staff == null) {
            throw new NotFoundException("Error : No staff found");
        }

        if (!(staff instanceof LoanOfficer)) {
            throw new IllegalArgumentException("Error : staff is not a Loan Officerr");
        }

        LoanOfficer officer = (LoanOfficer) staff;
        officer.setMaxApprovalLimit(BigDecimal.valueOf(newAmount));
        transaction(() -> {
            staffDao.update(officer);
            auditDao.record(actingUser, "SET_APPROVAL_LIMIT", "STAFF", loanOfficerId, "New limit: $" + newAmount);
        });
        setLastMessage("Sucessfully set new approval limit for "+ officer.getName());
        return officer;
    }

    public int setNewRequiredVotes(ILoginable actingUser, int votes){
        requirePermission(actingUser, SET_NEW_REQV);

        if (votes <= 0) {
            throw new IllegalArgumentException("Error : Required votes must be at least 1");
        }

        this.requiredCommitteeVotes = votes;
        sql(() -> auditDao.record(actingUser, "SET_REQUIRED_VOTES", null, null, "New required votes: " + votes));
        setLastMessage("Successfully set required committee votes to " + votes);
        return requiredCommitteeVotes;
    }

    public double setNewMaxDebtToIncomeRatio(ILoginable actingUser, double ratio){
        requirePermission(actingUser, SET_NEW_DTI);

        if (ratio <= 0 || ratio > 1) {
            throw new IllegalArgumentException("Error : Debt-to-income ratio must be between 0 and 1 (e.g. 0.40 for 40%)");
        }

        this.maxDebtToIncomeRatio = ratio;
        sql(() -> auditDao.record(actingUser, "SET_MAX_DTI", null, null, "New max debt-to-income ratio: " + ratio));
        setLastMessage("Successfully set max debt-to-income ratio to " + (ratio * 100) + "%");
        return maxDebtToIncomeRatio;
    }

    public ILoginable setNewUserName(ILoginable actingUser, String username ,String newUsername ,  String password){
        requirePermission(actingUser, SET_NEW_NAME);

        if (actingUser.getUsername().equalsIgnoreCase(username) && actingUser.checkPassword(password)) {
            if (!checkIfUsernameAvailable(newUsername)) {
                throw new InputMismatchException("Error : username already taken");
            }
            actingUser.setUsername(newUsername);
            persistUser(actingUser, "CHANGE_USERNAME", "New username: " + newUsername);
            setLastMessage("Successfully change username ");
            return actingUser;
        }
        throw new InputMismatchException("Error : Authentication failed");
    }

    public void setNewPassword(ILoginable actingUser, String name , String password , String  newPassword){
        requirePermission(actingUser, SET_NEW_PASSWORD);

        if (actingUser.getUsername().equals(name) && actingUser.checkPassword(password)) {
            actingUser.setPassword(newPassword);
            persistUser(actingUser, "CHANGE_PASSWORD", null);
            setLastMessage("Successfully change your password");
            return;
        }
        throw new InputMismatchException("Error : Authentication failed");
    }

    private void persistUser(ILoginable user, String action, String details) {
        transaction(() -> {
            if (user instanceof Staff staff) {
                staffDao.update(staff);
            } else if (user instanceof Applicant applicant) {
                applicantDao.update(applicant);
            }
            auditDao.record(user, action, null, null, details);
        });
    }


    public List<Staff> getAllStaff() {
        return sql(staffDao::findAll);
    }

    public List<Applicant> getAllApplicants() {
        return sql(applicantDao::findAll);
    }

    public List<Contract> getAllContracts() {
        return sql(contractDao::findAll);
    }

    public List<AuditEntry> getAuditLog(ILoginable actingUser) {
        requirePermission(actingUser, VIEW_AUDIT_LOG);
        return sql(auditDao::findAll);
    }

    public List<AuditEntry> getAuditLogForContract(ILoginable actingUser, int contractId) {
        requirePermission(actingUser, VIEW_AUDIT_LOG);
        return sql(() -> auditDao.findBySubject("CONTRACT", contractId));
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

    private void requireLogin(ILoginable actingUser) {
        if (actingUser == null) {
            throw new LogginException("Action denied: please login first.");
        }
        if (!actingUser.isActive()) {
            throw new LogginException("Action denied: User is inactive.");
        }
    }

    public  boolean checkIfUsernameAvailable(String username){
          boolean taken = sql(() -> staffDao.existsUsername(username)) || sql(() -> applicantDao.existsUsername(username));
          return !taken;
    }

    public  boolean checkIfPhoneNumerAvailable(String phoneNumber){
          boolean taken = sql(() -> staffDao.existsPhoneNumber(phoneNumber)) || sql(() -> applicantDao.existsPhoneNumber(phoneNumber));
          return !taken;
    }



    private void requirePermission(ILoginable actingUser, String action) {
        requireLogin(actingUser);
        if (!actingUser.can(action)) {
            throw new LogginException("Error: Permission denied for action: " + action);
        }
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
        writeLock.lock();
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            writeLock.unlock();
        }
    }

    private void sql(SqlAction action) {
        writeLock.lock();
        try {
            action.run();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            writeLock.unlock();
        }
    }

    // Runs a multi-step write as a single transaction so a failure partway through
    // (e.g. schedule insert failing after the contract row was already updated)
    // can't leave the database in an inconsistent state.
    private void transaction(SqlAction action) {
        writeLock.lock();
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
            writeLock.unlock();
        }
    }
}
