package src.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import src.model.Contract;
import src.model.Payment;
import src.model.PaymentSchedule;

public class PaymentScheduleDao {

    private final Connection connection;
    private final ContractDao contractDao;

    public PaymentScheduleDao(Connection connection, ContractDao contractDao) {
        this.connection = connection;
        this.contractDao = contractDao;
    }

    public PaymentSchedule insert(PaymentSchedule schedule) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO payment_schedules (contract_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, schedule.getContract().getContractId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                schedule.setId(keys.getInt(1));
            }
        }

        String sql = "INSERT INTO payments (schedule_id, month_number, amount, interest_portion, principal_portion, remaining_balance, due_date, is_paid, paid_date, is_late, late_fee, amount_paid) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Payment payment : schedule.getPayments()) {
                ps.setInt(1, schedule.getScheduleId());
                ps.setInt(2, payment.getMonthNumber());
                ps.setString(3, payment.getAmount().toPlainString());
                ps.setString(4, payment.getInterestPortion().toPlainString());
                ps.setString(5, payment.getPrincipalPortion().toPlainString());
                ps.setString(6, payment.getRemainingBalance().toPlainString());
                ps.setString(7, payment.getDueDate().toString());
                ps.setInt(8, payment.isPaid() ? 1 : 0);
                ps.setString(9, payment.getPaidDate());
                ps.setInt(10, payment.isLate() ? 1 : 0);
                ps.setString(11, payment.getLateFee().toPlainString());
                ps.setString(12, payment.getAmountPaid().toPlainString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return schedule;
    }

    // Persists a payment's partial (or full) progress toward a month — the only mutation
    // payments undergo now that a month can take several payments to fully cover.
    public void updatePaymentProgress(int scheduleId, int monthNumber, BigDecimal amountPaid, boolean isPaid, String paidDate) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE payments SET amount_paid = ?, is_paid = ?, paid_date = ? WHERE schedule_id = ? AND month_number = ?")) {
            ps.setString(1, amountPaid.toPlainString());
            ps.setInt(2, isPaid ? 1 : 0);
            ps.setString(3, paidDate);
            ps.setInt(4, scheduleId);
            ps.setInt(5, monthNumber);
            ps.executeUpdate();
        }
    }

    // Persists a month flipping to late along with the fee that was applied.
    public void markLate(int scheduleId, int monthNumber, BigDecimal lateFee) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE payments SET is_late = 1, late_fee = ? WHERE schedule_id = ? AND month_number = ?")) {
            ps.setString(1, lateFee.toPlainString());
            ps.setInt(2, scheduleId);
            ps.setInt(3, monthNumber);
            ps.executeUpdate();
        }
    }

    public PaymentSchedule findByContractId(int contractId) throws SQLException {
        int scheduleId;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM payment_schedules WHERE contract_id = ?")) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                scheduleId = rs.getInt("id");
            }
        }

        Contract contract = contractDao.findById(contractId);
        PaymentSchedule schedule = new PaymentSchedule(contract);
        schedule.setId(scheduleId);

        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM payments WHERE schedule_id = ? ORDER BY month_number")) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment payment = new Payment(
                            rs.getInt("month_number"),
                            new BigDecimal(rs.getString("amount")),
                            new BigDecimal(rs.getString("interest_portion")),
                            new BigDecimal(rs.getString("principal_portion")),
                            new BigDecimal(rs.getString("remaining_balance")),
                            LocalDate.parse(rs.getString("due_date")));
                    payment.hydratePaidState(rs.getInt("is_paid") == 1, rs.getString("paid_date"));
                    payment.hydrateLateState(rs.getInt("is_late") == 1, new BigDecimal(rs.getString("late_fee")));
                    payment.hydrateAmountPaid(new BigDecimal(rs.getString("amount_paid")));
                    payments.add(payment);
                }
            }
        }
        schedule.setPayments(payments);
        return schedule;
    }

    public List<PaymentSchedule> findAll() throws SQLException {
        List<PaymentSchedule> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT contract_id FROM payment_schedules ORDER BY id")) {
            while (rs.next()) {
                result.add(findByContractId(rs.getInt("contract_id")));
            }
        }
        return result;
    }
}
