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
import src.model.Contract;
import src.model.Staff;

public class ContractDao {

    private final Connection connection;
    private final ApplicantDao applicantDao;
    private final StaffDao staffDao;

    public ContractDao(Connection connection, ApplicantDao applicantDao, StaffDao staffDao) {
        this.connection = connection;
        this.applicantDao = applicantDao;
        this.staffDao = staffDao;
    }

    public Contract insert(Contract contract) throws SQLException {
        String sql = "INSERT INTO contracts (applicant_id, principal_amount, total_amount, duration, interest_rate, status, approving_officer_id, drafting_officer_id) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMutableAndImmutableColumns(ps, contract);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                contract.setId(keys.getInt(1));
            }
        }
        return contract;
    }

    // Persists the columns that change after creation: status and the officers who acted on it.
    public void save(Contract contract) throws SQLException {
        String sql = "UPDATE contracts SET status=?, approving_officer_id=?, drafting_officer_id=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, contract.getStatus());
            setNullableStaffId(ps, 2, contract.getApprovingOfficer());
            setNullableStaffId(ps, 3, contract.getDraftingOfficer());
            ps.setInt(4, contract.getContractId());
            ps.executeUpdate();
        }
    }

    public void addCommitteeVote(int contractId, int staffId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO committee_votes (contract_id, staff_id) VALUES (?,?)")) {
            ps.setInt(1, contractId);
            ps.setInt(2, staffId);
            ps.executeUpdate();
        }
    }

    public void addCoSigner(int contractId, int staffId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO cosigners (contract_id, staff_id) VALUES (?,?)")) {
            ps.setInt(1, contractId);
            ps.setInt(2, staffId);
            ps.executeUpdate();
        }
    }

    public Contract findById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM contracts WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Contract> findAll() throws SQLException {
        List<Contract> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM contracts ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Contract> findByApplicantId(int applicantId) throws SQLException {
        List<Contract> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM contracts WHERE applicant_id = ? ORDER BY id")) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private void bindMutableAndImmutableColumns(PreparedStatement ps, Contract contract) throws SQLException {
        ps.setInt(1, contract.getApplicant().getId());
        ps.setString(2, contract.getPrincipalAmount().toPlainString());
        ps.setString(3, contract.getTotalAmount().toPlainString());
        ps.setInt(4, contract.getDuration());
        ps.setDouble(5, contract.getInterestRate());
        ps.setString(6, contract.getStatus());
        setNullableStaffId(ps, 7, contract.getApprovingOfficer());
        setNullableStaffId(ps, 8, contract.getDraftingOfficer());
    }

    private void setNullableStaffId(PreparedStatement ps, int index, Staff staff) throws SQLException {
        if (staff == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, staff.getId());
        }
    }

    private Contract mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Applicant applicant = applicantDao.findById(rs.getInt("applicant_id"));
        Contract contract = new Contract(applicant, new BigDecimal(rs.getString("principal_amount")),
                rs.getInt("duration"), rs.getDouble("interest_rate"));
        contract.setId(id);
        contract.setStatus(rs.getString("status"));

        int approvingOfficerId = rs.getInt("approving_officer_id");
        if (!rs.wasNull()) {
            contract.setApprovingOfficer(staffDao.findById(approvingOfficerId));
        }
        int draftingOfficerId = rs.getInt("drafting_officer_id");
        if (!rs.wasNull()) {
            contract.setDraftingOfficer(staffDao.findById(draftingOfficerId));
        }

        try (PreparedStatement ps = connection.prepareStatement("SELECT staff_id FROM committee_votes WHERE contract_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet voteRs = ps.executeQuery()) {
                while (voteRs.next()) {
                    contract.addCommitteeVote(voteRs.getInt("staff_id"));
                }
            }
        }

        try (PreparedStatement ps = connection.prepareStatement("SELECT staff_id FROM cosigners WHERE contract_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet signerRs = ps.executeQuery()) {
                while (signerRs.next()) {
                    contract.addCoSigner(staffDao.findById(signerRs.getInt("staff_id")));
                }
            }
        }

        return contract;
    }
}
