package com.ecommerce.commerceapi.masters.service;
import com.ecommerce.commerceapi.masters.api.MasterDataRequest;
import com.ecommerce.commerceapi.masters.domain.Size;
import com.ecommerce.commerceapi.masters.repository.SizeRepository;
import org.springframework.stereotype.Service;
@Service public class SizeService extends AbstractMasterDataService<Size> {
    public SizeService(SizeRepository repository) { super(repository); }
    protected Size newEntity() { return new Size(); }
    protected void applySpecificFields(Size entity, MasterDataRequest request) { entity.setSizeType(required(request.sizeType(), "sizeType")); }
    protected String sizeType(Size entity) { return entity.getSizeType(); }
    private String required(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required"); return value.trim().toUpperCase(); }
}
