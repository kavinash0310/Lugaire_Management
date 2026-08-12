package com.ecommerce.commerceapi.masters.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sizes")
public class Size extends MasterDataEntity {
    @Column(name = "size_type", nullable = false, length = 30)
    private String sizeType;
    public String getSizeType() { return sizeType; }
    public void setSizeType(String sizeType) { this.sizeType = sizeType; }
}
