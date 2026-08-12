package com.ecommerce.commerceapi.masters.api;
import com.ecommerce.commerceapi.masters.service.SizeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/sizes") public class SizeController extends AbstractMasterDataController { public SizeController(SizeService service) { super(service); } }
