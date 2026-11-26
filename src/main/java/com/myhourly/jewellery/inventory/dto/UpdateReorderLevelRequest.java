package com.myhourly.jewellery.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateReorderLevelRequest {

    @NotNull
    @Min(0)
    private Integer reorderLevel;

    public UpdateReorderLevelRequest() { }

    public UpdateReorderLevelRequest(Integer reorderLevel) { this.reorderLevel = reorderLevel; }

    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
}
