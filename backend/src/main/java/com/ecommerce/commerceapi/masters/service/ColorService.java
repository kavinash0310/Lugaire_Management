package com.ecommerce.commerceapi.masters.service;
import com.ecommerce.commerceapi.masters.domain.Color;
import com.ecommerce.commerceapi.masters.repository.ColorRepository;
import org.springframework.stereotype.Service;
@Service public class ColorService extends AbstractMasterDataService<Color> { public ColorService(ColorRepository repository) { super(repository); } protected Color newEntity() { return new Color(); } }
