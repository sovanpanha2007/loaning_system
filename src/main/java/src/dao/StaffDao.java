package src.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import src.controller.LoaningSystem;
import src.model.CreditCommittee;
import src.model.LoanOfficer;
import src.model.Manager;
import src.model.Staff;

public class StaffDao {

    private final Connection connection;

    public StaffDao(Connection connection) {
        this.connection = connection;
    }

    public Staff insert(Staff staff) throws SQLException {
        String sql = "INSERT INTO staff (role, name, username, phone_number, age, password_hash, active, salary, position, access_level, max_approval_limit) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String role = staff.getPosition();
            ps.setString(1, role);
            ps.setString(2, staff.getName());
            ps.setString(3, staff.getUsername());
            ps.setString(4, staff.getPhoneNumber());
            ps.setInt(5, staff.getAge());
            ps.setString(6, staff.getPasswordHash());
            ps.setInt(7, staff.isActive() ? 1 : 0);
            ps.setDouble(8, staff.getSalary());
            ps.setString(9, staff.getPosition());
            if (staff instanceof Manager manager) {
                ps.setInt(10, manager.getAccessLevel());
            } else {
                ps.setNull(10, java.sql.Types.INTEGER);
            }
            if (staff instanceof LoanOfficer officer) {
                ps.setString(11, officer.getMaxApprovalLimit().toPlainString());
            } else {
                ps.setNull(11, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                staff.setId(keys.getInt(1));
            }
        }
        return staff;
    }

    public void update(Staff staff) throws SQLException {
        String sql = "UPDATE staff SET name=?, username=?, phone_number=?, active=?, salary=?, access_level=?, max_approval_limit=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, staff.getName());
            ps.setString(2, staff.getUsername());
            ps.setString(3, staff.getPhoneNumber());
            ps.setInt(4, staff.isActive() ? 1 : 0);
            ps.setDouble(5, staff.getSalary());
            if (staff instanceof Manager manager) {
                ps.setInt(6, manager.getAccessLevel());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            if (staff instanceof LoanOfficer officer) {
                ps.setString(7, officer.getMaxApprovalLimit().toPlainString());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            ps.setInt(8, staff.getId());
            ps.executeUpdate();
        }
    }

    public Staff findById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM staff WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Staff findByUsername(String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM staff WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Staff> findAll() throws SQLException {
        List<Staff> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM staff ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public boolean existsUsername(String username) throws SQLException {
        return exists("SELECT 1 FROM staff WHERE username = ?", username);
    }

    public boolean existsPhoneNumber(String phoneNumber) throws SQLException {
        return exists("SELECT 1 FROM staff WHERE phone_number = ?", phoneNumber);
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Staff mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String name = rs.getString("name");
        String username = rs.getString("username");
        String phoneNumber = rs.getString("phone_number");
        int age = rs.getInt("age");
        double salary = rs.getDouble("salary");

        Staff staff;
        if (LoaningSystem.MANAGER.equals(role)) {
            staff = new Manager(name, username, phoneNumber, age, "placeholder", salary, rs.getInt("access_level"));
        } else if (LoaningSystem.LOAN_OFFICER.equals(role)) {
            staff = new LoanOfficer(name, username, phoneNumber, age, "placeholder", salary, new BigDecimal(rs.getString("max_approval_limit")));
        } else if (LoaningSystem.CREDIT_COMMITTEE.equals(role)) {
            staff = new CreditCommittee(name, username, phoneNumber, age, "placeholder", salary);
        } else {
            throw new IllegalStateException("Unknown staff role in database: " + role);
        }

        staff.setId(rs.getInt("id"));
        staff.setPasswordHash(rs.getString("password_hash"));
        staff.setActive(rs.getInt("active") == 1);
        return staff;
    }
}
