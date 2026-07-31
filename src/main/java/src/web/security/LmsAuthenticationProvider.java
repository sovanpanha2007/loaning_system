package src.web.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import src.controller.LoaningSystem;
import src.controller.LogginException;
import src.interfaces.ILoginable;

// Delegates credential checking straight to LoaningSystem.authenticate() — the same lookup
// (staff, then applicant, by username) and password check the CLI uses — rather than
// reimplementing it against Spring Security's UserDetailsService/PasswordEncoder machinery.
@Component
public class LmsAuthenticationProvider implements AuthenticationProvider {

    private final LoaningSystem loaningSystem;

    public LmsAuthenticationProvider(LoaningSystem loaningSystem) {
        this.loaningSystem = loaningSystem;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());

        ILoginable user;
        try {
            user = loaningSystem.authenticate(username, password);
        } catch (LogginException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        }

        LmsUserPrincipal principal = new LmsUserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
