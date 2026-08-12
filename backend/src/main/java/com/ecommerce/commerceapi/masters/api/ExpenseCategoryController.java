package com.ecommerce.commerceapi.masters.api;

import com.ecommerce.commerceapi.masters.service.ExpenseCategoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expense-categories")
public class ExpenseCategoryController extends AbstractMasterDataController {
    public ExpenseCategoryController(ExpenseCategoryService service) {
        super(service);
    }
}
