package com.ecommerce.commerceapi.settings.repository;

import com.ecommerce.commerceapi.settings.domain.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKeyIgnoreCase(String settingKey);
}
