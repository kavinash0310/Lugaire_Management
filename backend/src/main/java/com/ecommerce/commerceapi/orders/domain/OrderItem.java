package com.ecommerce.commerceapi.orders.domain;

import com.ecommerce.commerceapi.products.domain.ProductVariant;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id", nullable = false) private ProductVariant variant;
    @Column(nullable = false) private String skuSnapshot, productNameSnapshot;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private BigDecimal sellingPrice, lineTotal;
    public Long getId(){return id;} public Order getOrder(){return order;} public ProductVariant getVariant(){return variant;} public int getQuantity(){return quantity;} public BigDecimal getSellingPrice(){return sellingPrice;} public BigDecimal getLineTotal(){return lineTotal;} public String getSkuSnapshot(){return skuSnapshot;}
    public void setOrder(Order value){order=value;} public void setVariant(ProductVariant value){variant=value;} public void setSkuSnapshot(String value){skuSnapshot=value;} public void setProductNameSnapshot(String value){productNameSnapshot=value;} public void setQuantity(int value){quantity=value;} public void setSellingPrice(BigDecimal value){sellingPrice=value;} public void setLineTotal(BigDecimal value){lineTotal=value;}
}
