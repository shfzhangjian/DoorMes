package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingConsumptionDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HcGrindingConsumptionEntryPointTest {
    @Test void allFourEntryPointsReturnPreviousResultBeforeRepeatingBusinessWrites() {
        var consumption = mock(HcGrindingConsumptionService.class);
        var previous = new HcGrindingConsumptionDO(); previous.setResultId(987L);
        when(consumption.begin(anyString(), any(), any())).thenReturn(previous);
        var console = new HcRoughGrindingConsoleServiceImpl();
        ReflectionTestUtils.setField(console, "grindingConsumptionService", consumption);
        assertEquals(987L, console.replaceConsumable(new HcRoughConsoleConsumableReplaceReqVO()));
        assertEquals(987L, console.saveFirstAllocation(new HcRoughConsoleFirstAllocationSaveReqVO()));
        assertEquals(987L, console.saveSecondReport(new HcRoughConsoleSecondReportSaveReqVO()));
        var records = new HcGrindingProductionRecordLedgerServiceImpl();
        ReflectionTestUtils.setField(records, "grindingConsumptionService", consumption);
        assertEquals(987L, records.create(new HcGrindingProductionRecordSaveReqVO()));
        verify(consumption).begin(eq("REPLACE"), any(), any());
        verify(consumption).begin(eq("FIRST"), any(), any());
        verify(consumption).begin(eq("SECOND"), any(), any());
        verify(consumption).begin(eq("MANUAL"), any(), any());
        verifyNoMoreInteractions(consumption);
    }

    @Test void splitOldRowDoesNotDuplicateConsumptionAndOrdinarySegmentOneStillDisplaysIt() {
        var consumption = mock(HcGrindingConsumptionService.class);
        var mapper = mock(cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper.class);
        var records = new HcGrindingProductionRecordLedgerServiceImpl();
        ReflectionTestUtils.setField(records, "grindingConsumptionService", consumption);
        ReflectionTestUtils.setField(records, "ledgerMapper", mapper);
        var old = new cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO();
        old.setId(1L); old.setSourceType("REPORT_AUTO"); old.setSourceBizType("SECOND");
        old.setPassType("SECOND"); old.setSourceDetailId(20L); old.setSourceSegmentNo(1);
        var newer = new cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO();
        newer.setId(2L); newer.setSourceType("REPORT_AUTO"); newer.setSourceBizType("SECOND");
        newer.setPassType("SECOND"); newer.setSourceDetailId(20L); newer.setSourceSegmentNo(2);
        when(mapper.selectListBySource("SECOND", "SECOND", 20L)).thenReturn(java.util.List.of(old, newer));
        ReflectionTestUtils.invokeMethod(records, "fillConsumption", old);
        verifyNoInteractions(consumption);
        var snapshot = new HcGrindingConsumptionVO(); snapshot.setSandpaperQty(new java.math.BigDecimal("2"));
        when(consumption.get("SECOND", 20L)).thenReturn(snapshot);
        ReflectionTestUtils.invokeMethod(records, "fillConsumption", newer);
        assertEquals(snapshot, newer.getConsumption());
        when(mapper.selectListBySource("SECOND", "SECOND", 20L)).thenReturn(java.util.List.of(old));
        ReflectionTestUtils.invokeMethod(records, "fillConsumption", old);
        assertEquals(snapshot, old.getConsumption());
    }
}
