package src.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class Contract {

   public static final String APPROVED="APPROVED";
   public static final String PENDING="PENDING";
   public static final String REJECTED="REJECTED";
   public static final String ACTIVE="ACTIVE";
   public static final String CLOSED="CLOSED";
   public static final String FORWARDED="FORWARDED";
   public static final String DEFAULTED="DEFAULTED";


    private int contractId;

    private Applicant applicant;
    private BigDecimal principalAmount;   // original loan amount
    private BigDecimal totalAmount;       // after compound interest
    private int duration;
    private double interestRate;

    // Staff involvement
    private Staff approvingOfficer;  // set AFTER creation
    private Staff draftingOfficer;   // set AFTER creation
    private ArrayList<Staff> coSigners;
    private ArrayList<Integer> votedCommitteeIds;

    // Status
    private String status;  // "PENDING", "APPROVED", "REJECTED", "ACTIVE", "CLOSED"
    private static final int MAX_COSIGNERS = 3;


    public Contract(Applicant applicant, BigDecimal amount, int duration, double interestRate) {
    setApplicant(applicant);
    setPrincipalAmount(amount);
    setDuration(duration);
    this.interestRate = interestRate;
    this.totalAmount = calculateTotal();   // runs ONCE here, never again
    this.coSigners = new ArrayList<>();
    this.votedCommitteeIds = new ArrayList<>();
    this.status =PENDING;
  }


  private BigDecimal calculateTotal(){
    int totalMonths = duration * 12;
    BigDecimal total = BigDecimal.ZERO;
    for (Amortization.Entry entry : Amortization.schedule(principalAmount, interestRate, totalMonths)) {
        total = total.add(entry.getPayment());
    }
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  // The nominal rate alone understates true cost/return for a reducing-balance loan: the
  // borrower only ever owes the full principal for the first instant, so total interest paid
  // is much less than principal * nominalRate * years. This annualizes actual interest paid
  // against principal, giving the disclosure figure that reflects what the loan really costs.
  public BigDecimal getEffectiveApr(){
    return totalAmount.subtract(principalAmount)
            .divide(principalAmount, 4, RoundingMode.HALF_UP)
            .divide(BigDecimal.valueOf(duration), 4, RoundingMode.HALF_UP);
  }

  public int getContractId()        { return contractId; }
  public Applicant getApplicant()   { return applicant; }
  public BigDecimal getPrincipalAmount(){ return principalAmount; }
  public BigDecimal getTotalAmount()    { return totalAmount; }
  public int getDuration()          { return duration; }
  public double getInterestRate()   { return interestRate; }
  public String getStatus()         { return status; }
  public Staff getApprovingOfficer(){ return approvingOfficer; }
  public Staff getDraftingOfficer() { return draftingOfficer; }

  // Assigned by ContractDao once the row is inserted (or when hydrating from an existing row) —
  // ids are database-generated now, not a static in-process counter.
  public void setId(int id){
      this.contractId=id;
  }




  

  public void setApplicant(Applicant applicant) {
     if(applicant == null){
        throw new IllegalArgumentException("Error: Applicant cannot be null.");        
     }
    this.applicant = applicant;
}


  public void setPrincipalAmount(BigDecimal principalAmount) {
    if(principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0){
        throw new IllegalArgumentException("Amount should be > 0 ");
    }
    this.principalAmount = principalAmount.setScale(2, RoundingMode.HALF_UP);
  }


  public void setDuration(int duration) {
    if(duration <=0 ){
      throw new IllegalArgumentException("Duration should be > 0");
    }
    this.duration = duration;
  }


// will review later
  // Called by LoaningSystem after officer reviews
public void setApprovingOfficer(Staff officer) {
    if (officer == null) return;
    this.approvingOfficer = officer;
}

// will review later
// Called by LoaningSystem after legal drafts it
public void setDraftingOfficer(Staff officer) {
    if (officer == null) return;
    this.draftingOfficer = officer;
}


// Called by LoaningSystem when status changes
public void setStatus(String status) {
    if (status == null || status.isBlank()) return;
    
    this.status = status;
}

public boolean addCoSigner(Staff signer) {
    if (signer == null) return false;
    if (coSigners.size() >= MAX_COSIGNERS) {
        System.out.println("Max co-signers reached.");
        return false;
    }
    coSigners.add(signer);
    return true;
}

public boolean isApproved() {
    return status.equals("APPROVED");
}

public ArrayList<Staff> getCoSigners() {
    return coSigners;
}

// Records a distinct CreditCommittee member's vote; returns false if that member already voted
public boolean addCommitteeVote(int committeeStaffId) {
    if (votedCommitteeIds.contains(committeeStaffId)) {
        return false;
    }
    votedCommitteeIds.add(committeeStaffId);
    return true;
}

public int getCommitteeVoteCount() {
    return votedCommitteeIds.size();
}

@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Contract)) return false;
    Contract other = (Contract) obj;

    return this.contractId == other.contractId;
}

@Override
public int hashCode() {
    return Integer.hashCode(contractId);
}

@Override
public String toString() {
    return "Contract #" + contractId +
           " | Applicant: " + applicant.getName() +
           " | Principal: $" + String.format("%.2f", principalAmount) +
           " | Total: $" + String.format("%.2f", totalAmount) +
           " | Duration: " + duration + " yrs" +
           " | Nominal Rate: " + (interestRate * 100) + "%" +
           " | Effective APR: " + getEffectiveApr().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%" +
           " | Status: " + status +
           " | Check By: " + (approvingOfficer != null ? approvingOfficer.getPosition() + " " + approvingOfficer.getName() : "Pending");
}

}
