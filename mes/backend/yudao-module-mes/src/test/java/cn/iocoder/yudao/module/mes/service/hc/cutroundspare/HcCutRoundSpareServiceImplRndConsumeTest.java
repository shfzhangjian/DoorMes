package cn.iocoder.yudao.module.mes.service.hc.cutroundspare;

import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcCutRoundSpareServiceImplRndConsumeTest {

    @InjectMocks
    private HcCutRoundSpareServiceImpl service;

    @Mock
    private HcEquipmentMapper hcEquipmentMapper;
    @Mock
    private HcCutRoundSpareMapper hcCutRoundSpareMapper;
    @Mock
    private HcCutRoundSpareRecordMapper hcCutRoundSpareRecordMapper;

    @Test
    void shouldIncreaseBladeAndFeltAndSaveOneGroupedManualConsumption() {
        LocalDateTime consumeTime = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0)
                .withNano(0);
        HcCutRoundSpareDO blade = spare(10L, "CUTTING_BLADE", 100, null, consumeTime.minusDays(2));
        HcCutRoundSpareDO felt = spare(11L, "CUTTING_FELT", 200, consumeTime.minusDays(7), consumeTime.minusDays(2));
        when(hcEquipmentMapper.selectById(1L)).thenReturn(HcEquipmentDO.builder()
                .id(1L)
                .equipmentCode("CUT-01")
                .equipmentName("裁切机01")
                .tenantId(1L)
                .build());
        when(hcCutRoundSpareMapper.selectForUpdate(1L, "CUTTING_BLADE")).thenReturn(blade);
        when(hcCutRoundSpareMapper.selectForUpdate(1L, "CUTTING_FELT")).thenReturn(felt);

        String groupNo = service.saveRndConsume(request(consumeTime));

        assertEquals(110, blade.getUseCount());
        assertEquals(210, felt.getUseCount());
        verify(hcCutRoundSpareMapper).updateById(blade);
        verify(hcCutRoundSpareMapper).updateById(felt);
        ArgumentCaptor<HcCutRoundSpareRecordDO> recordCaptor =
                ArgumentCaptor.forClass(HcCutRoundSpareRecordDO.class);
        verify(hcCutRoundSpareRecordMapper, org.mockito.Mockito.times(2)).insert(recordCaptor.capture());
        List<HcCutRoundSpareRecordDO> records = recordCaptor.getAllValues();
        HcCutRoundSpareRecordDO bladeRecord = records.stream()
                .filter(item -> "CUTTING_BLADE".equals(item.getSpareType()))
                .findFirst()
                .orElseThrow();
        HcCutRoundSpareRecordDO feltRecord = records.stream()
                .filter(item -> "CUTTING_FELT".equals(item.getSpareType()))
                .findFirst()
                .orElseThrow();
        assertTrue(groupNo.startsWith("RND-"));
        assertEquals(groupNo, bladeRecord.getRecordGroupNo());
        assertEquals(groupNo, feltRecord.getRecordGroupNo());
        assertEquals("RND_MANUAL_USE", bladeRecord.getEventType());
        assertEquals("RND_MANUAL", bladeRecord.getRecordSource());
        assertEquals("RND-MODEL", bladeRecord.getModelCode());
        assertEquals("BLACK_PAD", bladeRecord.getPadType());
        assertEquals("BLACK_PAD", feltRecord.getPadType());
        assertEquals("RND-BATCH-001", bladeRecord.getProductionBatchNo());
        assertEquals(10, bladeRecord.getChangeUseCount());
        assertEquals(7, feltRecord.getFeltUseDays());
    }

    @Test
    void shouldRejectMissingOrInvalidPadTypeBeforeUpdatingConsumables() {
        for (String padType : new String[] {null, "", "COMMON", "UNCLASSIFIED"}) {
            var req = request(LocalDateTime.now().minusDays(1));
            req.setPadType(padType);
            org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.saveRndConsume(req));
        }
        org.mockito.Mockito.verifyNoInteractions(hcEquipmentMapper, hcCutRoundSpareMapper, hcCutRoundSpareRecordMapper);
    }

    private HcCutRoundSpareRndConsumeSaveReqVO request(LocalDateTime consumeTime) {
        HcCutRoundSpareRndConsumeSaveReqVO reqVO = new HcCutRoundSpareRndConsumeSaveReqVO();
        reqVO.setEquipmentId(1L);
        reqVO.setModelCode("RND-MODEL");
        reqVO.setPadType("BLACK_PAD");
        reqVO.setProductionBatchNo("RND-BATCH-001");
        reqVO.setCutSizeMm("775");
        reqVO.setCutInputPcs(10);
        reqVO.setCutOutputPcs(9);
        reqVO.setConsumeTime(consumeTime);
        reqVO.setRemark("研发裁切消耗");
        return reqVO;
    }

    private HcCutRoundSpareDO spare(Long id, String spareType, int useCount, LocalDateTime lastReplaceTime,
                                    LocalDateTime lastEventTime) {
        return HcCutRoundSpareDO.builder()
                .id(id)
                .equipmentId(1L)
                .equipmentCode("CUT-01")
                .equipmentName("裁切机01")
                .spareType(spareType)
                .onlineQuantity(java.math.BigDecimal.ONE)
                .useCount(useCount)
                .limitCount("CUTTING_BLADE".equals(spareType) ? 5000 : 2000)
                .limitDays("CUTTING_BLADE".equals(spareType) ? null : 90)
                .status("ACTIVE")
                .lastReplaceTime(lastReplaceTime)
                .lastEventTime(lastEventTime)
                .tenantId(1L)
                .build();
    }
}
