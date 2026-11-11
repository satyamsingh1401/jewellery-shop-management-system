package com.myhourly.jewellery.inventory.service;

import com.myhourly.jewellery.inventory.dto.AdjustStockRequest;
import com.myhourly.jewellery.inventory.entity.CancelledOrder;
import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.entity.StockMovement;
import com.myhourly.jewellery.inventory.entity.Inventory;
import com.myhourly.jewellery.inventory.event.LowStockDetectedEvent;
import com.myhourly.jewellery.inventory.event.OrderEvent;
import com.myhourly.jewellery.inventory.event.OrderItemPayload;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.InsufficientStockException;
import com.myhourly.jewellery.inventory.repository.CancelledOrderRepository;
import com.myhourly.jewellery.inventory.repository.InventoryRepository;
import com.myhourly.jewellery.inventory.repository.ProcessedEventRepository;
import com.myhourly.jewellery.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CancelledOrderRepository cancelledOrderRepository;
    @InjectMocks InventoryService service;

    private OrderEvent confirmed(String eventId, long productId, int qty) {
        return new OrderEvent(eventId, "OrderConfirmed", "ORD-1", List.of(new OrderItemPayload(productId, qty)));
    }

    private OrderEvent cancelled(String eventId, long productId, int qty) {
        return new OrderEvent(eventId, "OrderCancelled", "ORD-1", List.of(new OrderItemPayload(productId, qty)));
    }

    @Test
    void confirmForAlreadyCancelledOrderDeductsNothingButIsRecorded() {
        when(cancelledOrderRepository.existsById("ORD-1")).thenReturn(true);

        service.processOrderEvent(confirmed("evt-c1", 10L, 2));

        verify(inventoryRepository, never()).deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class));
        verify(movementRepository, never()).save(any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void confirmWithInvalidLineIsRejectedBeforeCancelCheck() {
        assertThrows(IllegalArgumentException.class, () -> service.processOrderEvent(
                new OrderEvent("evt-c2", "OrderConfirmed", "ORD-1", List.of(new OrderItemPayload(10L, 0)))));
        verify(processedEventRepository, never()).save(any());
        verify(cancelledOrderRepository, never()).existsById(anyString());
    }

    @Test
    void cancelWithoutSaleRemembersTheOrderAndRestoresNothing() {
        when(movementRepository.findByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
                10L, MovementType.SALE, "ORDER", "ORD-1")).thenReturn(Optional.empty());

        service.processOrderEvent(cancelled("evt-x1", 10L, 3));

        verify(cancelledOrderRepository).save(any(CancelledOrder.class));
        verify(inventoryRepository, never()).addQuantity(anyLong(), anyInt(), any(LocalDateTime.class));
        verify(processedEventRepository).save(any());
    }

    @Test
    void cancelForAlreadyCancelledOrderDoesNotInsertMarkerAgain() {
        when(cancelledOrderRepository.existsById("ORD-1")).thenReturn(true);
        when(movementRepository.findByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
                10L, MovementType.SALE, "ORDER", "ORD-1")).thenReturn(Optional.empty());

        service.processOrderEvent(cancelled("evt-x2", 10L, 3));

        verify(cancelledOrderRepository, never()).save(any());
    }

    @Test
    void cancelAfterSaleRestoresDeductedQuantityAndRemembersTheOrder() {
        StockMovement sale = new StockMovement(10L, MovementType.SALE, -3, 7, "ORDER", "ORD-1", "r", "u");
        when(movementRepository.findByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
                10L, MovementType.SALE, "ORDER", "ORD-1")).thenReturn(Optional.of(sale));
        when(inventoryRepository.addQuantity(eq(10L), eq(3), any(LocalDateTime.class))).thenReturn(1);
        when(inventoryRepository.findAvailableQuantity(10L)).thenReturn(Optional.of(10));

        service.processOrderEvent(cancelled("evt-x3", 10L, 500));

        verify(inventoryRepository).addQuantity(eq(10L), eq(3), any(LocalDateTime.class));
        verify(cancelledOrderRepository).save(any(CancelledOrder.class));
    }

    @Test
    void caseVariantEventTypeIsRejectedAndNotRecorded() {
        for (String type : List.of("orderConfirmed", "ORDERCANCELLED", "OrderConfirmed ")) {
            assertThrows(IllegalArgumentException.class, () -> service.processOrderEvent(
                    new OrderEvent("evt-t", type, "ORD-1", List.of(new OrderItemPayload(10L, 1)))));
        }
        verify(processedEventRepository, never()).save(any());
        verify(inventoryRepository, never()).deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    void unrelatedEventTypeIsIgnoredAndNotRecorded() {
        service.processOrderEvent(new OrderEvent("evt-u", "OrderShipped", "ORD-1",
                List.of(new OrderItemPayload(10L, 1))));

        verify(processedEventRepository, never()).save(any());
        verify(processedEventRepository, never()).existsById(anyString());
        verify(inventoryRepository, never()).deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    void overLongIdsAreRejectedBeforeAnyWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.processOrderEvent(
                new OrderEvent("e".repeat(65), "OrderConfirmed", "ORD-1", List.of(new OrderItemPayload(10L, 1)))));
        assertThrows(IllegalArgumentException.class, () -> service.processOrderEvent(
                new OrderEvent("evt-l", "OrderCancelled", "o".repeat(65), List.of(new OrderItemPayload(10L, 1)))));
        verify(processedEventRepository, never()).save(any());
        verify(cancelledOrderRepository, never()).save(any());
    }

    @Test
    void duplicateEventIsIgnoredAndStockIsNotDeductedTwice() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        service.processOrderEvent(confirmed("evt-1", 10L, 2));

        verify(inventoryRepository, never()).deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    void insufficientStockIsRejected() {
        when(inventoryRepository.deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class))).thenReturn(0);
        when(inventoryRepository.existsByProductId(10L)).thenReturn(true);

        assertThrows(InsufficientStockException.class,
                () -> service.processOrderEvent(confirmed("evt-2", 10L, 5)));
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void adjustBeyondAvailableStockThrowsInsufficientStock() {
        when(inventoryRepository.deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class))).thenReturn(0);
        when(inventoryRepository.existsByProductId(1L)).thenReturn(true);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> service.adjust(1L, new AdjustStockRequest(-100, "Excess stock deduction test"), "admin"));
        assertEquals("Insufficient stock for product 1 (requested 100)", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void lowStockEventIsPublishedWhenStockCrossesReorderLevel() {
        // before: 6 (above level 5)  ->  deduct 2  ->  after: 4 (at/below level 5)
        when(inventoryRepository.deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(inventoryRepository.findAvailableQuantity(10L)).thenReturn(Optional.of(4));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(new Inventory(10L, 4, 5)));

        service.processOrderEvent(confirmed("evt-3", 10L, 2));

        verify(eventPublisher).publishEvent(any(LowStockDetectedEvent.class));
    }

    @Test
    void lowStockEventIsNotRepeatedWhenAlreadyLow() {
        // before: 4 (already low)  ->  deduct 1  ->  after: 3
        when(inventoryRepository.deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(inventoryRepository.findAvailableQuantity(10L)).thenReturn(Optional.of(3));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(new Inventory(10L, 3, 5)));

        service.processOrderEvent(confirmed("evt-4", 10L, 1));

        verify(eventPublisher, never()).publishEvent(any(LowStockDetectedEvent.class));
    }

    @Test
    void zeroAdjustmentIsRejected() {
        assertThrows(BusinessValidationException.class,
                () -> service.adjust(10L, new AdjustStockRequest(0, "oops"), "admin"));
    }

    @Test
    void availabilityCheckRejectsZeroAndNegativeQuantity() {
        assertThrows(BusinessValidationException.class, () -> service.checkAvailability(10L, 0));
        assertThrows(BusinessValidationException.class, () -> service.checkAvailability(10L, -5));
    }

    @Test
    void adjustOfIntegerMinValueIsRejectedBeforeTouchingStock() {
        assertThrows(BusinessValidationException.class,
                () -> service.adjust(10L, new AdjustStockRequest(Integer.MIN_VALUE, "edge"), "admin"));
        verify(inventoryRepository, never()).deductIfAvailable(anyLong(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    void additionBeyondStorableMaximumIsRejectedWithoutMovement() {
        when(inventoryRepository.addQuantity(anyLong(), anyInt(), any(LocalDateTime.class))).thenReturn(0);
        when(inventoryRepository.existsByProductId(10L)).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.adjust(10L, new AdjustStockRequest(5, "recount"), "admin"));
        verify(movementRepository, never()).save(any());
    }
}
