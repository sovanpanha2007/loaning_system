package src.web.security;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Spring Security's CookieCsrfTokenRepository resolves the token lazily and only writes the
// XSRF-TOKEN cookie if something actually reads it during the request — a plain GET otherwise
// leaves the SPA with no cookie to echo back on its first mutating request. Forcing
// CsrfToken.getToken() here on every request is Spring's own documented fix for cookie-based
// CSRF with a JS frontend.
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
