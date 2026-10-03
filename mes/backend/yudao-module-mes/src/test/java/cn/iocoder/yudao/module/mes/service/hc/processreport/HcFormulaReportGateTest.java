package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcFormulaReportGateTest {
    @Test void directFinishCallsRuntimeGateBeforeWritingReport() {
        HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();
        HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
        HcPlanOrderOperationMapper operations = mock(HcPlanOrderOperationMapper.class);
        HcFormulaStationRuntimeService runtime = mock(HcFormulaStationRuntimeService.class);
        ReflectionTestUtils.setField(service, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationMapper", operations);
        ReflectionTestUtils.setField(service, "hcFormulaStationRuntimeService", runtime);
        HcPlanOrderDO plan = HcPlanOrderDO.builder().id(1L).planStatus("RELEASED").modelCode("W26P0200").build();
        HcPlanOrderOperationDO op = HcPlanOrderOperationDO.builder().id(11L).planId(1L).opName("配料").operationStatus("RUNNING").build();
        when(plans.selectByIdForUpdate(1L)).thenReturn(plan);
        when(operations.selectByIdForUpdate(11L)).thenReturn(op);
        doThrow(new IllegalStateException("待确认的必填表单")).when(runtime).assertFinished(plan, op, "W26P0200");
        HcFormulaReportSaveReqVO req = new HcFormulaReportSaveReqVO(); req.setPlanId(1L); req.setPlanOperationId(11L);
        assertEquals("待确认的必填表单", assertThrows(IllegalStateException.class, () -> service.submitFormulaReport(req)).getMessage());
        var order = inOrder(operations, plans, runtime);
        order.verify(plans).selectByIdForUpdate(1L);
        order.verify(operations).selectByIdForUpdate(11L);
        order.verify(runtime).assertFinished(plan, op, "W26P0200");
    }

    @Test void finishedOperationCannotSaveOrConfirmEvenWithDirectRequest() {
        HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();
        HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
        HcPlanOrderOperationMapper operations = mock(HcPlanOrderOperationMapper.class);
        HcFormulaStationRuntimeService runtime = mock(HcFormulaStationRuntimeService.class);
        ReflectionTestUtils.setField(service, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationMapper", operations);
        ReflectionTestUtils.setField(service, "hcFormulaStationRuntimeService", runtime);
        when(plans.selectByIdForUpdate(1L)).thenReturn(HcPlanOrderDO.builder().id(1L).planStatus("RELEASED").build());
        when(operations.selectByIdForUpdate(11L)).thenReturn(HcPlanOrderOperationDO.builder().id(11L).planId(1L).opName("配料").operationStatus("FINISHED").build());
        HcFormulaPassWorkSaveReqVO req = new HcFormulaPassWorkSaveReqVO(); req.setPlanId(1L); req.setPlanOperationId(11L); req.setFormCode("FORMULA_PROCESS_CHECK");
        assertThrows(RuntimeException.class, () -> service.saveFormulaPassWork(req));
        assertThrows(RuntimeException.class, () -> service.confirmFormulaPassWork(req));
        verifyNoInteractions(runtime);
    }
}
