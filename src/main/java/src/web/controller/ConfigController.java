package src.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import src.controller.LoaningSystem;
import src.web.dto.ConfigResponse;
import src.web.dto.DelinquencyResponse;
import src.web.dto.SetMaxDtiRequest;
import src.web.dto.SetRequiredVotesRequest;
import src.web.security.LmsUserPrincipal;

@RestController
@RequestMapping("/api/config")
@PreAuthorize("hasRole('MANAGER')")
public class ConfigController {

    private final LoaningSystem system;

    public ConfigController(LoaningSystem system) {
        this.system = system;
    }

    @GetMapping
    public ConfigResponse get() {
        return ConfigResponse.from(system);
    }

    @PutMapping("/required-votes")
    public ConfigResponse setRequiredVotes(@AuthenticationPrincipal LmsUserPrincipal principal,
                                            @Valid @RequestBody SetRequiredVotesRequest request) {
        system.setNewRequiredVotes(principal.getDomainUser(), request.votes());
        return ConfigResponse.from(system);
    }

    @PutMapping("/max-dti")
    public ConfigResponse setMaxDti(@AuthenticationPrincipal LmsUserPrincipal principal,
                                     @Valid @RequestBody SetMaxDtiRequest request) {
        system.setNewMaxDebtToIncomeRatio(principal.getDomainUser(), request.ratio());
        return ConfigResponse.from(system);
    }

    @PostMapping("/delinquency-check")
    public DelinquencyResponse checkDelinquency(@AuthenticationPrincipal LmsUserPrincipal principal) {
        return DelinquencyResponse.from(system.checkDelinquency(principal.getDomainUser()));
    }
}
