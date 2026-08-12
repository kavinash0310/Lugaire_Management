package com.ecommerce.commerceapi.auth.service;

import com.ecommerce.commerceapi.auth.api.LoginRequest;
import com.ecommerce.commerceapi.auth.api.SessionUserResponse;
import com.ecommerce.commerceapi.auth.domain.UserAccount;
import com.ecommerce.commerceapi.auth.repository.UserAccountRepository;
import com.ecommerce.commerceapi.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
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
        return toResponse(userAccount);
    }

    public SessionUserResponse me(AuthenticatedUser principal) {
        UserAccount userAccount = userAccountRepository.findById(principal.id())
                .orElseThrow(() -> new AuthenticationFailedException("Session is no longer valid"));
        if (!userAccount.isActive()) {
            throw new InactiveAccountException("Account is inactive");
        }
        return toResponse(userAccount);
    }

    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
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
