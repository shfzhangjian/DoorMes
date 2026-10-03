package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcCutRoundSampleGateTest {
    @InjectMocks private HcCutRoundConsoleServiceImpl service;
    @Mock private HcUpstreamSampleLockService upstreamSampleLockService;
    @Mock private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock private HcCutRoundInspectionTaskMapper hcCutRoundInspectionTaskMapper;
    private HcCutRoundReportDO report;

    @BeforeEach
    void setup() {
        when(hcPlanOrderMapper.selectById(1L)).thenReturn(HcPlanOrderDO.builder().id(1L).build());
        when(hcPlanOrderOperationMapper.selectById(2L)).thenReturn(HcPlanOrderOperationDO.builder()
                .id(2L).planId(1L).opCode("OP-CUT").operationStatus("RUNNING").build());
        report = HcCutRoundReportDO.builder().id(3L).planId(1L).planOperationId(2L)
                .sourceBatchNo("W26H154AQ").productionBatchNo("W26H154AQ001A").reportStatus("CONFIRMED").build();
        when(upstreamSampleLockService.getCutRoundLockReason(report)).thenReturn("湿法留样NG，检验单 FAI-1，禁止裁切放行");
    }

    @Test
    void rejectInspectionBeforeCreatingTask() {
        when(hcCutRoundReportMapper.selectListByIds(List.of(3L))).thenReturn(List.of(report));
        var request = new HcCutRoundInspectionTaskSaveReqVO();
        request.setPlanId(1L); request.setPlanOperationId(2L); request.setReportIds(List.of(3L));
        assertTrue(assertThrows(ServiceException.class, () -> service.createInspectionTask(request)).getMessage().contains("FAI-1"));
        verifyNoInteractions(hcCutRoundInspectionTaskMapper);
    }

    @Test
    void rejectSegmentBeforeMarkingReportsSubmitted() {
        when(hcCutRoundReportMapper.selectListByPlanOperationId(2L)).thenReturn(List.of(report));
        var request = new HcAdhesiveSegmentCompleteReqVO();
        request.setPlanId(1L); request.setPlanOperationId(2L); request.setSourceProductionBatchNo("W26H154AQ");
        assertTrue(assertThrows(ServiceException.class, () -> service.completeSegment(request)).getMessage().contains("FAI-1"));
        verify(hcCutRoundReportMapper, never()).updateById(any(HcCutRoundReportDO.class));
    }

    @Test
    void rejectWorkOrderEvenWhenSegmentWasCompletedBeforeNgFeedback() {
        report.setReportStatus("SUBMITTED");
        when(hcCutRoundReportMapper.selectListByPlanOperationId(2L)).thenReturn(List.of(report));
        var request = new HcAdhesiveReportSubmitReqVO();
        request.setPlanId(1L); request.setPlanOperationId(2L);
        assertTrue(assertThrows(ServiceException.class, () -> service.submit(request)).getMessage().contains("FAI-1"));
        verify(hcPlanOrderOperationMapper, never()).updateById(any(HcPlanOrderOperationDO.class));
    }
}
