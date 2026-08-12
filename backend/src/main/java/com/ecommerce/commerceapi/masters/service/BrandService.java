package com.ecommerce.commerceapi.masters.service;
import com.ecommerce.commerceapi.masters.domain.Brand;
import com.ecommerce.commerceapi.masters.repository.BrandRepository;
import org.springframework.stereotype.Service;
@Service public class BrandService extends AbstractMasterDataService<Brand> { public BrandService(BrandRepository repository) { super(repository); } protected Brand newEntity() { return new Brand(); } }
