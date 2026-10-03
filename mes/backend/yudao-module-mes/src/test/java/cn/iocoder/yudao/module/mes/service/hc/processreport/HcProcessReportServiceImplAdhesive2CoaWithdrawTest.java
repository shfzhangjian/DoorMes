package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2CoaWithdrawReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplAdhesive2CoaWithdrawTest {
    @InjectMocks private HcProcessReportServiceImpl service;
    @Mock private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Mock private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock private QmsFaiOrderMapper qmsFaiOrderMapper;
    private QmsFaiOrderDO fai;
    private HcAdhesive2CoaWithdrawReqVO req;

    @BeforeEach
    void setup() {
        when(hcPlanOrderMapper.selectById(118L)).thenReturn(HcPlanOrderDO.builder().id(118L).planNo("20260904-002").build());
        when(hcPlanOrderOperationMapper.selectById(950L)).thenReturn(HcPlanOrderOperationDO.builder()
                .id(950L).planId(118L).opCode("OP-ADHESIVE2").opSeq(7).build());
        fai = QmsFaiOrderDO.builder().id(1131L).planOrderId(118L).sourceModule("ADHESIVE2_REPORT")
                .processCategory("ADHESIVE2").sourceReportId(1695L)
                .sourceReportNo("20260904-002-07-ADHESIVE2-COA-POST-1695")
                .productBatchNo("W26J032AQ094A").status("PENDING").judgment("PENDING").build();
        when(qmsFaiOrderMapper.selectById(1131L)).thenReturn(fai);
        lenient().when(hcAdhesive2ReportMapper.selectByIdForUpdate(1695L)).thenReturn(HcAdhesive2ReportDO.builder()
                .id(1695L).planId(118L).planOperationId(950L).productionBatchNo("W26J032AQ094A").reportStatus("CONFIRMED").build());
        req = new HcAdhesive2CoaWithdrawReqVO();
        req.setPlanId(118L); req.setPlanOperationId(950L); req.setFaiId(1131L); req.setWithdrawReason("误送第94片");
    }

    @Test void cancelsOnlySelectedOrderAndPreservesReport() {
        when(qmsFaiOrderMapper.cancelUntouchedPendingById(1131L,"误送第94片","系统")).thenReturn(true);
        service.withdrawAdhesive2Coa(req);
        verify(qmsFaiOrderMapper).cancelUntouchedPendingById(1131L,"误送第94片","系统");
        verify(qmsFaiOrderMapper,never()).cancelUntouchedPendingById(eq(1134L),anyString(),anyString());
        verify(hcAdhesive2ReportMapper,never()).updateById(any(HcAdhesive2ReportDO.class));
    }
    private void rejects() {
        assertThrows(ServiceException.class, () -> service.withdrawAdhesive2Coa(req));
        verify(qmsFaiOrderMapper,never()).cancelUntouchedPendingById(anyLong(),anyString(),anyString());
    }
    @Test void rejectsScanned() { fai.setLastScanTime(LocalDateTime.now()); rejects(); }
    @Test void rejectsSaved() { fai.setLastSaveTime(LocalDateTime.now()); rejects(); }
    @Test void rejectsSubmitted() { fai.setOperatorTime(LocalDateTime.now()); rejects(); }
    @Test void rejectsAudited() { fai.setQaTime(LocalDateTime.now()); rejects(); }
    @Test void rejectsCompleted() { fai.setStatus("COMPLETED"); rejects(); }
    @Test void rejectsOtherPlan() { fai.setPlanOrderId(999L); rejects(); }
    @Test void rejectsLegacyCoa() { fai.setSourceReportNo("20260904-002-07-ADHESIVE2-COA-094"); rejects(); }
    @Test void rejectsOtherPiece() { fai.setProductBatchNo("W26J032AQ090A"); rejects(); }
    @Test void rejectsDownstreamReport() {
        when(hcCutRoundReportMapper.selectListBySourceAdhesive2ReportId(1695L)).thenReturn(List.of(HcCutRoundReportDO.builder().id(1L).build()));
        rejects();
    }
    @Test void rejectsConcurrentQualityProcessing() {
        when(qmsFaiOrderMapper.cancelUntouchedPendingById(1131L,"误送第94片","系统")).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.withdrawAdhesive2Coa(req));
    }
}
