package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetFaiRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcPickQualificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcWetPickSummaryTest {
    private HcWetFaiRespVO summary(QmsNcPickQualificationService.Qualification qualification) {
        var pick = mock(QmsNcPickQualificationService.class);
        var service = new HcProcessReportServiceImpl();
        ReflectionTestUtils.setField(service, "pickQualificationService", pick);
        var fai = new QmsFaiOrderDO();
        fai.setId(1122L); fai.setFaiNo("FAI-20260912-010");
        fai.setProductBatchNo("W26J169A"); fai.setStatus("REJECTED"); fai.setJudgment("NG");
        when(pick.qualification(1122L, "W26J169A")).thenReturn(qualification);
        when(pick.isQualified(fai, "W26J169A")).thenReturn(
                qualification == QmsNcPickQualificationService.Qualification.QUALIFIED);
        HcWetFaiRespVO result = ReflectionTestUtils.invokeMethod(service, "buildWetFaiResp", fai, null, List.of(fai));
        assertEquals("NG", fai.getJudgment());
        assertEquals("REJECTED", fai.getStatus());
        return result;
    }

    @Test void qualifiedSummaryAndRecordUseEffectiveOkInsteadOfRejectedBadge() {
        var result = summary(QmsNcPickQualificationService.Qualification.QUALIFIED);
        assertEquals("OK", result.getFaiJudgment());
        assertEquals("COMPLETED", result.getFaiStatus());
        assertTrue(result.getAllowReportSubmit());
        assertTrue(result.getDisplayText().contains("处置后"));
        assertEquals("OK", result.getRecords().get(0).getFaiJudgment());
        assertEquals("COMPLETED", result.getRecords().get(0).getFaiStatus());
    }

    @Test void partialCoverageIsVisibleButCannotReleaseWholeRoll() {
        var result = summary(QmsNcPickQualificationService.Qualification.PARTIAL);
        assertEquals("NG", result.getFaiJudgment());
        assertFalse(result.getAllowReportSubmit());
        assertTrue(result.getDisplayText().contains("部分处置合格"));
        assertTrue(result.getRecords().get(0).getDisplayText().contains("部分处置合格"));
    }
    @Test void taskListUsesSameEffectiveJudgmentAsDetail() {
        var pick = mock(QmsNcPickQualificationService.class);
        var service = new HcProcessReportServiceImpl();
        ReflectionTestUtils.setField(service, "pickQualificationService", pick);
        var row = new cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportTaskRespVO();
        row.setFaiId(1122L); row.setProductionBatchNo("W26J169A");
        row.setFaiJudgment("NG"); row.setFaiStatus("REJECTED");
        when(pick.isQualified(1122L, "W26J169A")).thenReturn(true);
        ReflectionTestUtils.invokeMethod(service, "fillWetTaskPickQualification", row);
        assertEquals("OK", row.getFaiJudgment());
        assertEquals("COMPLETED", row.getFaiStatus());
    }

}
