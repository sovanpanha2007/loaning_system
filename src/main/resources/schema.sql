CREATE TABLE IF NOT EXISTS staff (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT NOT NULL,
    name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    phone_number TEXT NOT NULL UNIQUE,
    age INTEGER NOT NULL,
    password_hash TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    salary REAL NOT NULL DEFAULT 0,
    position TEXT NOT NULL,
    access_level INTEGER,
    max_approval_limit TEXT
);

CREATE TABLE IF NOT EXISTS applicants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    phone_number TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    gender TEXT NOT NULL,
    salary TEXT NOT NULL,
    age INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    account_balance TEXT NOT NULL DEFAULT '0.00',
    existing_external_debt TEXT NOT NULL DEFAULT '0.00'
);

CREATE TABLE IF NOT EXISTS contracts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    applicant_id INTEGER NOT NULL REFERENCES applicants(id),
    principal_amount TEXT NOT NULL,
    total_amount TEXT NOT NULL,
    duration INTEGER NOT NULL,
    interest_rate REAL NOT NULL,
    status TEXT NOT NULL,
    approving_officer_id INTEGER REFERENCES staff(id),
    drafting_officer_id INTEGER REFERENCES staff(id)
);

CREATE TABLE IF NOT EXISTS cosigners (
    contract_id INTEGER NOT NULL REFERENCES contracts(id),
    staff_id INTEGER NOT NULL REFERENCES staff(id),
    PRIMARY KEY (contract_id, staff_id)
);

CREATE TABLE IF NOT EXISTS committee_votes (
    contract_id INTEGER NOT NULL REFERENCES contracts(id),
    staff_id INTEGER NOT NULL REFERENCES staff(id),
    PRIMARY KEY (contract_id, staff_id)
);

CREATE TABLE IF NOT EXISTS payment_schedules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contract_id INTEGER NOT NULL UNIQUE REFERENCES contracts(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    schedule_id INTEGER NOT NULL REFERENCES payment_schedules(id),
    month_number INTEGER NOT NULL,
    amount TEXT NOT NULL,
    interest_portion TEXT NOT NULL,
    principal_portion TEXT NOT NULL,
    remaining_balance TEXT NOT NULL,
    due_date TEXT NOT NULL,
    is_paid INTEGER NOT NULL DEFAULT 0,
    paid_date TEXT,
    is_late INTEGER NOT NULL DEFAULT 0,
    late_fee TEXT NOT NULL DEFAULT '0.00',
    amount_paid TEXT NOT NULL DEFAULT '0.00',
    UNIQUE(schedule_id, month_number)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    occurred_at TEXT NOT NULL,
    actor_id INTEGER,
    actor_type TEXT,
    action TEXT NOT NULL,
    subject_type TEXT,
    subject_id INTEGER,
    details TEXT
);
