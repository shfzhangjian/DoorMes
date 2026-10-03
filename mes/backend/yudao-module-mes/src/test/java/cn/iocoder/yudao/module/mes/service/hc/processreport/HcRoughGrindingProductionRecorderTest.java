package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstAllocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSegmentTimingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcRoughGrindingProductionRecorderTest {
    private final HcRoughGrindingConsoleServiceImpl service = new HcRoughGrindingConsoleServiceImpl();
    private final HcGrindingSegmentTimingMapper timingMapper = mock(HcGrindingSegmentTimingMapper.class);
    private final HcGrindingProductionRecordLedgerService ledger = mock(HcGrindingProductionRecordLedgerService.class);
    private final HcPlanOrderDO plan = new HcPlanOrderDO();
    private final HcPlanOrderOperationDO operation = new HcPlanOrderOperationDO();

    HcRoughGrindingProductionRecorderTest() {
        ReflectionTestUtils.setField(service, "hcGrindingSegmentTimingMapper", timingMapper);
        ReflectionTestUtils.setField(service, "hcGrindingProductionRecordLedgerService", ledger);
        plan.setId(1L);
        plan.setProductionBatchNo("PLAN-BATCH");
        operation.setId(2L);
        operation.setProductionBatchNo("MOTHER-BATCH");
    }

    private HcGrindingSegmentTimingDO start(String pass, String segment, String name) {
        var timing = HcGrindingSegmentTimingDO.builder()
                .startTime(LocalDateTime.of(2026, 9, 20, 8, 0)).startOperatorName(name).build();
        when(timingMapper.selectBySegment(2L, "MOTHER-BATCH", pass, segment)).thenReturn(timing);
        return timing;
    }

    private String resolve(String pass, String segment) {
        return ReflectionTestUtils.invokeMethod(service, "requireProductionRecordStartOperator",
                plan, operation, pass, segment);
    }

    @Test void matchesEachPassAndSegmentIndependently() {
        for (String pass : List.of("FIRST", "SECOND")) {
            for (String segment : List.of("P", "Q", "R", "S", "NONE")) {
                String name = pass + "-" + segment;
                start(pass, segment, name);
                assertEquals(name, resolve(pass, segment));
            }
        }
    }

    @Test void emptySegmentUsesNoneAndOriginalMotherKeepsItsOwnKey() {
        start("FIRST", "NONE", "一磨不分段人员");
        start("SECOND", "NONE", "二磨不分段人员");
        start("FIRST", "MOTHER", "原母批开工人员");
        assertEquals("一磨不分段人员", resolve("FIRST", ""));
        assertEquals("二磨不分段人员", resolve("SECOND", null));
        assertEquals("原母批开工人员", resolve("FIRST", "MOTHER"));
    }

    @Test void missingOrIncompleteStartCannotFallBackToSubmitter() {
        assertThrows(ServiceException.class, () -> resolve("SECOND", "P"));
        var timing = start("SECOND", "P", "   ");
        assertThrows(ServiceException.class, () -> resolve("SECOND", "P"));
        timing.setStartOperatorName("张三");
        timing.setStartTime(null);
        assertThrows(ServiceException.class, () -> resolve("SECOND", "P"));
        verifyNoInteractions(ledger);
    }

    @Test void firstAllocationRecordsUseStarterInsteadOfSubmittingOperator() {
        start("FIRST", "P", "一磨开工人");
        var allocation = HcGrindingFirstAllocationDO.builder().id(11L).segmentMark("P").build();
        var request = new HcRoughConsoleFirstAllocationSaveReqVO();
        request.setOperatorName("报工提交人");
        ReflectionTestUtils.invokeMethod(service, "syncFirstAllocationProductionRecord",
                plan, operation, new HcGrindingFirstDetailDO(), allocation, null, List.of(), null, request);
        assertRecorders("一磨开工人", 1);
    }

    @Test void secondSandpaperSplitRecordsShareSegmentStarter() throws Exception {
        start("SECOND", "Q", "二磨开工人");
        var detail = HcGrindingSecondDetailDO.builder().id(12L).segmentMark("Q").build();
        var request = new HcRoughConsoleSecondReportSaveReqVO();
        request.setOperatorName("另一报工提交人");
        Class<?> snapshot = Class.forName(HcRoughGrindingConsoleServiceImpl.class.getName() + "$SandpaperUsageSnapshot");
        var constructor = snapshot.getDeclaredConstructor(Integer.class, String.class, BigDecimal.class, LocalDateTime.class);
        constructor.setAccessible(true);
        var segments = List.of(constructor.newInstance(1, "旧砂纸", BigDecimal.TEN, null),
                constructor.newInstance(2, "新砂纸", BigDecimal.ONE, null));
        ReflectionTestUtils.invokeMethod(service, "syncSecondProductionRecord",
                plan, operation, detail, null, segments, null, request);
        assertRecorders("二磨开工人", 2);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertRecorders(String expected, int count) {
        ArgumentCaptor<List<HcGrindingProductionRecordDO>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(ledger).syncAutoRecords(captor.capture());
        assertEquals(count, captor.getValue().size());
        captor.getValue().forEach(row -> assertEquals(expected, row.getRecorderName()));
    }
}
