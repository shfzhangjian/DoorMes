package cn.iocoder.yudao.module.mes.service.hc.guideclothrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRuntimeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.guideclothrecord.HcGuideClothRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcProductionRecordPadTypeResolver;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineContext;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineResolverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcGuideClothRecordServiceImplTest {

    private static final String MODEL_CODE = "W33P0300";

    @InjectMocks
    private HcGuideClothRecordServiceImpl service;

    @Mock
    private HcGuideClothRecordMapper guideClothRecordMapper;

    @Mock
    private HcEquipmentConsumableStateMapper equipmentConsumableStateMapper;

    @Mock
    private HcEquipmentConsumableEventMapper equipmentConsumableEventMapper;

    @Mock
    private HcEquipmentMapper equipmentMapper;

    @Mock
    private HcToolingConsumableLedgerMapper toolingConsumableLedgerMapper;

    @Mock
    private HcProductionLineResolverService productionLineResolverService;

    @Mock
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Test
    void shouldTreatEpochPlaceholderAsMissingRuntimeTime() {
        mockWhiteLine();
        when(guideClothRecordMapper.selectCurrentByLineCodes(anyCollection()))
                .thenReturn(HcGuideClothRecordDO.builder()
                        .replaceTime(LocalDateTime.of(1970, 1, 1, 8, 0))
                        .useCount(8)
                        .currentFlag(0)
                        .build());

        HcGuideClothRuntimeRespVO runtime = service.getRuntimeByMotherModelCode(MODEL_CODE);

        assertNull(runtime.getReplaceTime());
        assertEquals(1, runtime.getWarningFlag());
        assertEquals("未记录有效的导布更换时间，请先登记导布更换", runtime.getWarningText());
    }

    @Test
    void shouldFallbackToValidLineRecordWhenEquipmentStateTimeIsEpochPlaceholder() {
        mockWhiteLine();
        LocalDateTime validReplaceTime = LocalDateTime.of(2026, 7, 5, 0, 0);
        when(guideClothRecordMapper.selectCurrentByLineCodes(anyCollection()))
                .thenReturn(HcGuideClothRecordDO.builder()
                        .replaceTime(validReplaceTime)
                        .useCount(8)
                        .currentFlag(0)
                        .build());
        when(equipmentConsumableStateMapper.selectOneByEquipmentAndType(5L, "WET", "GUIDE_CLOTH"))
                .thenReturn(HcEquipmentConsumableStateDO.builder()
                        .lastReplaceTime(LocalDateTime.of(1970, 1, 1, 8, 0))
                        .useCount(8)
                        .build());

        HcGuideClothRuntimeRespVO runtime = service.getRuntimeByMotherModelCode(MODEL_CODE, 5L);

        assertEquals(validReplaceTime, runtime.getReplaceTime());
    }

    @Test
    void shouldRejectCreateWithoutValidReplaceTime() {
        HcGuideClothRecordSaveReqVO reqVO = new HcGuideClothRecordSaveReqVO();
        reqVO.setLineName("白垫线");
        reqVO.setLineCode("WHITE");
        reqVO.setUseCount(1);
        reqVO.setCurrentFlag(0);

        assertThrows(ServiceException.class, () -> service.create(reqVO));
        verifyNoInteractions(guideClothRecordMapper);
    }

    @Test
    void shouldRejectRndConsumeWhenPetLedgerIsMarkedUsedUp() {
        HcGuideClothRndConsumeSaveReqVO reqVO = new HcGuideClothRndConsumeSaveReqVO();
        reqVO.setPetLedgerId(100L);
        when(toolingConsumableLedgerMapper.selectByIdForUpdate(100L))
                .thenReturn(HcToolingConsumableLedgerDO.builder()
                        .processCode("WET")
                        .consumableType("PET")
                        .model("PET-100")
                        .batchNo("PET-BATCH-100")
                        .usageStatus("USED_UP")
                        .build());

        assertThrows(ServiceException.class, () -> service.saveRndConsume(reqVO));
        verifyNoInteractions(guideClothRecordMapper);
    }

    @Test
    void shouldRejectRndConsumeWhenGuideClothLineHasNoPadType() {
        HcGuideClothRndConsumeSaveReqVO reqVO = new HcGuideClothRndConsumeSaveReqVO();
        reqVO.setGuideClothRecordId(200L);
        reqVO.setPetLedgerId(100L);
        when(toolingConsumableLedgerMapper.selectByIdForUpdate(100L))
                .thenReturn(HcToolingConsumableLedgerDO.builder()
                        .processCode("WET")
                        .consumableType("PET")
                        .model("PET-100")
                        .batchNo("PET-BATCH-100")
                        .usageStatus("ACTIVE")
                        .build());
        when(guideClothRecordMapper.selectById(200L)).thenReturn(HcGuideClothRecordDO.builder()
                .id(200L)
                .lineCode("UNKNOWN")
                .currentFlag(0)
                .build());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveRndConsume(reqVO));

        assertTrue(exception.getMessage().contains("未归属黑垫线或白垫线"));
        verifyNoInteractions(equipmentMapper);
    }

    @Test
    void shouldResolveHcsp10AsBlackLineFromProductModelCategory() {
        when(padTypeResolver.resolveByModelCode("HCSP10"))
                .thenReturn(HcProductionRecordPadTypeResolver.BLACK_PAD);

        HcGuideClothRuntimeRespVO runtime = service.getRuntimeByMotherModelCode("HCSP10");

        assertEquals("BLACK", runtime.getLineCode());
        assertEquals("黑垫线", runtime.getLineName());
        verifyNoInteractions(productionLineResolverService);
    }

    @Test
    void shouldInitializeBlackCurrentRecordFromEquipmentReplaceBaseline() {
        LocalDateTime actualReplaceTime = LocalDateTime.of(2026, 8, 15, 19, 10, 45);
        LocalDateTime reportEndTime = LocalDateTime.of(2026, 9, 1, 9, 49, 29);
        when(padTypeResolver.resolveByModelCode("HCSP10"))
                .thenReturn(HcProductionRecordPadTypeResolver.BLACK_PAD);
        when(guideClothRecordMapper.selectCurrentByLineCodes(anyCollection())).thenReturn(null);
        HcEquipmentConsumableStateDO state = HcEquipmentConsumableStateDO.builder()
                .id(9L)
                .equipmentId(5L)
                .equipmentCode("HC-SB-018")
                .equipmentName("CMP湿法1线")
                .batchNo("DCAThjul1")
                .lastReplaceTime(actualReplaceTime)
                .lastReplacePlanNo("20260811-001")
                .lastReplaceReason("清理水槽")
                .useCount(13)
                .usedLength(new BigDecimal("1164"))
                .limitLength(new BigDecimal("300"))
                .tenantId(1L)
                .build();
        when(equipmentConsumableStateMapper.selectOneByEquipmentAndType(5L, "WET", "GUIDE_CLOTH"))
                .thenReturn(state);
        when(equipmentMapper.selectById(5L)).thenReturn(HcEquipmentDO.builder()
                .id(5L)
                .equipmentCode("HC-SB-018")
                .equipmentName("CMP湿法1线")
                .workCenterId(1002L)
                .workCenterCode("WC-COAT")
                .workCenterName("湿法工作中心")
                .tenantId(1L)
                .build());

        service.handleWetReport("HCSP10", "20260901-001", "H4651493-B31CL", "DCAThjul1",
                "01.05.00132", null, 14, false, "", 1L, reportEndTime,
                5L, "HC-SB-018", "CMP湿法1线", 1002L, "WC-COAT", "湿法工作中心",
                120L, 960L, "WC-COAT", "湿法", new BigDecimal("100"), 1000L, 155L, "范培扬");

        ArgumentCaptor<HcGuideClothRecordDO> recordCaptor = ArgumentCaptor.forClass(HcGuideClothRecordDO.class);
        verify(guideClothRecordMapper).insert(recordCaptor.capture());
        HcGuideClothRecordDO inserted = recordCaptor.getValue();
        assertEquals("BLACK", inserted.getLineCode());
        assertEquals("黑垫线", inserted.getLineName());
        assertEquals(actualReplaceTime, inserted.getReplaceTime());
        assertEquals("20260811-001", inserted.getReplacePlanNo());
        assertEquals("清理水槽", inserted.getReplaceReason());
        assertEquals("DCAThjul1", inserted.getGuideClothBatchNo());

        ArgumentCaptor<HcEquipmentConsumableEventDO> eventCaptor =
                ArgumentCaptor.forClass(HcEquipmentConsumableEventDO.class);
        verify(equipmentConsumableEventMapper).insert(eventCaptor.capture());
        assertEquals(reportEndTime, eventCaptor.getValue().getEventTime());
    }

    @Test
    void shouldRejectInitializationWithoutValidEquipmentReplaceBaseline() {
        when(padTypeResolver.resolveByModelCode("HCSP10"))
                .thenReturn(HcProductionRecordPadTypeResolver.BLACK_PAD);
        when(guideClothRecordMapper.selectCurrentByLineCodes(anyCollection())).thenReturn(null);
        when(equipmentConsumableStateMapper.selectOneByEquipmentAndType(5L, "WET", "GUIDE_CLOTH"))
                .thenReturn(HcEquipmentConsumableStateDO.builder()
                        .equipmentId(5L)
                        .batchNo("DCAThjul1")
                        .lastReplaceTime(null)
                        .build());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.handleWetReport(
                "HCSP10", "20260901-001", "H4651493-B31CL", "DCAThjul1", "01.05.00132",
                null, 14, false, "", 1L, LocalDateTime.of(2026, 9, 1, 9, 49, 29),
                5L, "HC-SB-018", "CMP湿法1线", 1002L, "WC-COAT", "湿法工作中心",
                120L, 960L, "WC-COAT", "湿法", new BigDecimal("100"), 1000L, 155L, "范培扬"));

        assertTrue(exception.getMessage().contains("不会使用湿法完工时间代替更换时间"));
        verify(guideClothRecordMapper, never()).insert(any(HcGuideClothRecordDO.class));
    }

    private void mockWhiteLine() {
        when(productionLineResolverService.resolveByMotherModelCode(MODEL_CODE))
                .thenReturn(HcProductionLineContext.builder()
                        .lineCode("WHITE")
                        .lineShortCode("W")
                        .lineName("白垫线")
                        .build());
    }
}
