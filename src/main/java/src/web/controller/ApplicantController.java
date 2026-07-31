package src.web.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import src.controller.LoaningSystem;
import src.model.Applicant;
import src.web.dto.AddBalanceRequest;
import src.web.dto.ApplicantResponse;
import src.web.dto.BalanceResponse;
import src.web.dto.CreateApplicantRequest;
import src.web.security.LmsUserPrincipal;

@RestController
@RequestMapping("/api/applicants")
public class ApplicantController {

    private final LoaningSystem system;

    public ApplicantController(LoaningSystem system) {
        this.system = system;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicantResponse create(@AuthenticationPrincipal LmsUserPrincipal principal,
                                     @Valid @RequestBody CreateApplicantRequest request) {
        Applicant applicant = system.createApplicant(principal.getDomainUser(), request.name(), request.username(),
                request.phoneNumber(), request.password(), request.age(), request.income(), request.gender(),
                request.existingExternalDebt());
        return ApplicantResponse.from(applicant);
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<ApplicantResponse> list() {
        return system.getAllApplicants().stream().map(ApplicantResponse::from).toList();
    }

    @GetMapping("/me/balance")
    @PreAuthorize("hasRole('APPLICANT')")
    public BalanceResponse myBalance(@AuthenticationPrincipal LmsUserPrincipal principal) {
        return new BalanceResponse(system.getMyBalance(principal.getDomainUser()));
    }

    @PostMapping("/{applicantId}/balance")
    @PreAuthorize("hasRole('MANAGER')")
    public ApplicantResponse addBalance(@AuthenticationPrincipal LmsUserPrincipal principal,
                                         @PathVariable int applicantId, @Valid @RequestBody AddBalanceRequest request) {
        Applicant applicant = system.addBalanceforApplicant(principal.getDomainUser(), applicantId, request.amount());
        return ApplicantResponse.from(applicant);
    }
}
