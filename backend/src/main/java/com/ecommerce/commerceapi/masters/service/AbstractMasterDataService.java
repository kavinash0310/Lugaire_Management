package com.ecommerce.commerceapi.masters.service;

import com.ecommerce.commerceapi.masters.api.MasterDataRequest;
import com.ecommerce.commerceapi.masters.api.MasterDataResponse;
import com.ecommerce.commerceapi.masters.domain.MasterDataEntity;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractMasterDataService<T extends MasterDataEntity> {
    private final JpaRepository<T, Long> repository;

    protected AbstractMasterDataService(JpaRepository<T, Long> repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<MasterDataResponse> findAll() { return repository.findAll().stream().map(this::toResponse).toList(); }

    @Transactional
    public MasterDataResponse create(MasterDataRequest request) {
        T entity = newEntity();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public MasterDataResponse update(Long id, MasterDataRequest request) {
        T entity = findEntity(id);
        apply(entity, request);
        return toResponse(entity);
    }

    @Transactional
    public MasterDataResponse updateActive(Long id, boolean active) {
        T entity = findEntity(id);
        entity.setActive(active);
        return toResponse(entity);
    }

    private T findEntity(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Master data item not found: " + id)); }

    private void apply(T entity, MasterDataRequest request) {
        entity.setName(request.name().trim());
        entity.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        applySpecificFields(entity, request);
    }

    protected abstract T newEntity();
    protected void applySpecificFields(T entity, MasterDataRequest request) {}
    protected String groupName(T entity) { return null; }
    protected String sizeType(T entity) { return null; }

    private MasterDataResponse toResponse(T entity) {
        return new MasterDataResponse(entity.getId(), entity.getName(), entity.getCode(), groupName(entity), sizeType(entity), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
