package com.ecommerce.commerceapi.orders.domain;

import com.ecommerce.commerceapi.marketplaces.domain.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private String orderId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "marketplace_id", nullable = false) private Marketplace marketplace;
    @Column(nullable = false) private LocalDate orderDate;
    private String customerName, customerPhone, shippingAddress, city, state, pincode, trackingNumber, courierPartner, remarks;
    @Column(nullable = false) private BigDecimal totalOrderValue, commission, shippingCharge, otherCharges, netAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrderStatus orderStatus = OrderStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    @Column(nullable = false) private boolean inventoryDeducted;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) private List<OrderItem> items = new ArrayList<>();
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
    public Long getId(){return id;} public String getOrderId(){return orderId;} public void setOrderId(String v){orderId=v;} public Marketplace getMarketplace(){return marketplace;} public void setMarketplace(Marketplace v){marketplace=v;} public LocalDate getOrderDate(){return orderDate;} public void setOrderDate(LocalDate v){orderDate=v;} public List<OrderItem> getItems(){return items;} public boolean isInventoryDeducted(){return inventoryDeducted;} public void setInventoryDeducted(boolean v){inventoryDeducted=v;} public OrderStatus getOrderStatus(){return orderStatus;} public void setOrderStatus(OrderStatus v){orderStatus=v;}
    public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;} public String getCustomerPhone(){return customerPhone;} public void setCustomerPhone(String v){customerPhone=v;} public String getShippingAddress(){return shippingAddress;} public void setShippingAddress(String v){shippingAddress=v;} public String getCity(){return city;} public void setCity(String v){city=v;} public String getState(){return state;} public void setState(String v){state=v;} public String getPincode(){return pincode;} public void setPincode(String v){pincode=v;} public String getTrackingNumber(){return trackingNumber;} public void setTrackingNumber(String v){trackingNumber=v;} public String getCourierPartner(){return courierPartner;} public void setCourierPartner(String v){courierPartner=v;} public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
    public void setCommission(BigDecimal v){commission=v;} public void setShippingCharge(BigDecimal v){shippingCharge=v;} public void setOtherCharges(BigDecimal v){otherCharges=v;} public void setTotalOrderValue(BigDecimal v){totalOrderValue=v;} public void setNetAmount(BigDecimal v){netAmount=v;}
    public PaymentStatus getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(PaymentStatus v){paymentStatus=v;}
    public BigDecimal getTotalOrderValue(){return totalOrderValue;} public BigDecimal getCommission(){return commission;} public BigDecimal getShippingCharge(){return shippingCharge;} public BigDecimal getOtherCharges(){return otherCharges;} public BigDecimal getNetAmount(){return netAmount;}
}
