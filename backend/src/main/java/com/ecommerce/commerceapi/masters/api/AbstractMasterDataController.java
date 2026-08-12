package com.ecommerce.commerceapi.masters.api;

import com.ecommerce.commerceapi.masters.service.AbstractMasterDataService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

abstract class AbstractMasterDataController {
    private final AbstractMasterDataService<?> service;
    AbstractMasterDataController(AbstractMasterDataService<?> service) { this.service = service; }
    @GetMapping public List<MasterDataResponse> findAll() { return service.findAll(); }
    @PostMapping @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED) public MasterDataResponse create(@Valid @RequestBody MasterDataRequest request) { return service.create(request); }
    @PutMapping("/{id}") public MasterDataResponse update(@PathVariable Long id, @Valid @RequestBody MasterDataRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/active") public MasterDataResponse updateActive(@PathVariable Long id, @RequestBody ActiveStatusRequest request) { return service.updateActive(id, request.active()); }
}
