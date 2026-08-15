package com.ecommerce.commerceapi.auth.service;

import com.ecommerce.commerceapi.auth.api.LoginRequest;
import com.ecommerce.commerceapi.auth.api.SessionUserResponse;
import com.ecommerce.commerceapi.auth.domain.UserAccount;
import com.ecommerce.commerceapi.auth.repository.UserAccountRepository;
import com.ecommerce.commerceapi.auth.security.AuthenticatedUser;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogs;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogs) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogs = auditLogs;
    }

    public SessionUserResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
        if (!userAccount.isActive()) {
            throw new InactiveAccountException("Account is inactive");
        }
        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        AuthenticatedUser principal = AuthenticatedUser.from(userAccount);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        servletRequest.changeSessionId();
        auditLogs.log(
                principal,
                "LOGIN",
                "AUTH",
                "User",
                String.valueOf(userAccount.getId()),
                "User logged in",
                null,
                Map.of("email", userAccount.getEmail(), "name", userAccount.getName(), "role", userAccount.getRole().getCode()));
        return toResponse(userAccount);
    }

    public SessionUserResponse me(AuthenticatedUser principal) {
        if (principal == null) {
            throw new AuthenticationFailedException("Session is no longer valid");
        }
        UserAccount userAccount = userAccountRepository.findById(principal.id())
                .orElseThrow(() -> new AuthenticationFailedException("Session is no longer valid"));
        if (!userAccount.isActive()) {
            throw new InactiveAccountException("Account is inactive");
        }
        return toResponse(userAccount);
    }

    public void logout(HttpServletRequest request) {
        AuthenticatedUser currentUser = currentUser();
        if (currentUser != null) {
            auditLogs.log(
                    currentUser,
                    "LOGOUT",
                    "AUTH",
                    "User",
                    String.valueOf(currentUser.id()),
                    "User logged out",
                    null,
                    Map.of("email", currentUser.email(), "name", currentUser.name(), "role", currentUser.roleCode()));
        }
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private AuthenticatedUser currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }

    private SessionUserResponse toResponse(UserAccount userAccount) {
        return new SessionUserResponse(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmail(),
                userAccount.getRole().getCode(),
                userAccount.getRole().getName(),
                userAccount.isActive(),
                userAccount.getCreatedAt(),
                userAccount.getUpdatedAt());
    }
}
