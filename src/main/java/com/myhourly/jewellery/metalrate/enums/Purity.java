package com.myhourly.jewellery.metalrate.enums;

public enum Purity {
    K18(MetalType.GOLD),
    K22(MetalType.GOLD),
    K24(MetalType.GOLD),
    SILVER_925(MetalType.SILVER),
    PLATINUM_950(MetalType.PLATINUM);

    private final MetalType metalType;

    Purity(MetalType metalType) {
        this.metalType = metalType;
    }

    public MetalType getMetalType() {
        return metalType;
    }

    public boolean belongsTo(MetalType candidate) {
        return metalType == candidate;
    }
}
