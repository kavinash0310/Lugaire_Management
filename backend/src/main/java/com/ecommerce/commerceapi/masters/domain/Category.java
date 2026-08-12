package com.ecommerce.commerceapi.masters.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends MasterDataEntity {
    @Column(name = "group_name", nullable = false, length = 30)
    private String groupName;
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
