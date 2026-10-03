package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveFaiRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcPickQualificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcAdhesivePickSummaryTest {
    private HcAdhesiveFaiRespVO summary(boolean qualified) {
        var service = new HcProcessReportServiceImpl();
        var pick = mock(QmsNcPickQualificationService.class);
        ReflectionTestUtils.setField(service, "pickQualificationService", pick);
        var fai = new QmsFaiOrderDO();
        fai.setId(1L); fai.setJudgment("NG"); fai.setStatus("REJECTED");
        var response = new HcAdhesiveFaiRespVO();
        response.setProductBatchNo("W26J171AQ"); response.setFaiJudgment("NG");
        response.setFaiStatus("REJECTED"); response.setAllowReportSubmit(false);
        when(pick.isQualified(fai, "W26J171AQ")).thenReturn(qualified);
        ReflectionTestUtils.invokeMethod(service, "applyAdhesivePickQualification", response, fai);
        assertEquals("NG", fai.getJudgment()); assertEquals("REJECTED", fai.getStatus());
        return response;
    }

    @Test void confirmedSegmentShowsEffectiveConclusionWithoutRejectedBadge() {
        var result = summary(true);
        assertEquals("OK", result.getFaiJudgment());
        assertEquals("COMPLETED", result.getFaiStatus());
        assertTrue(result.getAllowReportSubmit());
        assertTrue(result.getDisplayText().contains("原始检验NG"));
    }

    @Test void unconfirmedOrOtherInspectionKeepsNg() {
        var result = summary(false);
        assertEquals("NG", result.getFaiJudgment());
        assertEquals("REJECTED", result.getFaiStatus());
        assertFalse(result.getAllowReportSubmit());
    }
}
