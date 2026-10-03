package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcGrindingConsumableSyncOrderTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 21, 19, 7, 39);
    private static final String SYNC = "GRINDING_PRODUCTION_RECORD_CONSUMABLE_SYNC";
    @InjectMocks private HcGrindingProductionRecordLedgerServiceImpl service;
    @Mock private HcEquipmentConsumableEventMapper consumableEventMapper;
    @Mock private HcGrindingProductionLedgerMapper ledgerMapper;
    @Mock private HcEquipmentConsumableStateMapper consumableStateMapper;
    @Mock private HcEquipmentMapper equipmentMapper;

    @ParameterizedTest @ValueSource(strings = {"GRINDING_PRODUCTION_RECORD_CONSUMABLE_SYNC", "GRINDING_PRODUCTION_RECORD_CONFIRM"})
    void olderCompletionIsAllowedDespiteLaterAudit(String bizType) {
        var old = record(307L, TIME.minusDays(1));
        var audit = event("ADJUST", TIME.plusSeconds(68)); audit.setBizType(bizType); audit.setBizId(307L);
        candidates(audit); when(ledgerMapper.selectById(307L)).thenReturn(old);
        assertFalse(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @ParameterizedTest @ValueSource(strings = {"USE", "REPLACE", "ADJUST"})
    void realLaterEventCannotBeHiddenBehindOldRecordSync(String eventType) {
        var actual = event(eventType, TIME.plusSeconds(1)); actual.setBizType(""); actual.setBizId(null);
        candidates(actual, event("ADJUST", TIME.plusSeconds(68)));
        assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
        verifyNoInteractions(ledgerMapper);
    }

    @Test void laterRealReplacementBlocksEvenWhenCountersAlreadyMatch() {
        var source = record(318L, TIME); var state = state();
        state.setUsedLength(source.getSandpaperLife()); state.setLastReplaceTime(TIME);
        preparePublicSync(source, state);
        var actual = event("REPLACE", TIME.plusSeconds(68)); candidates(actual);
        assertThrows(ServiceException.class, () -> service.syncLatestConfirmedConsumableState(request()));
        verify(consumableStateMapper, never()).updateById(any(HcEquipmentConsumableStateDO.class));
    }

    @Test void repeatedSameSourceIsIdempotent() {
        var source = record(318L, TIME); var state = state();
        state.setUsedLength(source.getSandpaperLife()); state.setLastReplaceTime(TIME);
        preparePublicSync(source, state);
        var cloth = state(); cloth.setId(7L); cloth.setConsumableType("GUIDE_CLOTH");
        cloth.setBatchNo("CLOTH"); cloth.setUseCount(110); cloth.setLastEventTime(TIME);
        when(consumableStateMapper.selectOneByEquipmentAndType(7L, "ROUGH_GRINDING", "GUIDE_CLOTH")).thenReturn(cloth);
        var audit = event("ADJUST", TIME.plusSeconds(68)); audit.setBizId(318L);
        candidates(audit); when(ledgerMapper.selectById(318L)).thenReturn(source);
        var response = service.syncLatestConfirmedConsumableState(request());
        assertFalse(response.getSynced());
        verify(consumableStateMapper, never()).updateById(any(HcEquipmentConsumableStateDO.class));
        verify(consumableEventMapper, never()).insert(any(HcEquipmentConsumableEventDO.class));
    }

    @Test void genuineBatchConflictStillBlocks() {
        var source = record(318L, TIME); var state = state(); state.setBatchNo("OTHER");
        preparePublicSync(source, state);
        var audit = event("ADJUST", TIME.plusSeconds(68)); candidates(audit);
        when(ledgerMapper.selectById(307L)).thenReturn(record(307L, TIME.minusDays(1)));
        assertThrows(ServiceException.class, () -> service.syncLatestConfirmedConsumableState(request()));
    }

    @Test void sourceCompletionLaterThanTargetBlocksEvenWithEarlierAudit() {
        var audit = event("ADJUST", TIME.minusHours(1)); candidates(audit);
        when(ledgerMapper.selectById(307L)).thenReturn(record(307L, TIME.plusHours(1)));
        assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @Test void missingSourceBlocks() {
        candidates(event("ADJUST", TIME.plusSeconds(68)));
        assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @ParameterizedTest @ValueSource(strings = {"tenant", "equipment", "status", "deleted", "time"})
    void invalidSourceCannotAuthorizeOverwrite(String invalid) {
        var source = record(307L, TIME.minusDays(1));
        switch (invalid) {
            case "tenant" -> source.setTenantId(2L);
            case "equipment" -> source.setEquipmentId(8L);
            case "status" -> source.setStatus("WAIT_CONFIRM");
            case "deleted" -> source.setDeleted(true);
            case "time" -> source.setRecordTime(null);
        }
        candidates(event("ADJUST", TIME.plusSeconds(68)));
        when(ledgerMapper.selectById(307L)).thenReturn(source);
        assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @Test void missingAuditForNewerStateBlocks() {
        candidates(); assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @Test void sourceSegmentOrderingMatchesLatestRecordQuery() {
        var source = record(307L, TIME); source.setSourceSegmentNo(2);
        candidates(event("ADJUST", TIME.plusSeconds(68)));
        when(ledgerMapper.selectById(307L)).thenReturn(source);
        assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @Test void sameTimeRecordIdBreaksTie() {
        candidates(event("ADJUST", TIME.plusSeconds(68)));
        when(ledgerMapper.selectById(307L)).thenReturn(record(319L, TIME));
        assertTrue(service.isStateNewerThanRecord(state(), record(318L, TIME)));
    }

    @Test void automaticConfirmationUsesSameCompletionOrderGuard() {
        // 两个自动回写入口与手动同步共用此判断，旧审计不能阻止新记录累计值回写。
        var audit = event("ADJUST", TIME.plusSeconds(68)); candidates(audit);
        when(ledgerMapper.selectById(307L)).thenReturn(record(307L, TIME.minusDays(1)));
        var state = state(); var equipment = new HcEquipmentDO(); equipment.setId(7L);
        when(consumableStateMapper.selectOneByEquipmentAndType(7L, "ROUGH_GRINDING", "SANDPAPER")).thenReturn(state);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "syncSandpaperState",
                record(318L, TIME), equipment, 1L, "test");
        assertEquals(new BigDecimal("114"), state.getUsedLength());
        verify(consumableStateMapper).updateById(state);
        verify(consumableEventMapper).insert(any(HcEquipmentConsumableEventDO.class));
    }

    private void preparePublicSync(HcGrindingProductionRecordDO source, HcEquipmentConsumableStateDO state) {
        var equipment = new HcEquipmentDO(); equipment.setId(7L); equipment.setTenantId(1L);
        when(equipmentMapper.selectById(7L)).thenReturn(equipment);
        when(ledgerMapper.selectLatestConfirmedByEquipment(7L)).thenReturn(source);
        when(consumableStateMapper.selectOneByEquipmentAndType(7L, "ROUGH_GRINDING", "SANDPAPER")).thenReturn(state);
    }
    private HcGrindingProductionRecordConsumableSyncReqVO request() {
        var request = new HcGrindingProductionRecordConsumableSyncReqVO(); request.setEquipmentId(7L); return request;
    }
    private void candidates(HcEquipmentConsumableEventDO... events) {
        when(consumableEventMapper.selectConsumableSyncCandidates(5L, 1L, TIME)).thenReturn(List.of(events));
    }
    private HcGrindingProductionRecordDO record(Long id, LocalDateTime time) {
        var record = new HcGrindingProductionRecordDO(); record.setId(id); record.setEquipmentId(7L);
        record.setTenantId(1L); record.setStatus("CONFIRMED"); record.setRecordTime(time); record.setSourceSegmentNo(1);
        record.setSandpaperLife(new BigDecimal("114")); record.setSandpaperLifeDays(1); record.setGuideClothLife(110);
        record.setSandpaperBatchNo("SP"); record.setGuideClothBatchNo("CLOTH"); return record;
    }
    private HcEquipmentConsumableStateDO state() {
        var state = new HcEquipmentConsumableStateDO(); state.setId(5L); state.setTenantId(1L); state.setEquipmentId(7L);
        state.setProcessCode("ROUGH_GRINDING"); state.setConsumableType("SANDPAPER"); state.setBatchNo("SP");
        state.setUsedLength(new BigDecimal("459")); state.setUseCount(6); state.setLastEventTime(TIME.plusSeconds(68));
        return state;
    }
    private HcEquipmentConsumableEventDO event(String type, LocalDateTime time) {
        var event = new HcEquipmentConsumableEventDO(); event.setId(1073L); event.setStateId(5L);
        event.setTenantId(1L); event.setEquipmentId(7L); event.setProcessCode("ROUGH_GRINDING");
        event.setConsumableType("SANDPAPER"); event.setEventType(type); event.setEventTime(time);
        event.setBizType(SYNC); event.setBizId(307L); return event;
    }
}
