package com.ecommerce.commerceapi.returns.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.ecommerce.commerceapi.inventory.service.InventoryService;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.orders.repository.OrderItemRepository;
import com.ecommerce.commerceapi.products.domain.Product;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.returns.api.ReturnCreateRequest;
import com.ecommerce.commerceapi.returns.api.ReturnStatusRequest;
import com.ecommerce.commerceapi.returns.domain.ReturnStatus;
import com.ecommerce.commerceapi.returns.domain.ReturnType;
import com.ecommerce.commerceapi.returns.repository.ReturnRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.never;

class ReturnServiceTest {
    private final ReturnRecordRepository records = mock(ReturnRecordRepository.class);
    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final ReturnService service = new ReturnService(records, orderItems, inventory);
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        Order order = new Order();
        order.setOrderId("MSH-20260805-0001");
        order.setPlatform(OrderPlatform.MEESHO);
        order.setOrderStatus(OrderStatus.DELIVERED);
        Product product = new Product();
        product.setProductName("T-Shirt");
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("CK-MTS-0001-BLK-M");
        variant.setCostPrice(BigDecimal.valueOf(180));
        orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setVariant(variant);
        orderItem.setQuantity(2);
        when(orderItems.findById(1L)).thenReturn(Optional.of(orderItem));
        when(records.returnedQuantity(any())).thenReturn(0);
        when(records.save(any())).thenAnswer(invocation -> {
            var record = (com.ecommerce.commerceapi.returns.domain.ReturnRecord) invocation.getArgument(0);
            if (record.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(record, "id", 1L);
            }
            return record;
        });
    }

    @Test
    void restoresResellableReturnOnlyOnce() {
        var created = service.create(request(ReturnType.RETURNED, 1, true));
        when(records.findById(created.id())).thenReturn(Optional.of(recordFrom(created)));

        service.updateStatus(created.id(), new ReturnStatusRequest(ReturnStatus.RECEIVED, LocalDate.now()));
        service.updateStatus(created.id(), new ReturnStatusRequest(ReturnStatus.RECEIVED, LocalDate.now()));

        verify(inventory, times(1)).record(any());
    }

    @Test
    void damagedReturnDoesNotRestoreSellableStock() {
        var created = service.create(request(ReturnType.RETURNED, 1, false));
        when(records.findById(created.id())).thenReturn(Optional.of(recordFrom(created)));

        service.updateStatus(created.id(), new ReturnStatusRequest(ReturnStatus.RECEIVED, LocalDate.now()));

        verify(inventory, never()).record(any());
        assertEquals(BigDecimal.valueOf(180), created.productCost());
    }

    @Test
    void rejectsReturnQuantityBeyondOrderedQuantity() {
        when(records.returnedQuantity(any())).thenReturn(1);

        assertThrows(IllegalArgumentException.class, () -> service.create(request(ReturnType.RETURNED, 2, true)));
    }

    private ReturnCreateRequest request(ReturnType type, int quantity, boolean resellable) {
        return new ReturnCreateRequest(1L, type, "Customer reason", quantity, BigDecimal.TEN, BigDecimal.ONE,
                resellable, LocalDate.now(), "Test");
    }

    private com.ecommerce.commerceapi.returns.domain.ReturnRecord recordFrom(com.ecommerce.commerceapi.returns.api.ReturnResponse response) {
        var record = new com.ecommerce.commerceapi.returns.domain.ReturnRecord();
        org.springframework.test.util.ReflectionTestUtils.setField(record, "id", response.id());
        record.setOrder(orderItem.getOrder());
        record.setOrderItem(orderItem);
        record.setVariant(orderItem.getVariant());
        record.setType(ReturnType.valueOf(response.type()));
        record.setQuantity(response.quantity());
        record.setResellable(response.resellable());
        record.setStatus(ReturnStatus.INITIATED);
        record.setReturnDate(response.returnDate());
        record.setProductCost(response.productCost());
        record.setShippingLoss(response.shippingLoss());
        record.setOtherLoss(response.otherLoss());
        record.setTotalLoss(response.totalLoss());
        return record;
    }
}
