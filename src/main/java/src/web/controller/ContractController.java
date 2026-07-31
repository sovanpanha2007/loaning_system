package src.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import src.controller.LoaningSystem;
import src.model.Contract;
import src.model.PaymentSchedule;
import src.web.dto.AddCoSignerRequest;
import src.web.dto.ContractResponse;
import src.web.dto.CreateContractRequest;
import src.web.dto.MakePaymentRequest;
import src.web.dto.MakePaymentResponse;
import src.web.dto.PaymentScheduleResponse;
import src.web.security.LmsUserPrincipal;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final LoaningSystem system;

    public ContractController(LoaningSystem system) {
        this.system = system;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('APPLICANT', 'LOAN_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse create(@AuthenticationPrincipal LmsUserPrincipal principal,
                                    @Valid @RequestBody CreateContractRequest request) {
        Contract contract = system.createContract(principal.getDomainUser(), request.applicantId(),
                request.amount(), request.duration());
        return ContractResponse.from(contract);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'LOAN_OFFICER', 'CREDIT_COMMITTEE')")
    public List<ContractResponse> list() {
        return system.getAllContracts().stream().map(ContractResponse::from).toList();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('APPLICANT')")
    public List<ContractResponse> mine(@AuthenticationPrincipal LmsUserPrincipal principal) {
        return system.getMyContracts(principal.getDomainUser()).stream().map(ContractResponse::from).toList();
    }

    @GetMapping("/{contractId}/schedule")
    @PreAuthorize("hasRole('APPLICANT')")
    public PaymentScheduleResponse schedule(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int contractId) {
        PaymentSchedule schedule = system.getMySchedule(principal.getDomainUser(), contractId);
        return PaymentScheduleResponse.from(schedule);
    }

    @PostMapping("/{contractId}/approve")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'CREDIT_COMMITTEE')")
    public ContractResponse approve(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int contractId) {
        Contract contract = system.approveContract(principal.getDomainUser(), contractId);
        return ContractResponse.from(contract);
    }

    @PostMapping("/{contractId}/reject")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ContractResponse reject(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int contractId) {
        Contract contract = system.rejectContract(principal.getDomainUser(), contractId);
        return ContractResponse.from(contract);
    }

    @PostMapping("/{contractId}/cosigners")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'CREDIT_COMMITTEE')")
    public ContractResponse addCoSigner(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int contractId,
                                         @Valid @RequestBody AddCoSignerRequest request) {
        Contract contract = system.addCoSigner(principal.getDomainUser(), contractId, request.staffId());
        return ContractResponse.from(contract);
    }

    @PostMapping("/{contractId}/payments")
    @PreAuthorize("hasRole('APPLICANT')")
    public MakePaymentResponse makePayment(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int contractId,
                                            @Valid @RequestBody MakePaymentRequest request) {
        PaymentSchedule.PaymentResult result = system.makePayment(principal.getDomainUser(), contractId, request.amount());
        return new MakePaymentResponse(result.getAmountApplied(), result.getMonthsTouched(),
                system.getMyBalance(principal.getDomainUser()));
    }
}
