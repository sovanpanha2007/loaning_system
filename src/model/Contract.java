package src.model;
import java.util.ArrayList;

public class Contract {

   public static final String APPROVED="APPROVED";
   public static final String PENDING="PENDING";
   public static final String REJECTED="REJECTED";
   public static final String ACTIVE="ACTIVE";
   public static final String CLOSED="CLOSED";
   public static final String FORWARDED="FORWARDED";


    private static int indexID = 1;
    private int contractId;

    private Applicant applicant;
    private double principalAmount;   // original loan amount
    private double totalAmount;       // after compound interest
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


    public Contract(Applicant applicant, double amount, int duration, double interestRate) {
    this.contractId = indexID++;
    setApplicant(applicant);
    setPrincipalAmount(amount);
    setDuration(duration);
    this.interestRate = interestRate;
    this.totalAmount = calculateTotal();   // runs ONCE here, never again
    this.coSigners = new ArrayList<>();
    this.votedCommitteeIds = new ArrayList<>();
    this.status =PENDING;
  }


  private double calculateTotal(){
    return principalAmount * Math.pow(1+ interestRate , duration);
  }

  public int getContractId()        { return contractId; }
  public Applicant getApplicant()   { return applicant; }
  public double getPrincipalAmount(){ return principalAmount; }
  public double getTotalAmount()    { return totalAmount; }
  public int getDuration()          { return duration; }
  public double getInterestRate()   { return interestRate; }
  public String getStatus()         { return status; }




  

  public void setApplicant(Applicant applicant) {
     if(applicant == null){
        throw new IllegalArgumentException("Error: Applicant cannot be null.");        
     }
    this.applicant = applicant;
}


  public void setPrincipalAmount(double principalAmount) {
    if(principalAmount <=0){
        throw new IllegalArgumentException("Amount should be > 0 ");
    }
    this.principalAmount = principalAmount;
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

// will check later
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Contract)) return false;
    Contract other = (Contract) obj;
    
    return this.applicant.getName().equals(other.applicant.getName()) &&
           this.principalAmount == other.principalAmount &&
           this.duration == other.duration;
}

@Override
public String toString() {
    return "Contract #" + contractId +
           " | Applicant: " + applicant.getName() +
           " | Principal: $" + String.format("%.2f", principalAmount) +
           " | Total: $" + String.format("%.2f", totalAmount) +
           " | Duration: " + duration + " yrs" +
           " | Rate: " + (interestRate * 100) + "%" +
           " | Status: " + status +
           " | Check By: " + (approvingOfficer != null ? approvingOfficer.getPosition() + " " + approvingOfficer.getName() : "Pending");
}

}
