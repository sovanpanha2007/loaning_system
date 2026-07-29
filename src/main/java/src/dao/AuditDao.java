package src.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import src.interfaces.ILoginable;
import src.model.AuditEntry;
import src.model.Staff;

public class AuditDao {

    private final Connection connection;

    public AuditDao(Connection connection) {
        this.connection = connection;
    }

    // actor may be null for system-initiated events (e.g. the startup admin seed).
    public void record(ILoginable actor, String action, String subjectType, Integer subjectId, String details) throws SQLException {
        String sql = "INSERT INTO audit_log (occurred_at, actor_id, actor_type, action, subject_type, subject_id, details) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            if (actor == null) {
                ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setInt(2, actor.getId());
                ps.setString(3, actor instanceof Staff ? "STAFF" : "APPLICANT");
            }
            ps.setString(4, action);
            ps.setString(5, subjectType);
            if (subjectId == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, subjectId);
            }
            ps.setString(7, details);
            ps.executeUpdate();
        }
    }

    public List<AuditEntry> findAll() throws SQLException {
        List<AuditEntry> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM audit_log ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<AuditEntry> findBySubject(String subjectType, int subjectId) throws SQLException {
        List<AuditEntry> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM audit_log WHERE subject_type = ? AND subject_id = ? ORDER BY id")) {
            ps.setString(1, subjectType);
            ps.setInt(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        int actorId = rs.getInt("actor_id");
        Integer actorIdBoxed = rs.wasNull() ? null : actorId;
        int subjectId = rs.getInt("subject_id");
        Integer subjectIdBoxed = rs.wasNull() ? null : subjectId;
        return new AuditEntry(
                rs.getInt("id"),
                rs.getString("occurred_at"),
                actorIdBoxed,
                rs.getString("actor_type"),
                rs.getString("action"),
                rs.getString("subject_type"),
                subjectIdBoxed,
                rs.getString("details"));
    }
}
