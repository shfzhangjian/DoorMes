package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFaiWithdrawReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotFirstInspectionSampleClaimDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotFirstInspectionSampleClaimMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplPressSlotFaiWithdrawTest {

    private static final Long PLAN_ID = 11L;
    private static final Long OPERATION_ID = 12L;
    private static final Long FAI_ID = 13L;
    private static final Long START_REPORT_ID = 14L;

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock
    private HcProcessReportMapper hcProcessReportMapper;
    @Mock
    private HcPressSlotFirstInspectionSampleClaimMapper hcPressSlotFirstInspectionSampleClaimMapper;
    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Test
    void shouldCancelUntouchedFirstInspectionAndReleaseItsSampleClaim() {
        HcPlanOrderDO plan = plan();
        HcPlanOrderOperationDO operation = operation();
        QmsFaiOrderDO pending = pendingFirstInspection();
        QmsFaiOrderDO cancelled = QmsFaiOrderDO.builder()
                .id(FAI_ID).status("CANCELED").judgment("PENDING")
                .lastReturnReason("片号录入错误").build();
        HcProcessReportDO startReport = HcProcessReportDO.builder()
                .id(START_REPORT_ID).planId(PLAN_ID).planOperationId(OPERATION_ID).build();
        HcPressSlotFirstInspectionSampleClaimDO claim = HcPressSlotFirstInspectionSampleClaimDO.builder()
                .id(15L).firstFaiId(FAI_ID).build();

        when(hcPlanOrderMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(hcPlanOrderOperationMapper.selectById(OPERATION_ID)).thenReturn(operation);
        when(qmsFaiOrderMapper.selectById(FAI_ID)).thenReturn(pending, cancelled);
        when(qmsFaiOrderMapper.cancelUntouchedPendingById(FAI_ID, "片号录入错误", "系统")).thenReturn(true);
        when(hcProcessReportMapper.selectById(START_REPORT_ID)).thenReturn(startReport);
        when(hcPressSlotFirstInspectionSampleClaimMapper.selectListByFirstFaiId(FAI_ID)).thenReturn(List.of(claim));

        service.withdrawPressSlotFai(withdrawReq());

        verify(qmsFaiOrderMapper).cancelUntouchedPendingById(FAI_ID, "片号录入错误", "系统");
        verify(hcPressSlotFirstInspectionSampleClaimMapper).deleteById(15L);
        verify(hcProcessReportMapper).updateById(org.mockito.ArgumentMatchers.argThat((HcProcessReportDO report) ->
                "CANCELED".equals(report.getFaiStatus())
                        && "片号录入错误".equals(report.getFaiRejectReason())));
    }

    @Test
    void shouldRejectWithdrawAfterQualityScan() {
        HcPlanOrderDO plan = plan();
        HcPlanOrderOperationDO operation = operation();
        QmsFaiOrderDO scanned = pendingFirstInspection();
        scanned.setLastScanTime(LocalDateTime.now());

        when(hcPlanOrderMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(hcPlanOrderOperationMapper.selectById(OPERATION_ID)).thenReturn(operation);
        when(qmsFaiOrderMapper.selectById(FAI_ID)).thenReturn(scanned);

        assertThrows(ServiceException.class, () -> service.withdrawPressSlotFai(withdrawReq()));

        verify(qmsFaiOrderMapper, never()).cancelUntouchedPendingById(FAI_ID, "片号录入错误", "系统");
    }

    private HcPressSlotFaiWithdrawReqVO withdrawReq() {
        HcPressSlotFaiWithdrawReqVO req = new HcPressSlotFaiWithdrawReqVO();
        req.setPlanId(PLAN_ID);
        req.setPlanOperationId(OPERATION_ID);
        req.setFaiId(FAI_ID);
        req.setWithdrawReason("片号录入错误");
        return req;
    }

    private HcPlanOrderDO plan() {
        return HcPlanOrderDO.builder().id(PLAN_ID).planNo("PLAN-001").build();
    }

    private HcPlanOrderOperationDO operation() {
        return HcPlanOrderOperationDO.builder()
                .id(OPERATION_ID).planId(PLAN_ID).opCode("OP-PRESS-SLOT").opSeq(1).build();
    }

    private QmsFaiOrderDO pendingFirstInspection() {
        return QmsFaiOrderDO.builder()
                .id(FAI_ID)
                .sourceModule("PRESS_SLOT_REPORT")
                .processCategory("PRESS_SLOT")
                .planOrderId(PLAN_ID)
                .sourceReportId(START_REPORT_ID)
                .sourceReportNo("PLAN-001-01")
                .status("PENDING")
                .judgment("PENDING")
                .build();
    }

}
