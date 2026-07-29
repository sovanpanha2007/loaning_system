package src.model;

// A single read-only row from the audit log: who did what, to what, and when.
// Populated only by AuditDao — nothing about it is mutable business state.
public class AuditEntry {

    private final int id;
    private final String occurredAt;
    private final Integer actorId;
    private final String actorType;
    private final String action;
    private final String subjectType;
    private final Integer subjectId;
    private final String details;

    public AuditEntry(int id, String occurredAt, Integer actorId, String actorType, String action,
                       String subjectType, Integer subjectId, String details) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorType = actorType;
        this.action = action;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.details = details;
    }

    public int getId()              { return id; }
    public String getOccurredAt()   { return occurredAt; }
    public Integer getActorId()     { return actorId; }
    public String getActorType()    { return actorType; }
    public String getAction()       { return action; }
    public String getSubjectType()  { return subjectType; }
    public Integer getSubjectId()   { return subjectId; }
    public String getDetails()      { return details; }

    @Override
    public String toString() {
        String actor = actorType != null ? actorType + " #" + actorId : "system";
        String subject = subjectType != null ? subjectType + " #" + subjectId : "-";
        return "[" + occurredAt + "] " + actor + " -> " + action + " (" + subject + ")"
                + (details != null && !details.isBlank() ? ": " + details : "");
    }
}
