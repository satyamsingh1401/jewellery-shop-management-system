package com.myhourly.jewellery.inventory.controller;

import com.myhourly.jewellery.inventory.dto.*;
import com.myhourly.jewellery.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST layer: only HTTP concerns (URLs, validation, status codes).
 * No business rules here - they all live in InventoryService.
 * Principal is null until the Authentication module is merged; we then record "system".
 * TODO after JWT is in: add @PreAuthorize - adjust/stock-in/reorder-level for ADMIN only.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(@Valid @RequestBody CreateInventoryRequest request, Principal principal) {
        return service.createInventory(request, user(principal));
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> lowStock() {
        return service.getLowStock();
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return service.getInventory(productId);
    }

    @GetMapping("/{productId}/availability")
    public AvailabilityResponse availability(@PathVariable Long productId, @RequestParam int quantity) {
        return new AvailabilityResponse(productId, quantity, service.checkAvailability(productId, quantity));
    }

    @GetMapping("/{productId}/movements")
    public Page<StockMovementResponse> movements(@PathVariable Long productId,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return service.getMovements(productId, pageable);
    }

    @PostMapping("/{productId}/stock-in")
    public InventoryResponse stockIn(@PathVariable Long productId,
                                     @Valid @RequestBody StockInRequest request, Principal principal) {
        return service.stockIn(productId, request, user(principal));
    }

    @PostMapping("/{productId}/adjust")
    public InventoryResponse adjust(@PathVariable Long productId,
                                    @Valid @RequestBody AdjustStockRequest request, Principal principal) {
        return service.adjust(productId, request, user(principal));
    }

    @PatchMapping("/{productId}/reorder-level")
    public InventoryResponse reorderLevel(@PathVariable Long productId,
                                          @Valid @RequestBody UpdateReorderLevelRequest request) {
        return service.updateReorderLevel(productId, request.getReorderLevel());
    }

    private String user(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
