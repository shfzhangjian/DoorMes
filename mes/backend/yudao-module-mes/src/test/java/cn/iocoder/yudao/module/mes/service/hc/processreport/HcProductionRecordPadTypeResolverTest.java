package cn.iocoder.yudao.module.mes.service.hc.processreport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HcProductionRecordPadTypeResolverTest {

    private final HcProductionRecordPadTypeResolver resolver = new HcProductionRecordPadTypeResolver();

    @Test
    void shouldResolveResearchPadTypeFromGuideClothLineCode() {
        assertEquals(HcProductionRecordPadTypeResolver.BLACK_PAD,
                resolver.resolveByGuideClothLineCode("BLACK"));
        assertEquals(HcProductionRecordPadTypeResolver.WHITE_PAD,
                resolver.resolveByGuideClothLineCode("WHITE"));
        assertEquals(HcProductionRecordPadTypeResolver.BLACK_PAD,
                resolver.resolveByGuideClothLineCode("B"));
        assertEquals(HcProductionRecordPadTypeResolver.WHITE_PAD,
                resolver.resolveByGuideClothLineCode("W"));
        assertNull(resolver.resolveByGuideClothLineCode("UNKNOWN"));
    }
}
