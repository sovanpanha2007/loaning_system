package src.model;

import java.util.ArrayList;

public class PaymentSchedule {

    private static int indexId = 1;
    private int scheduleId;
    private Contract contract;
    private ArrayList<Payment> payments;  

    public PaymentSchedule(Contract contract) {
        this.scheduleId = indexId++;
        this.contract = contract;
        this.payments = new ArrayList<>();
        generatePayments();  
    }

    private void generatePayments() {
        int totalMonths = contract.getDuration() * 12;
        double monthlyAmount = contract.getTotalAmount() / totalMonths;

        for (int i = 1; i <= totalMonths; i++) {
            payments.add(new Payment(i, monthlyAmount));
        }
    }

    public double getMonthlyPayment(){
         int totalMonths = contract.getDuration() * 12;
         double  monthlyAmount = contract.getTotalAmount() / totalMonths;

        return monthlyAmount;
    }

    public boolean  payMonth(int monthNumber , double applicantBalance) {
        for (int i = 0 ; i < payments.size(); i++) {
            Payment p = payments.get(i);
            if (p.getMonthNumber() == monthNumber) {
                if (p.isPaid()) {
                    System.out.println("Error: Month " + monthNumber + " already paid.");
                    return false;
                }
                if(applicantBalance >= p.getAmount()){
                p.setPaid(true);
                System.out.println("Payment successful for month: " + monthNumber);
                return true;
                }
                System.out.println("Error: Insufficient balance to pay month " + monthNumber + ".");
                return false;
            }
        }
        System.out.println("Error: Month not found.");
        return false;
    }

    public void printSchedule() {
        System.out.println("\n===== Payment Schedule #" + scheduleId + " =====");
        System.out.println("Contract ID : " + contract.getContractId());
        System.out.println("Applicant   : " + contract.getApplicant().getName());
        System.out.println("Total Amount: $" + String.format("%.2f", contract.getTotalAmount()));
        System.out.println("------------------------------------------");
        for (int i =getPaidCount() ; i < payments.size(); i++) {
            System.out.println("Month " + (i+1) + " :" + String.format("%.2f", payments.get(i).getAmount()) + "$");
        }
        System.out.println("------------------------------------------");
        System.out.println("Paid    : " + getPaidCount() + " months");
        System.out.println("Remaining: " + getRemainingCount() + " months");
        System.out.println("==========================================");
    }

    public int getPaidCount() {
        int count = 0;
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).isPaid()) count++;
        }
        return count;
    }

    public int getRemainingCount() {
        return payments.size() - getPaidCount();
    }

    public boolean isFullyPaid() {
        return getRemainingCount() == 0;
    }

    public int getScheduleId()      { return scheduleId; }
    public Contract getContract()   { return contract; }
}