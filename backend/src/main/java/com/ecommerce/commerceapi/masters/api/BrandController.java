package com.ecommerce.commerceapi.masters.api;
import com.ecommerce.commerceapi.masters.service.BrandService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/brands") public class BrandController extends AbstractMasterDataController { public BrandController(BrandService service) { super(service); } }
