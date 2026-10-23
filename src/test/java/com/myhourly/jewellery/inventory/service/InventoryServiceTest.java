package com.myhourly.jewellery.inventory.service;

import com.myhourly.jewellery.inventory.dto.AdjustStockRequest;
import com.myhourly.jewellery.inventory.entity.Inventory;
import com.myhourly.jewellery.inventory.event.LowStockDetectedEvent;
import com.myhourly.jewellery.inventory.event.OrderEvent;
import com.myhourly.jewellery.inventory.event.OrderItemPayload;
import com.myhourly.jewellery.inventory.exception.InsufficientStockException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks InventoryService service;

    private OrderEvent confirmed(String eventId, long productId, int qty) {
        return new OrderEvent(eventId, "OrderConfirmed", "ORD-1", List.of(new OrderItemPayload(productId, qty)));
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
        assertThrows(IllegalArgumentException.class,
                () -> service.adjust(10L, new AdjustStockRequest(0, "oops"), "admin"));
    }
}
