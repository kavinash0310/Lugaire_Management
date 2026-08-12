package com.ecommerce.commerceapi.auth.security;

import com.ecommerce.commerceapi.auth.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class CurrentUserSessionFilter extends OncePerRequestFilter {
    private final UserAccountRepository userAccountRepository;

    public CurrentUserSessionFilter(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedUser currentUser) {
                userAccountRepository.findById(currentUser.id()).ifPresentOrElse(userAccount -> {
                    if (!userAccount.isActive()) {
                        SecurityContextHolder.clearContext();
                        var session = request.getSession(false);
                        if (session != null) {
                            session.invalidate();
                        }
                    } else {
                        AuthenticatedUser refreshed = AuthenticatedUser.from(userAccount);
                        UsernamePasswordAuthenticationToken updated = new UsernamePasswordAuthenticationToken(refreshed, authentication.getCredentials(), refreshed.getAuthorities());
                        updated.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(updated);
                    }
                }, SecurityContextHolder::clearContext);
            }
        }
        filterChain.doFilter(request, response);
    }
}
