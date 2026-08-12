package com.ecommerce.commerceapi.masters.service;
import com.ecommerce.commerceapi.masters.api.MasterDataRequest;
import com.ecommerce.commerceapi.masters.domain.Category;
import com.ecommerce.commerceapi.masters.repository.CategoryRepository;
import org.springframework.stereotype.Service;
@Service public class CategoryService extends AbstractMasterDataService<Category> {
    public CategoryService(CategoryRepository repository) { super(repository); }
    protected Category newEntity() { return new Category(); }
    protected void applySpecificFields(Category entity, MasterDataRequest request) { entity.setGroupName(required(request.groupName(), "groupName")); }
    protected String groupName(Category entity) { return entity.getGroupName(); }
    private String required(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required"); return value.trim().toUpperCase(); }
}
