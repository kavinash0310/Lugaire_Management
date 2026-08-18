package com.ecommerce.commerceapi.auth.service;

import com.ecommerce.commerceapi.auth.domain.UserAccount;
import com.ecommerce.commerceapi.auth.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AuthBootstrapper implements ApplicationRunner {
    private final boolean enabled;
    private final String bootstrapName;
    private final String bootstrapEmail;
    private final String bootstrapPassword;
    private final String bootstrapRoleCode;
    private final RoleService roleService;
    private final UserAccountRepository userAccountRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthBootstrapper(
            @Value("${app.auth.bootstrap.enabled:true}") boolean enabled,
            @Value("${app.auth.bootstrap.name:Administrator}") String bootstrapName,
            @Value("${app.auth.bootstrap.email:admin@lugaire.local}") String bootstrapEmail,
            @Value("${app.auth.bootstrap.password:lugai.re}") String bootstrapPassword,
            @Value("${app.auth.bootstrap.role-code:ADMIN}") String bootstrapRoleCode,
            RoleService roleService,
            UserAccountRepository userAccountRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.bootstrapName = bootstrapName;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapRoleCode = bootstrapRoleCode;
        this.roleService = roleService;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        roleService.ensureRole("ADMIN", "Administrator");
        roleService.ensureRole("MANAGER", "Manager");
        roleService.ensureRole("STAFF", "Staff");

        UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(bootstrapEmail.trim())
                .orElseGet(UserAccount::new);
        userAccount.setName(bootstrapName);
        userAccount.setEmail(bootstrapEmail.trim().toLowerCase());
        userAccount.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        userAccount.setRole(roleService.requireRole(bootstrapRoleCode));
        userAccount.setActive(true);
        userAccountRepository.save(userAccount);
    }
}
