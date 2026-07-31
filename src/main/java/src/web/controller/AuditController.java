package src.web.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import src.controller.LoaningSystem;
import src.model.AuditEntry;
import src.web.security.LmsUserPrincipal;

@RestController
@RequestMapping("/api/audit-log")
@PreAuthorize("hasRole('MANAGER')")
public class AuditController {

    private final LoaningSystem system;

    public AuditController(LoaningSystem system) {
        this.system = system;
    }

    @GetMapping
    public List<AuditEntry> list(@AuthenticationPrincipal LmsUserPrincipal principal) {
        return system.getAuditLog(principal.getDomainUser());
    }

    @GetMapping("/contract/{contractId}")
    public List<AuditEntry> forContract(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int contractId) {
        return system.getAuditLogForContract(principal.getDomainUser(), contractId);
    }
}
