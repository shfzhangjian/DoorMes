package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsSampleAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsSampleAbnormalLockMapper;
import java.util.List;
import java.util.Set;
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
class HcUpstreamSampleLockServiceTest {
    @InjectMocks private HcUpstreamSampleLockService service;
    @Mock private QmsSampleAbnormalLockMapper lockMapper;
    @Mock private cn.iocoder.yudao.module.mes.service.qms.QmsNcPickQualificationService pickQualificationService;
    @Mock private HcSlittingSliceRecordMapper sliceMapper;
    @Mock private HcAdhesiveReportMapper adhesiveMapper;

    @ParameterizedTest
    @ValueSource(strings = {"W26H154AQ001A", "W26H154AQ001B", "W26H154AQ-J1", "W26H154AQ-S2", "W26H154AQ"})
    void sourceSuffixesMustResolveToSameMotherAndSegment(String batch) {
        when(lockMapper.selectEffectiveUpstreamLocks(anySet(), anySet(), eq(1L))).thenAnswer(call -> {
            Set<String> mothers = call.getArgument(0);
            Set<String> segments = call.getArgument(1);
            assertTrue(mothers.contains("W26H154A"));
            assertEquals(Set.of("W26H154AQ"), segments);
            return List.of(lock("WET", "MOTHER_ROLL", "W26H154A", "FAI-WET"));
        });
        String reason = service.getCutRoundLockReason(HcCutRoundReportDO.builder()
                .tenantId(1L).sourceProductionBatchNo(batch).build());
        assertTrue(reason.contains("FAI-WET"));
        assertTrue(reason.contains("裁切禁止提交检验"));
    }

    @Test
    void historicalStockSourceAndMultipleLocksRemainVisibleUntilAllReleased() {
        var report = HcCutRoundReportDO.builder().tenantId(1L).sourceStockBatchNo("W26H154AQ001B").build();
        var wet = lock("WET", "MOTHER_ROLL", "W26H154A", "FAI-WET");
        var grinding = lock("ROUGH_GRINDING", "SEGMENT", "W26H154AQ", "FAI-GRIND");
        when(lockMapper.selectEffectiveUpstreamLocks(anySet(), anySet(), eq(1L)))
                .thenReturn(List.of(wet, grinding), List.of(grinding), List.of());
        assertTrue(service.getCutRoundLockReason(report).contains("FAI-WET"));
        assertTrue(service.getCutRoundLockReason(report).contains("FAI-GRIND"));
        assertNull(service.getCutRoundLockReason(report));
    }

    @Test
    void unrelatedSegmentMustNotBeIncluded() {
        when(lockMapper.selectEffectiveUpstreamLocks(anySet(), eq(Set.of("W26H154AR")), eq(1L)))
                .thenReturn(List.of());
        assertNull(service.getCutRoundLockReason(HcCutRoundReportDO.builder()
                .tenantId(1L).sourceBatchNo("W26H154AR").build()));
    }

    @Test
    void traceOriginalSourceAcrossPlansRatherThanCurrentPlanBatch() {
        when(sliceMapper.selectById(7L)).thenReturn(HcSlittingSliceRecordDO.builder()
                .sourceAdhesiveReportId(8L).sourceProductionBatchNo("W26H154AQ-J1").build());
        when(adhesiveMapper.selectById(8L)).thenReturn(HcAdhesiveReportDO.builder()
                .sourceBatchNo("W26H154A").productionBatchNo("W26H154AQ-J1").build());
        when(lockMapper.selectEffectiveUpstreamLocks(anySet(), anySet(), eq(1L))).thenAnswer(call -> {
            Set<String> segments = call.getArgument(1);
            assertTrue(segments.contains("W26H154AQ"));
            assertFalse(segments.contains("NEWPLAN"));
            return List.of(lock("ROUGH_GRINDING", "SEGMENT", "W26H154AQ", "FAI-GRIND"));
        });
        assertTrue(service.getCutRoundLockReason(HcCutRoundReportDO.builder().tenantId(1L)
                .productionBatchNo("NEWPLAN").sourceSlittingSliceId(7L).build()).contains("FAI-GRIND"));
    }

    private QmsSampleAbnormalLockDO lock(String process, String type, String batch, String no) {
        return QmsSampleAbnormalLockDO.builder().sourceProcessCode(process).objectType(type)
                .objectNo(batch).abnormalInspectionNo(no).lockNo("LOCK-1").build();
    }
    @Test
    void ncrPickReleasesOnlyItsSourceLockAtCutRound() {
        var wet = lock("WET", "MOTHER_ROLL", "W26H154A", "FAI-WET");
        var grinding = lock("ROUGH_GRINDING", "SEGMENT", "W26H154AQ", "FAI-GRIND");
        when(lockMapper.selectEffectiveUpstreamLocks(anySet(), anySet(), eq(1L))).thenReturn(List.of(wet, grinding));
        when(pickQualificationService.isLockQualified(eq(wet), anySet())).thenReturn(true);
        String reason = service.getCutRoundLockReason(HcCutRoundReportDO.builder()
                .tenantId(1L).sourceStockBatchNo("W26H154AQ001B").build());
        assertFalse(reason.contains("FAI-WET"));
        assertTrue(reason.contains("FAI-GRIND"));
    }

}
