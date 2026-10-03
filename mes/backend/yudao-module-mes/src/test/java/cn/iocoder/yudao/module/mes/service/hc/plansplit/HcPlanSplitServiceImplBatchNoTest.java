package cn.iocoder.yudao.module.mes.service.hc.plansplit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HcPlanSplitServiceImplBatchNoTest {

    private final HcPlanSplitServiceImpl service = new HcPlanSplitServiceImpl();

    @Test
    void shouldNormalizeSegmentPieceSerialWithOptionalSizeSuffix() {
        assertEquals("W26G029AP", service.normalizeSegmentBatchNo("W26G029AP002"));
        assertEquals("W26G029AP", service.normalizeSegmentBatchNo("W26G029AP002A"));
        assertEquals("W26G029AQ", service.normalizeSegmentBatchNo("w26g029aq002b"));
    }

    @Test
    void shouldNormalizeDerivedBatchSuffixBeforePieceSerial() {
        assertEquals("W26G029AP", service.normalizeSegmentBatchNo("W26G029AP002A-J1"));
        assertEquals("W26G029AR", service.normalizeSegmentBatchNo("W26G029AR002B-S1"));
    }

    @Test
    void shouldKeepSegmentBatchWithoutPieceSerial() {
        assertEquals("W26G029AP", service.normalizeSegmentBatchNo("W26G029AP"));
    }
}
