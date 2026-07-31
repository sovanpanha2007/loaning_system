package src.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import src.controller.LoaningSystem;
import src.model.LoanOfficer;
import src.model.Staff;
import src.web.dto.CreateStaffRequest;
import src.web.dto.SetApprovalLimitRequest;
import src.web.dto.StaffResponse;
import src.web.security.LmsUserPrincipal;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final LoaningSystem system;

    public StaffController(LoaningSystem system) {
        this.system = system;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@AuthenticationPrincipal LmsUserPrincipal principal,
                                 @Valid @RequestBody CreateStaffRequest request) {
        Staff staff = system.createStaff(principal.getDomainUser(), request.name(), request.username(),
                request.phoneNumber(), request.age(), request.password(), request.salary(), request.position());
        return StaffResponse.from(staff);
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<StaffResponse> list() {
        return system.getAllStaff().stream().map(StaffResponse::from).toList();
    }

    @PutMapping("/{staffId}/deactivate")
    @PreAuthorize("hasRole('MANAGER')")
    public StaffResponse deactivate(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int staffId) {
        Staff staff = system.deactivateStaff(principal.getDomainUser(), staffId);
        return StaffResponse.from(staff);
    }

    @PutMapping("/{staffId}/approval-limit")
    @PreAuthorize("hasRole('MANAGER')")
    public StaffResponse setApprovalLimit(@AuthenticationPrincipal LmsUserPrincipal principal, @PathVariable int staffId,
                                           @Valid @RequestBody SetApprovalLimitRequest request) {
        LoanOfficer officer = system.setNewApprovalLimit(principal.getDomainUser(), staffId, request.newAmount());
        return StaffResponse.from(officer);
    }
}
