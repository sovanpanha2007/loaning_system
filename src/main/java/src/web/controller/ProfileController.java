package src.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import src.controller.LoaningSystem;
import src.interfaces.ILoginable;
import src.web.dto.ChangePasswordRequest;
import src.web.dto.ChangeUsernameRequest;
import src.web.dto.UserResponse;
import src.web.security.LmsUserPrincipal;

// SET_NEW_NAME / SET_NEW_PASSWORD are granted to every role (Staff and Applicant alike), so
// these self-service actions live here rather than under /api/staff or /api/applicants.
@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final LoaningSystem system;

    public ProfileController(LoaningSystem system) {
        this.system = system;
    }

    @PutMapping("/username")
    public UserResponse changeUsername(@AuthenticationPrincipal LmsUserPrincipal principal,
                                        @Valid @RequestBody ChangeUsernameRequest request) {
        ILoginable updated = system.setNewUserName(principal.getDomainUser(), request.username(),
                request.newUsername(), request.password());
        return UserResponse.from(updated);
    }

    @PutMapping("/password")
    public void changePassword(@AuthenticationPrincipal LmsUserPrincipal principal,
                                @Valid @RequestBody ChangePasswordRequest request) {
        system.setNewPassword(principal.getDomainUser(), request.name(), request.password(), request.newPassword());
    }
}
