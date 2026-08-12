package com.ecommerce.commerceapi.masters.api;
import com.ecommerce.commerceapi.masters.service.ColorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/colors") public class ColorController extends AbstractMasterDataController { public ColorController(ColorService service) { super(service); } }
