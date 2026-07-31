export type Role = 'MANAGER' | 'LOAN_OFFICER' | 'CREDIT_COMMITTEE' | 'APPLICANT'

export interface UserResponse {
  id: number
  name: string
  username: string
  role: Role
}

export interface ApplicantResponse {
  id: number
  name: string
  username: string
  phoneNumber: string
  gender: string
  salary: number
  age: number
  existingExternalDebt: number
  accountBalance: number
  active: boolean
}

export interface StaffResponse {
  id: number
  name: string
  username: string
  phoneNumber: string
  age: number
  salary: number
  position: string
  active: boolean
  maxApprovalLimit: number | null
  accessLevel: number | null
}

export interface ContractResponse {
  id: number
  applicantId: number
  applicantName: string
  principalAmount: number
  totalAmount: number
  duration: number
  interestRate: number
  effectiveApr: number
  status: string
  approvingOfficerId: number | null
  approvingOfficerName: string | null
  draftingOfficerId: number | null
  draftingOfficerName: string | null
  coSignerIds: number[]
  committeeVoteCount: number
}

export interface PaymentResponse {
  monthNumber: number
  amount: number
  interestPortion: number
  principalPortion: number
  remainingBalance: number
  dueDate: string
  paid: boolean
  paidDate: string | null
  late: boolean
  lateFee: number
  amountPaid: number
  remainingDue: number
  totalDue: number
}

export interface PaymentScheduleResponse {
  scheduleId: number
  contractId: number
  fullyPaid: boolean
  payments: PaymentResponse[]
}

export interface MakePaymentResponse {
  amountApplied: number
  monthsTouched: number[]
  remainingBalance: number
}

export interface BalanceResponse {
  balance: number
}

export interface AuditEntry {
  id: number
  occurredAt: string
  actorId: number | null
  actorType: string | null
  action: string
  subjectType: string | null
  subjectId: number | null
  details: string | null
}

export interface ConfigResponse {
  bankName: string
  interestRate: number
  requiredCommitteeVotes: number
  maxDebtToIncomeRatio: number
}

export interface DelinquencyResponse {
  flaggedLate: number
  defaulted: number
}

export interface ApiError {
  error: string
}
