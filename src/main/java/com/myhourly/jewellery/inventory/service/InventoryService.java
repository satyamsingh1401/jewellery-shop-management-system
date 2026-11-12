package com.myhourly.jewellery.inventory.service;

import com.myhourly.jewellery.inventory.dto.*;
import com.myhourly.jewellery.inventory.entity.CancelledOrder;
import com.myhourly.jewellery.inventory.entity.Inventory;
import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.entity.ProcessedEvent;
import com.myhourly.jewellery.inventory.entity.StockMovement;
import com.myhourly.jewellery.inventory.event.LowStockDetectedEvent;
import com.myhourly.jewellery.inventory.event.OrderEvent;
import com.myhourly.jewellery.inventory.event.OrderItemPayload;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.InsufficientStockException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.inventory.repository.CancelledOrderRepository;
import com.myhourly.jewellery.inventory.repository.InventoryRepository;
import com.myhourly.jewellery.inventory.repository.ProcessedEventRepository;
import com.myhourly.jewellery.inventory.repository.StockMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.myhourly.jewellery.inventory.InventoryLimits.MAX_QUANTITY_PER_MOVEMENT;

/**
 * All business rules of the Inventory module. Every public method that changes stock
 * is @Transactional: the stock change, the StockMovement row and the processed-event
 * marker are saved together or not at all.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String ORDER = "ORDER";
    private static final String KAFKA_USER = "system:kafka";
    private static final String ORDER_CONFIRMED = "OrderConfirmed";
    private static final String ORDER_CANCELLED = "OrderCancelled";
    private static final int MAX_ID_LENGTH = 64;

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CancelledOrderRepository cancelledOrderRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            StockMovementRepository movementRepository,
                            ProcessedEventRepository processedEventRepository,
                            ApplicationEventPublisher eventPublisher,
                            CancelledOrderRepository cancelledOrderRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
        this.cancelledOrderRepository = cancelledOrderRepository;
    }

    // =====================================================================
    // Use cases called from the REST controller
    // =====================================================================

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request, String user) {
        // TODO: ask the Product module whether productId exists (Satyam's module).
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new DuplicateResourceException("Inventory already exists for product " + request.getProductId());
        }
        int initialQuantity = requireInRange(request.getInitialQuantity(), "initialQuantity", 0, MAX_QUANTITY_PER_MOVEMENT);
        int reorderLevel = requireInRange(request.getReorderLevel(), "reorderLevel", 0, Integer.MAX_VALUE);
        inventoryRepository.save(new Inventory(request.getProductId(), initialQuantity, reorderLevel));
        if (initialQuantity > 0) {
            record(request.getProductId(), MovementType.STOCK_IN, initialQuantity,
                    initialQuantity, null, null, "Initial stock", user);
        }
        return InventoryResponse.from(findOrThrow(request.getProductId()));
    }

    @Transactional
    public InventoryResponse stockIn(Long productId, StockInRequest request, String user) {
        int quantity = requireInRange(request.getQuantity(), "quantity", 1, MAX_QUANTITY_PER_MOVEMENT);
        addAndRecord(productId, quantity, MovementType.STOCK_IN, null, null, request.getReason(), user);
        return InventoryResponse.from(findOrThrow(productId));
    }

    @Transactional
    public InventoryResponse adjust(Long productId, AdjustStockRequest request, String user) {
        int change = requireInRange(request.getQuantityChange(), "quantityChange",
                -MAX_QUANTITY_PER_MOVEMENT, MAX_QUANTITY_PER_MOVEMENT);
        if (change == 0) {
            throw new BusinessValidationException("quantityChange must not be zero");
        }
        if (change > 0) {
            addAndRecord(productId, change, MovementType.ADJUSTMENT, null, null, request.getReason(), user);
        } else {
            deductAndRecord(productId, Math.negateExact(change), MovementType.ADJUSTMENT, null, null, request.getReason(), user);
        }
        return InventoryResponse.from(findOrThrow(productId));
    }

    @Transactional
    public InventoryResponse updateReorderLevel(Long productId, int reorderLevel) {
        Inventory inventory = findOrThrow(productId);
        inventory.setReorderLevel(reorderLevel); // managed entity: saved automatically at commit
        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        return InventoryResponse.from(findOrThrow(productId));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStock() {
        return inventoryRepository.findLowStock().stream().map(InventoryResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovements(Long productId, Pageable pageable) {
        findOrThrow(productId); // 404 if the product has no inventory
        return movementRepository.findByProductIdOrderByCreatedAtDescIdDesc(productId, pageable)
                .map(StockMovementResponse::from);
    }

    /**
     * Pre-check used by the Order module before it confirms an order.
     * It is only a hint - the real protection is the atomic deduct that happens later.
     */
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long productId, int quantity) {
        if (quantity < 1) {
            throw new BusinessValidationException("quantity must be at least 1");
        }
        int available = inventoryRepository.findAvailableQuantity(productId)
                .orElseThrow(() -> notFound(productId));
        return available >= quantity;
    }

    // =====================================================================
    // Use case called from Kafka (OrderEventConsumer)
    // =====================================================================

    /**
     * Handles one order event exactly-once-in-effect:
     *  - already-seen eventId  -> ignored (duplicate protection, layer 1)
     *  - OrderConfirmed        -> deduct stock for every item (all or nothing)
     *  - OrderCancelled        -> put back what was deducted for that order
     *  - any other event type  -> ignored
     * If anything throws, the whole transaction rolls back, so no half-applied orders.
     */
    @Transactional
    public void processOrderEvent(OrderEvent event) {
        if (event == null || event.getEventId() == null || event.getEventType() == null
                || event.getOrderId() == null || event.getItems() == null) {
            throw new IllegalArgumentException("Order event is missing required fields");
        }
        if (event.getEventId().length() > MAX_ID_LENGTH || event.getOrderId().length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("event_id and order_id must be at most " + MAX_ID_LENGTH + " characters");
        }
        String type = event.getEventType();
        if (!ORDER_CONFIRMED.equals(type) && !ORDER_CANCELLED.equals(type)) {
            rejectNearMiss(type);
            log.info("Event {} of type {} is not handled by inventory; ignored and not recorded",
                    event.getEventId(), type);
            return;
        }
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Duplicate event {} ignored", event.getEventId());
            return;
        }
        if (ORDER_CONFIRMED.equals(type)) {
            deductForOrder(event.getOrderId(), event.getItems());
        } else {
            restoreForOrder(event.getOrderId(), event.getItems());
        }
        processedEventRepository.save(new ProcessedEvent(event.getEventId(), type));
    }

    // =====================================================================
    // Internal helpers
    // =====================================================================

    private void rejectNearMiss(String type) {
        String normalized = type.strip();
        if (ORDER_CONFIRMED.equalsIgnoreCase(normalized) || ORDER_CANCELLED.equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Unsupported event_type '" + type
                    + "'; expected exactly " + ORDER_CONFIRMED + " or " + ORDER_CANCELLED);
        }
    }

    private void deductForOrder(String orderId, List<OrderItemPayload> items) {
        Map<Long, Integer> totals = totalsByProduct(items);
        if (cancelledOrderRepository.existsById(orderId)) {
            log.warn("Order {} was already cancelled; confirmation ignored, no stock deducted", orderId);
            return;
        }
        for (Map.Entry<Long, Integer> line : totals.entrySet()) {
            // Duplicate protection, layer 2: this order already deducted this product.
            if (movementRepository.existsByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
                    line.getKey(), MovementType.SALE, ORDER, orderId)) {
                log.info("Order {} already deducted for product {}", orderId, line.getKey());
                continue;
            }
            deductAndRecord(line.getKey(), line.getValue(), MovementType.SALE, ORDER, orderId,
                    "Order " + orderId + " confirmed", KAFKA_USER);
        }
    }

    private void restoreForOrder(String orderId, List<OrderItemPayload> items) {
        Map<Long, Integer> totals = totalsByProduct(items);
        markCancelled(orderId);
        for (Long productId : totals.keySet()) {
            Optional<StockMovement> sale = movementRepository
                    .findByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
                            productId, MovementType.SALE, ORDER, orderId);
            if (sale.isEmpty()) {
                log.info("Order {} never deducted product {} - nothing to restore", orderId, productId);
                continue;
            }
            if (movementRepository.existsByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
                    productId, MovementType.RETURN, ORDER, orderId)) {
                continue; // already restored
            }
            // Restore exactly what was deducted, not what the cancel event claims.
            addAndRecord(productId, Math.abs(sale.get().getQuantityChange()), MovementType.RETURN,
                    ORDER, orderId, "Order " + orderId + " cancelled", KAFKA_USER);
        }
    }

    private void markCancelled(String orderId) {
        if (!cancelledOrderRepository.existsById(orderId)) {
            cancelledOrderRepository.save(new CancelledOrder(orderId));
        }
    }

    /** Subtracts stock atomically; throws if there is not enough. Raises low-stock event if needed. */
    private void deductAndRecord(Long productId, int qty, MovementType type,
                                 String refType, String refId, String reason, String user) {
        requirePositive(qty);
        if (inventoryRepository.deductIfAvailable(productId, qty, LocalDateTime.now()) == 0) {
            if (!inventoryRepository.existsByProductId(productId)) {
                throw notFound(productId);
            }
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + " (requested " + qty + ")");
        }
        int balance = currentQuantity(productId);
        record(productId, type, -qty, balance, refType, refId, reason, user);
        raiseLowStockIfCrossed(productId, balance, qty);
    }

    private void addAndRecord(Long productId, int qty, MovementType type,
                              String refType, String refId, String reason, String user) {
        requirePositive(qty);
        if (inventoryRepository.addQuantity(productId, qty, LocalDateTime.now()) == 0) {
            if (!inventoryRepository.existsByProductId(productId)) {
                throw notFound(productId);
            }
            throw new BusinessValidationException(
                    "Adding " + qty + " units would exceed the maximum storable quantity for product " + productId);
        }
        record(productId, type, qty, currentQuantity(productId), refType, refId, reason, user);
    }

    /** Fires only when stock moves from ABOVE the reorder level to AT/BELOW it - once, not on every sale. */
    private void raiseLowStockIfCrossed(Long productId, int balanceAfter, int removed) {
        Inventory inventory = findOrThrow(productId);
        boolean lowNow = balanceAfter <= inventory.getReorderLevel();
        boolean wasLow = (balanceAfter + removed) <= inventory.getReorderLevel();
        if (lowNow && !wasLow) {
            eventPublisher.publishEvent(
                    LowStockDetectedEvent.of(productId, balanceAfter, inventory.getReorderLevel()));
        }
    }

    private static int requireInRange(Integer value, String field, int min, int max) {
        if (value == null) {
            throw new BusinessValidationException(field + " is required");
        }
        if (value < min || value > max) {
            throw new BusinessValidationException(field + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static void requirePositive(int qty) {
        if (qty <= 0) {
            throw new BusinessValidationException("quantity must be greater than zero");
        }
    }

    private void record(Long productId, MovementType type, int change, int balanceAfter,
                        String refType, String refId, String reason, String user) {
        movementRepository.save(new StockMovement(productId, type, change, balanceAfter, refType, refId, reason, user));
    }

    private int currentQuantity(Long productId) {
        return inventoryRepository.findAvailableQuantity(productId)
                .orElseThrow(() -> notFound(productId));
    }

    private ResourceNotFoundException notFound(Long productId) {
        return new ResourceNotFoundException("No inventory record for product " + productId);
    }

    private Inventory findOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> notFound(productId));
    }

    /** Merges duplicate lines of the same product and validates every line. */
    private Map<Long, Integer> totalsByProduct(List<OrderItemPayload> items) {
        Map<Long, Integer> totals = new LinkedHashMap<>();
        for (OrderItemPayload item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Order item needs a product_id and a quantity above zero");
            }
            totals.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return totals;
    }
}
