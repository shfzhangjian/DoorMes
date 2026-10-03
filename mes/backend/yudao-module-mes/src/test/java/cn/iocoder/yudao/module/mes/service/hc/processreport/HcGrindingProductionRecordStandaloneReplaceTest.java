package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
class HcGrindingProductionRecordStandaloneReplaceTest {
    private static final String LINK = "GRINDING_RECORD_SANDPAPER_REPLACE";
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 16, 10, 0);
    @InjectMocks private HcGrindingProductionRecordLedgerServiceImpl service;
    @Mock private HcEquipmentConsumableEventMapper consumableEventMapper;
    @Mock private HcGrindingProductionLedgerMapper ledgerMapper;
    @Mock private HcProductionRecordPadTypeResolver padTypeResolver;

    @org.junit.jupiter.api.BeforeAll static void initializeTableMetadata() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "standalone-replace-test"),
                HcGrindingProductionRecordDO.class);
    }

    @BeforeEach void noExistingAssociation() {
        when(consumableEventMapper.selectListByBiz(LINK, 30L)).thenReturn(List.of());
    }

    @ParameterizedTest @ValueSource(strings = {"FIRST", "SECOND"})
    void firstActualUsageReceivesReasonInAutoSync(String pass) {
        var record = record(pass);
        record.setSourceBizType("FIRST".equals(pass) ? "FIRST_ALLOCATION" : "SECOND");
        record.setRecordRole(record.getSourceBizType());
        record.setId(null);
        doAnswer(call -> { ((HcGrindingProductionRecordDO) call.getArgument(0)).setId(30L); return 1; })
                .when(ledgerMapper).insert(any(HcGrindingProductionRecordDO.class));
        prepare(record);
        when(consumableEventMapper.selectLaterUsageEvent(5L, TIME, 10L)).thenReturn(usage(pass));
        when(consumableEventMapper.bindStandaloneReplacement(10L, 1L, LINK, 30L)).thenReturn(1);
        when(ledgerMapper.update(isNull(), any())).thenReturn(1);
        service.syncAutoRecords(List.of(record));
        assertEquals("砂纸：磨损", record.getReplaceReason());
        verify(consumableEventMapper).bindStandaloneReplacement(10L, 1L, LINK, 30L);
        verify(ledgerMapper).update(isNull(), any());
    }
    @Test void laterReportCannotTakeReasonFromEarlierUsage() {
        var record = record("SECOND");
        prepare(record);
        var earlier = usage("FIRST"); earlier.setId(11L);
        when(consumableEventMapper.selectLaterUsageEvent(5L, TIME, 10L)).thenReturn(earlier);
        service.syncStandaloneSandpaperReason(record);
        assertNull(record.getReplaceReason());
        verify(consumableEventMapper, never()).bindStandaloneReplacement(anyLong(), anyLong(), anyString(), anyLong());
    }
    @Test void sameBatchPhysicalReplacementUsesEventIdentity() {
        var record = record("FIRST");
        var replacement = prepare(record);
        replacement.setBeforeBatchNo("SP-1");
        replacement.setBizType(""); replacement.setGrindingStage("");
        when(consumableEventMapper.selectLaterUsageEvent(5L, TIME, 10L)).thenReturn(usage("FIRST"));
        when(consumableEventMapper.bindStandaloneReplacement(10L, 1L, LINK, 30L)).thenReturn(1);
        when(ledgerMapper.update(isNull(), any())).thenReturn(1);
        service.syncStandaloneSandpaperReason(record);
        assertEquals("砂纸：磨损", record.getReplaceReason());
    }
    @Test void claimedEventCannotBeClaimedByAnotherRecord() {
        var record = record("FIRST");
        var replacement = prepare(record);
        replacement.setBizType(LINK); replacement.setBizId(29L);
        service.syncStandaloneSandpaperReason(record);
        assertNull(record.getReplaceReason());
        verify(consumableEventMapper, never()).selectLaterUsageEvent(anyLong(), any(), anyLong());
    }
    @Test void reportInternalReplacementIsNotStandalone() {
        var record = record("FIRST");
        prepare(record).setGrindingStage("FIRST");
        service.syncStandaloneSandpaperReason(record);
        assertNull(record.getReplaceReason());
        verify(consumableEventMapper, never()).bindStandaloneReplacement(anyLong(), anyLong(), anyString(), anyLong());
    }
    @Test void replayRestoresBoundReasonAndPreservesGuideClothReason() {
        var record = record("FIRST"); record.setReplaceReason("导布：破损");
        when(ledgerMapper.update(isNull(), any())).thenReturn(1);
        when(consumableEventMapper.selectListByBiz(LINK, 30L)).thenReturn(List.of(replacement()));
        service.syncStandaloneSandpaperReason(record);
        service.syncStandaloneSandpaperReason(record);
        assertEquals("砂纸：磨损；导布：破损", record.getReplaceReason());
        verify(consumableEventMapper, never()).bindStandaloneReplacement(anyLong(), anyLong(), anyString(), anyLong());
        verify(ledgerMapper, times(1)).update(isNull(), any());
    }
    @Test void replayDoesNotDuplicateReasonContainingSemicolons() {
        var record = record("FIRST");
        var event = replacement(); event.setReplaceReason("磨损；表面破损");
        when(consumableEventMapper.selectListByBiz(LINK, 30L)).thenReturn(List.of(event));
        when(ledgerMapper.update(isNull(), any())).thenReturn(1);
        service.syncStandaloneSandpaperReason(record);
        service.syncStandaloneSandpaperReason(record);
        assertEquals("砂纸：磨损；表面破损", record.getReplaceReason());
        verify(ledgerMapper, times(1)).update(isNull(), any());
    }

    @Test void failedConditionalClaimAbortsReportTransaction() {
        var record = record("SECOND"); prepare(record);
        when(consumableEventMapper.selectLaterUsageEvent(5L, TIME, 10L)).thenReturn(usage("SECOND"));
        assertThrows(ServiceException.class, () -> service.syncStandaloneSandpaperReason(record));
        verify(ledgerMapper, never()).update(isNull(), any());
    }
    @Test void missingUsageDoesNotInventAssociation() {
        var record = record("FIRST"); service.syncStandaloneSandpaperReason(record);
        assertNull(record.getReplaceReason());
        verify(consumableEventMapper, never()).selectByIdForUpdate(anyLong(), anyLong());
    }
    @Test void mismatchedTenantOrEquipmentIsIgnored() {
        var record = record("FIRST"); var other = usage("FIRST"); other.setTenantId(2L);
        when(consumableEventMapper.selectListByBiz("GRINDING_FIRST", 20L)).thenReturn(List.of(other));
        service.syncStandaloneSandpaperReason(record);
        other.setTenantId(1L); other.setEquipmentId(9L);
        service.syncStandaloneSandpaperReason(record);
        verify(consumableEventMapper, never()).selectByIdForUpdate(anyLong(), anyLong());
    }
    @Test void differentSplitSnapshotDoesNotReceiveReason() {
        var record = record("FIRST"); record.setSandpaperLife(new BigDecimal("500"));
        when(consumableEventMapper.selectListByBiz("GRINDING_FIRST", 20L)).thenReturn(List.of(usage("FIRST")));
        service.syncStandaloneSandpaperReason(record);
        assertNull(record.getReplaceReason());
        verify(consumableEventMapper, never()).selectByIdForUpdate(anyLong(), anyLong());
    }
    private HcEquipmentConsumableEventDO prepare(HcGrindingProductionRecordDO record) {
        var replacement = replacement(); var usage = usage(record.getPassType());
        when(consumableEventMapper.selectListByBiz("GRINDING_" + record.getPassType(), 20L)).thenReturn(List.of(usage));
        when(consumableEventMapper.selectLatestBeforeByStateIdAndEventType(5L, "REPLACE", TIME.plusHours(1), 12L))
                .thenReturn(replacement);
        when(consumableEventMapper.selectByIdForUpdate(10L, 1L)).thenReturn(replacement);
        return replacement;
    }
    private HcGrindingProductionRecordDO record(String pass) {
        return HcGrindingProductionRecordDO.builder().id(30L).tenantId(1L).equipmentId(7L)
                .modelCode("MODEL").materialCode("MATERIAL").batchNo("MOTHER-1P")
                .inputLength(new BigDecimal("100")).outputLength(new BigDecimal("100"))
                .segmentMark("P").firstAllocationId(40L).motherBatchNo("MOTHER-1")
                .passType(pass).sourceDetailId(20L).recordTime(TIME.plusHours(1))
                .sandpaperBatchNo("SP-1").sandpaperLife(new BigDecimal("100")).build();
    }
    private HcEquipmentConsumableEventDO replacement() {
        return HcEquipmentConsumableEventDO.builder().id(10L).tenantId(1L).equipmentId(7L).stateId(5L)
                .processCode("ROUGH_GRINDING").consumableType("SANDPAPER").eventType("REPLACE")
                .afterBatchNo("SP-1").replaceReason("磨损").eventTime(TIME).build();
    }
    private HcEquipmentConsumableEventDO usage(String pass) {
        return HcEquipmentConsumableEventDO.builder().id(12L).tenantId(1L).equipmentId(7L).stateId(5L)
                .processCode("ROUGH_GRINDING").consumableType("SANDPAPER").eventType("USE")
                .afterBatchNo("SP-1").afterUsedLength(new BigDecimal("100"))
                .bizType("GRINDING_" + pass).bizId(20L).eventTime(TIME.plusHours(1)).build();
    }
}
