package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcCutRoundConsoleServiceImplCoaSourceFilterTest {

    @InjectMocks
    private HcCutRoundConsoleServiceImpl service;

    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Test
    void shouldHideAdhesive2SourceWhenCoaInspectionIsActive() throws Exception {
        when(qmsFaiOrderMapper.selectListBySourceReportIds(anySet(), eq("ADHESIVE2_REPORT"), eq("ADHESIVE2")))
                .thenReturn(List.of(QmsFaiOrderDO.builder()
                        .sourceReportId(65L)
                        .sourceReportNo("PLAN-ADH2-COA-W26G163AP002")
                        .status("PENDING")
                        .build()));

        assertTrue(isAdhesive2SourceUnderCoaInspection(HcAdhesive2ReportDO.builder()
                .sourcePressSlotReportId(65L)
                .build()));
    }

    @Test
    void shouldKeepAdhesive2SourceVisibleWhenCoaInspectionIsCanceled() throws Exception {
        when(qmsFaiOrderMapper.selectListBySourceReportIds(anySet(), eq("ADHESIVE2_REPORT"), eq("ADHESIVE2")))
                .thenReturn(List.of(QmsFaiOrderDO.builder()
                        .sourceReportId(65L)
                        .sourceReportNo("PLAN-ADH2-COA-W26G163AP002")
                        .status("CANCELED")
                        .build()));

        assertFalse(isAdhesive2SourceUnderCoaInspection(HcAdhesive2ReportDO.builder()
                .sourcePressSlotReportId(65L)
                .build()));
    }

    @Test
    void shouldHideAdhesive2FinishedProductWhenPostConfirmCoaInspectionIsActive() throws Exception {
        when(qmsFaiOrderMapper.selectListBySourceReportIds(anySet(), eq("ADHESIVE2_REPORT"), eq("ADHESIVE2")))
                .thenReturn(List.of(QmsFaiOrderDO.builder()
                        .sourceReportId(91L)
                        .sourceReportNo("PLAN-ADH2-COA-POST-91")
                        .status("PENDING")
                        .build()));

        assertTrue(isAdhesive2SourceUnderCoaInspection(HcAdhesive2ReportDO.builder()
                .id(91L)
                .sourcePressSlotReportId(65L)
                .build()));
    }

    private boolean isAdhesive2SourceUnderCoaInspection(HcAdhesive2ReportDO source) throws Exception {
        Method method = HcCutRoundConsoleServiceImpl.class.getDeclaredMethod(
                "isAdhesive2SourceUnderCoaInspection", HcAdhesive2ReportDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, source);
    }
}
