package com.ecommerce.commerceapi.masters.api;
import com.ecommerce.commerceapi.masters.service.CategoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/categories") public class CategoryController extends AbstractMasterDataController { public CategoryController(CategoryService service) { super(service); } }
