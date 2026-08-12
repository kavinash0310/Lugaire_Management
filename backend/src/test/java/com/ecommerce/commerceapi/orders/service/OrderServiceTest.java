package com.ecommerce.commerceapi.orders.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecommerce.commerceapi.inventory.api.InventoryTransactionRequest;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import com.ecommerce.commerceapi.inventory.service.InventoryService;
import com.ecommerce.commerceapi.orders.api.OrderRequest;
import com.ecommerce.commerceapi.orders.api.OrderUpdateRequest;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.masters.domain.Color;
import com.ecommerce.commerceapi.masters.domain.Size;
import com.ecommerce.commerceapi.products.domain.Product;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import com.ecommerce.commerceapi.returns.repository.ReturnRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final ReturnRecordRepository returns = mock(ReturnRecordRepository.class);
    private final AtomicInteger stock = new AtomicInteger(50);
    private final ProductVariant variant = variant(1L);
    private final Order order = orderWithQuantity(2);
    private final OrderService service = new OrderService(orders, variants, inventory, returns);

    @BeforeEach
    void setUp() {
        stock.set(50);
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(variants.findById(1L)).thenReturn(Optional.of(variant));
        when(returns.returnedQuantity(1L)).thenReturn(0);
        doAnswer(invocation -> {
            InventoryTransactionRequest request = invocation.getArgument(0);
            int next = request.transactionType() == InventoryTransactionType.SALE
                    ? stock.addAndGet(-request.quantity())
                    : stock.addAndGet(request.quantity());
            if (next < 0) {
                throw new IllegalArgumentException("Insufficient sellable stock");
            }
            return null;
        }).when(inventory).record(any(InventoryTransactionRequest.class));
    }

    @Test
    void reconcilesStockWhenQuantityChangesThenOrderIsCancelled() {
        service.updateStatus(10L, OrderStatus.PROCESSING);
        assertEquals(48, stock.get());

        service.update(10L, updateRequest(5));
        assertEquals(45, stock.get());

        service.updateStatus(10L, OrderStatus.CANCELLED);
        assertEquals(50, stock.get());
    }

    @Test
    void rejectsInsufficientStockWithoutMarkingTheOrderAsDeducted() {
        order.getItems().getFirst().setQuantity(51);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, OrderStatus.PROCESSING));

        assertFalse(order.isInventoryDeducted());
    }

    @Test
    void rejectsDuplicatePlatformOrderIds() {
        when(orders.findByPlatformAndOrderId(OrderPlatform.MEESHO, "MSH-20260805-0001"))
                .thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> service.create(new OrderRequest(
                " MSH-20260805-0001 ", OrderPlatform.MEESHO, LocalDate.now(), null, null, null, null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of())));
    }

    @Test
    void exposesReturnabilityDetailsInOrderDetail() {
        when(returns.returnedQuantity(1L)).thenReturn(1);

        var detail = service.findDetailById(10L);
        var item = detail.items().get(0);

        assertEquals("Men's T-Shirt", item.productName());
        assertEquals("CK-MTS-0001-BLK-M", item.sku());
        assertEquals(1, item.returnedQuantity());
        assertEquals(1, item.remainingReturnableQuantity());
    }

    private OrderUpdateRequest updateRequest(int quantity) {
        return new OrderUpdateRequest(LocalDate.now(), null, null, null, null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(new OrderUpdateRequest.Item(1L, quantity, BigDecimal.TEN)));
    }

    private static ProductVariant variant(Long id) {
        Product product = new Product();
        product.setProductName("Men's T-Shirt");
        Color color = new Color();
        color.setName("Black");
        Size size = new Size();
        size.setName("M");
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        variant.setProduct(product);
        variant.setColor(color);
        variant.setSize(size);
        variant.setSku("CK-MTS-0001-BLK-M");
        return variant;
    }

    private Order orderWithQuantity(int quantity) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setOrderId("MSH-20260805-0001");
        order.setPlatform(OrderPlatform.MEESHO);
        order.setOrderDate(LocalDate.now());
        order.setCommission(BigDecimal.ZERO);
        order.setShippingCharge(BigDecimal.ZERO);
        order.setOtherCharges(BigDecimal.ZERO);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setVariant(variant);
        item.setSkuSnapshot(variant.getSku());
        item.setProductNameSnapshot("Men's T-Shirt");
        item.setQuantity(quantity);
        item.setSellingPrice(BigDecimal.TEN);
        item.setLineTotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)));
        order.getItems().add(item);
        return order;
    }
}
