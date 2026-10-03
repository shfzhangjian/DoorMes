package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcCutRoundConsoleServiceImplSourceNgReleaseTest {

    @InjectMocks
    private HcCutRoundConsoleServiceImpl service;

    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;

    @Test
    void shouldKeepAdhesive2NgAsTraceWhenCutRoundVisualReviewIsOk() throws Exception {
        HcAdhesive2ReportDO source = HcAdhesive2ReportDO.builder()
                .selfCheck("NG")
                .defectCode("BLACK_DOT")
                .extraJson("{\"visualItems\":[{\"itemName\":\"黑点\",\"result\":\"NG\"}]}")
                .build();

        Object qualityRisk = buildQualityRisk(source, "OK", null, null,
                "{\"sourceNgReviewResult\":\"OK\",\"sourceNgVisualItems\":[{\"itemName\":\"黑点\"}],\"visualItems\":[{\"itemName\":\"黑点\",\"result\":\"OK\"}]}");

        assertEquals("ADHESIVE2_NG", readQualityRiskField(qualityRisk, "flag"));
        String snapshotJson = (String) readQualityRiskField(qualityRisk, "snapshotJson");
        assertTrue(snapshotJson.contains("\"sourceNgReviewResult\":\"OK\""));
        assertTrue(snapshotJson.contains("\"cutRoundNg\":false"));
    }

    @Test
    void shouldCreateCutRoundNgSnapshotForFqcPrefill() throws Exception {
        HcAdhesive2ReportDO source = HcAdhesive2ReportDO.builder().selfCheck("NG").build();

        Object qualityRisk = buildQualityRisk(source, "NG", null, null,
                "{\"sourceNgReviewResult\":\"NG\",\"visualItems\":[{\"itemName\":\"条纹\",\"result\":\"NG\",\"remark\":\"外观复核\"}]}");

        assertEquals("BOTH_NG", readQualityRiskField(qualityRisk, "flag"));
        String snapshotJson = (String) readQualityRiskField(qualityRisk, "snapshotJson");
        assertTrue(snapshotJson.contains("\"cutRoundDefectItems\""));
        assertTrue(snapshotJson.contains("条纹"));
    }

    @Test
    void shouldDistinguishDraftAndScannedCutRoundReports() throws Exception {
        when(hcCutRoundReportMapper.selectListBySourceAdhesive2ReportId(101L))
                .thenReturn(List.of(HcCutRoundReportDO.builder().reportStatus("DRAFT").build()));
        assertEquals("裁切已报工待扫码确认", resolveCutRoundDownstreamStatus(101L));

        when(hcCutRoundReportMapper.selectListBySourceAdhesive2ReportId(101L))
                .thenReturn(List.of(HcCutRoundReportDO.builder().reportStatus("CONFIRMED").build()));
        assertEquals("裁切已扫码报工", resolveCutRoundDownstreamStatus(101L));
    }

    private Object buildQualityRisk(HcAdhesive2ReportDO source, String selfCheck, String defectCode,
                                    String remark, String extraJson) throws Exception {
        Method method = HcCutRoundConsoleServiceImpl.class.getDeclaredMethod(
                "buildQualityRisk", HcAdhesive2ReportDO.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        try {
            return method.invoke(service, source, selfCheck, defectCode, remark, extraJson);
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getCause();
        }
    }

    private Object readQualityRiskField(Object qualityRisk, String fieldName) throws Exception {
        Field field = qualityRisk.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(qualityRisk);
    }

    private String resolveCutRoundDownstreamStatus(Long sourceId) throws Exception {
        Method method = HcCutRoundConsoleServiceImpl.class.getDeclaredMethod(
                "resolveCutRoundDownstreamStatus", Long.class);
        method.setAccessible(true);
        return (String) method.invoke(service, sourceId);
    }
}
