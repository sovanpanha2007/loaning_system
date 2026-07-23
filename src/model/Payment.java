package src.model;

public class Payment {
    private int monthId;
    private boolean isPaid;
    private double amount;
    private String paidDate;

    public Payment(int monthId ,double amount){
        this.monthId=monthId;
        this.amount=amount;
        isPaid=false;
        this.paidDate=null;
    }

    public int getMonthNumber() {
        return monthId;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public double getAmount() {
        return amount;
    }

    public void setPaid(boolean isPaid) {
        this.isPaid = isPaid;
         if(isPaid){
           paidDate=java.time.LocalDate.now().toString();
         }
    }

    
}
