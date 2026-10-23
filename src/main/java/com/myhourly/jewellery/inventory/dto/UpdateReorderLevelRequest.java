package com.myhourly.jewellery.inventory.dto;

import jakarta.validation.constraints.Min;

public class UpdateReorderLevelRequest {

    @Min(0)
    private int reorderLevel;

    public UpdateReorderLevelRequest() { }

    public UpdateReorderLevelRequest(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}
