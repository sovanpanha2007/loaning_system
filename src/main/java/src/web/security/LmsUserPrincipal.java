package src.web.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import src.interfaces.ILoginable;
import src.model.Applicant;
import src.model.CreditCommittee;
import src.model.LoanOfficer;
import src.model.Manager;

// Adapts the app's own ILoginable (Applicant/Manager/LoanOfficer/CreditCommittee) to Spring
// Security's UserDetails so it can sit in the SecurityContext/session as the authenticated
// principal. Credential checking already happened in LmsAuthenticationProvider via
// LoaningSystem.authenticate(), so getPassword() here is never actually consulted.
public class LmsUserPrincipal implements UserDetails {

    private final ILoginable user;

    public LmsUserPrincipal(ILoginable user) {
        this.user = user;
    }

    public ILoginable getDomainUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role()));
    }

    private String role() {
        if (user instanceof Manager) return "ROLE_MANAGER";
        if (user instanceof LoanOfficer) return "ROLE_LOAN_OFFICER";
        if (user instanceof CreditCommittee) return "ROLE_CREDIT_COMMITTEE";
        if (user instanceof Applicant) return "ROLE_APPLICANT";
        throw new IllegalStateException("Unknown user type: " + user.getClass());
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
