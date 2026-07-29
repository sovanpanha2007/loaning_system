package src.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import src.model.Applicant;

public class ApplicantDao {

    private final Connection connection;

    public ApplicantDao(Connection connection) {
        this.connection = connection;
    }

    public Applicant insert(Applicant applicant) throws SQLException {
        String sql = "INSERT INTO applicants (name, username, phone_number, password_hash, gender, salary, age, active, account_balance, existing_external_debt) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, applicant.getName());
            ps.setString(2, applicant.getUsername());
            ps.setString(3, applicant.getPhoneNumber());
            ps.setString(4, applicant.getPasswordHash());
            ps.setString(5, applicant.getGender());
            ps.setString(6, applicant.getSalary().toPlainString());
            ps.setInt(7, applicant.getAge());
            ps.setInt(8, applicant.isActive() ? 1 : 0);
            ps.setString(9, applicant.getBalance().toPlainString());
            ps.setString(10, applicant.getExistingExternalDebt().toPlainString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                applicant.setId(keys.getInt(1));
            }
        }
        return applicant;
    }

    public void update(Applicant applicant) throws SQLException {
        String sql = "UPDATE applicants SET name=?, username=?, phone_number=?, active=?, account_balance=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, applicant.getName());
            ps.setString(2, applicant.getUsername());
            ps.setString(3, applicant.getPhoneNumber());
            ps.setInt(4, applicant.isActive() ? 1 : 0);
            ps.setString(5, applicant.getBalance().toPlainString());
            ps.setInt(6, applicant.getId());
            ps.executeUpdate();
        }
    }

    public Applicant findById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM applicants WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Applicant findByUsername(String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM applicants WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Applicant> findAll() throws SQLException {
        List<Applicant> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM applicants ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public boolean existsUsername(String username) throws SQLException {
        return exists("SELECT 1 FROM applicants WHERE username = ?", username);
    }

    public boolean existsPhoneNumber(String phoneNumber) throws SQLException {
        return exists("SELECT 1 FROM applicants WHERE phone_number = ?", phoneNumber);
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Applicant mapRow(ResultSet rs) throws SQLException {
        Applicant applicant = new Applicant(
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("phone_number"),
                "placeholder",
                rs.getString("gender"),
                new BigDecimal(rs.getString("salary")),
                rs.getInt("age"),
                new BigDecimal(rs.getString("existing_external_debt")));
        applicant.setId(rs.getInt("id"));
        applicant.setPasswordHash(rs.getString("password_hash"));
        applicant.setActive(rs.getInt("active") == 1);
        applicant.setBalance(new BigDecimal(rs.getString("account_balance")));
        return applicant;
    }
}
