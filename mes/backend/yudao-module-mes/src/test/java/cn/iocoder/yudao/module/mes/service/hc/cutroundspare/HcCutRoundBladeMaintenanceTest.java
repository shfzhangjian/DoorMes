package cn.iocoder.yudao.module.mes.service.hc.cutroundspare;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcCutRoundBladeMaintenanceTest {
    @InjectMocks HcCutRoundSpareServiceImpl service;
    @Mock HcEquipmentMapper hcEquipmentMapper;
    @Mock HcCutRoundSpareMapper hcCutRoundSpareMapper;
    @Mock HcCutRoundSpareRecordMapper hcCutRoundSpareRecordMapper;
    LocalDateTime oldTime=LocalDateTime.of(2026,9,20,10,0);
    void ready() {
        when(hcEquipmentMapper.selectById(5L)).thenReturn(HcEquipmentDO.builder().id(5L).tenantId(1L).equipmentCode("CUT").build());
        var state=HcCutRoundSpareDO.builder().id(4L).tenantId(1L).equipmentId(5L).spareType("CUTTING_BLADE")
                .useCount(500).lastReplaceTime(oldTime).batchNo("B001").limitCount(5000).limitDays(0).build();
        when(hcCutRoundSpareMapper.selectById(4L)).thenReturn(state);
        when(hcCutRoundSpareMapper.selectForUpdate(5L,"CUTTING_BLADE")).thenReturn(state);
    }
    HcCutRoundSpareSaveReqVO request() {
        var req=new HcCutRoundSpareSaveReqVO();req.setId(4L);req.setEquipmentId(5L);req.setSpareType("CUTTING_BLADE");
        req.setUseCount(0);req.setLastReplaceTime(oldTime.plusDays(1));req.setLimitCount(5000);
        req.setOperatorId(20L);req.setOperatorName("测试");return req;
    }
    @Test void normalMaintenanceCannotResetLifeOrEraseReceiptSource() {
        ready();service.save(request());var state=ArgumentCaptor.forClass(HcCutRoundSpareDO.class);
        verify(hcCutRoundSpareMapper).updateById(state.capture());assertEquals(500,state.getValue().getUseCount());
        assertEquals(oldTime,state.getValue().getLastReplaceTime());assertEquals("B001",state.getValue().getBatchNo());
    }
    @Test void explicitCorrectionMustHaveReason() {
        ready();var req=request();req.setCorrectHistory(true);
        assertThrows(RuntimeException.class,()->service.save(req));verify(hcCutRoundSpareMapper,never()).updateById(any(HcCutRoundSpareDO.class));
    }
    @Test void explicitCorrectionRecordsBeforeAfterAndReason() {
        ready();var req=request();req.setCorrectHistory(true);req.setReplaceReason("纠正历史录入错误");service.save(req);
        var record=ArgumentCaptor.forClass(HcCutRoundSpareRecordDO.class);verify(hcCutRoundSpareRecordMapper).insert(record.capture());
        assertEquals("ADJUST",record.getValue().getEventType());assertEquals(500,record.getValue().getBeforeUseCount());
        assertEquals(0,record.getValue().getAfterUseCount());assertTrue(record.getValue().getRemark().contains("纠正历史录入错误"));
        assertTrue(record.getValue().getRemark().contains(oldTime.toString()));assertNull(record.getValue().getConsumeId());
    }
}
