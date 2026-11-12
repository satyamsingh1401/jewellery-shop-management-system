package com.myhourly.jewellery.metalrate.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurityTest {

    @Test
    void goldPuritiesBelongToGoldOnly() {
        assertThat(Purity.K18.belongsTo(MetalType.GOLD)).isTrue();
        assertThat(Purity.K22.belongsTo(MetalType.GOLD)).isTrue();
        assertThat(Purity.K24.belongsTo(MetalType.GOLD)).isTrue();
        assertThat(Purity.K22.belongsTo(MetalType.SILVER)).isFalse();
    }

    @Test
    void silverAndPlatinumPuritiesMapToTheirMetals() {
        assertThat(Purity.SILVER_925.getMetalType()).isEqualTo(MetalType.SILVER);
        assertThat(Purity.PLATINUM_950.getMetalType()).isEqualTo(MetalType.PLATINUM);
        assertThat(Purity.SILVER_925.belongsTo(MetalType.GOLD)).isFalse();
    }

    @Test
    void constantNamesMatchProductModuleEnums() {
        assertThat(Purity.values())
                .extracting(Enum::name)
                .containsExactly("K18", "K22", "K24", "SILVER_925", "SILVER_999", "PLATINUM_950");
        assertThat(MetalType.values())
                .extracting(Enum::name)
                .containsExactly("GOLD", "SILVER", "PLATINUM");
    }
}
