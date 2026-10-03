package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2CheckDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotCheckDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplPreProcessFeedbackTest {

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Mock
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Mock
    private HcPressSlotCheckDetailMapper hcPressSlotCheckDetailMapper;
    @Mock
    private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Mock
    private HcAdhesive2CheckDetailMapper hcAdhesive2CheckDetailMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Mock
    private QmsFqcOrderMapper qmsFqcOrderMapper;

    @Test
    void shouldExposePressSlotFeedbackOnSlittingSliceWithoutOverwritingSelfCheck() {
        HcSlittingSliceRecordDO slice = HcSlittingSliceRecordDO.builder()
                .id(11L)
                .planOperationId(101L)
                .selfCheck("OK")
                .build();
        HcPressSlotReportDO feedback = HcPressSlotReportDO.builder()
                .id(21L)
                .sourceSlittingSliceId(11L)
                .reportStatus("CONFIRMED")
                .selfCheck("NG")
                .extraJson(attributionExtra("SLITTING"))
                .build();
        when(hcSlittingSliceRecordMapper.selectListByPlanOperationId(101L, null, null)).thenReturn(List.of(slice));
        when(hcPressSlotReportMapper.selectListBySourceSliceIds(List.of(11L))).thenReturn(List.of(feedback));

        HcSlittingSliceRespVO result = service.getSlittingSliceList(101L, null).get(0);

        assertTrue(result.getDownstreamFeedbackAbnormal());
        assertEquals("压槽", result.getDownstreamFeedbackProcessName());
        assertEquals("OK", result.getSelfCheck());
        assertTrue(result.getEditBlockedReason().contains("后续压槽确认"));
    }

    @Test
    void shouldExposeAdhesive2FeedbackOnPressSlotReportWithoutOverwritingSelfCheck() {
        HcPressSlotReportDO report = HcPressSlotReportDO.builder()
                .id(22L)
                .planOperationId(102L)
                .selfCheck("OK")
                .build();
        HcAdhesive2ReportDO feedback = HcAdhesive2ReportDO.builder()
                .id(32L)
                .sourcePressSlotReportId(22L)
                .reportStatus("CONFIRMED")
                .selfCheck("NG")
                .extraJson(attributionExtra("PRESS_SLOT"))
                .build();
        when(hcPressSlotReportMapper.selectListByPlanOperationId(102L, null, null)).thenReturn(List.of(report));
        when(hcAdhesive2ReportMapper.selectListBySourcePressSlotReportIds(List.of(22L)))
                .thenReturn(List.of(feedback));
        when(hcPressSlotCheckDetailMapper.selectListByReportId(22L)).thenReturn(List.of());

        HcAdhesiveReportRespVO result = service.getPressSlotReportList(102L).get(0);

        assertTrue(result.getDownstreamFeedbackAbnormal());
        assertEquals("粘胶2", result.getDownstreamFeedbackProcessName());
        assertEquals("OK", result.getSelfCheck());
        assertTrue(result.getEditBlockedReason().contains("后续粘胶2确认"));
    }

    @Test
    void shouldExposeCutRoundFeedbackOnAdhesive2ReportWithoutOverwritingSelfCheck() {
        HcAdhesive2ReportDO report = HcAdhesive2ReportDO.builder()
                .id(33L)
                .planOperationId(103L)
                .selfCheck("OK")
                .build();
        HcCutRoundReportDO feedback = HcCutRoundReportDO.builder()
                .id(43L)
                .sourceAdhesive2ReportId(33L)
                .reportStatus("CONFIRMED")
                .selfCheck("NG")
                .extraJson(attributionExtra("ADHESIVE2"))
                .build();
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(103L, null, null)).thenReturn(List.of(report));
        when(hcCutRoundReportMapper.selectListBySourceAdhesive2ReportIds(List.of(33L))).thenReturn(List.of(feedback));
        when(hcAdhesive2CheckDetailMapper.selectListByReportId(33L)).thenReturn(List.of());

        HcAdhesiveReportRespVO result = service.getAdhesive2ReportList(103L).get(0);

        assertTrue(result.getDownstreamFeedbackAbnormal());
        assertEquals("裁切", result.getDownstreamFeedbackProcessName());
        assertEquals("OK", result.getSelfCheck());
        assertTrue(result.getEditBlockedReason().contains("后续裁切确认"));
    }

    @Test
    void shouldLockSlittingSliceAfterPressSlotInspectionSubmitted() {
        HcSlittingSliceRecordDO slice = HcSlittingSliceRecordDO.builder()
                .id(44L)
                .planOperationId(104L)
                .sliceSerialNo("W26G163AP001")
                .selfCheck("OK")
                .build();
        QmsFaiOrderDO faiOrder = QmsFaiOrderDO.builder()
                .id(54L)
                .faiNo("FAI-PRESS-001")
                .productBatchNo("W26G163AP001")
                .sourceModule("PRESS_SLOT_REPORT")
                .processCategory("PRESS_SLOT")
                .build();
        when(hcSlittingSliceRecordMapper.selectListByPlanOperationId(104L, null, null)).thenReturn(List.of(slice));
        when(qmsFaiOrderMapper.selectListByProductBatchNos(
                List.of("W26G163AP001"), "PRESS_SLOT_REPORT", "PRESS_SLOT"))
                .thenReturn(List.of(faiOrder));

        HcSlittingSliceRespVO result = service.getSlittingSliceList(104L, null).get(0);

        assertTrue(result.getEditBlockedReason().contains("已提交压槽送检"));
        assertTrue(result.getEditBlockedReason().contains("FAI-PRESS-001"));
    }

    @Test
    void shouldLockPressSlotReportAfterAdhesive2CoaSubmitted() {
        HcPressSlotReportDO report = HcPressSlotReportDO.builder()
                .id(65L)
                .planOperationId(105L)
                .productionBatchNo("W26G163AP002")
                .reportStatus("CONFIRMED")
                .build();
        QmsFaiOrderDO faiOrder = QmsFaiOrderDO.builder()
                .id(75L)
                .faiNo("FAI-ADH2-COA-001")
                .sourceReportId(65L)
                .sourceReportNo("PLAN-ADH2-COA-W26G163AP002")
                .sourceModule("ADHESIVE2_REPORT")
                .processCategory("ADHESIVE2")
                .build();
        when(hcPressSlotReportMapper.selectListByPlanOperationId(105L, null, null)).thenReturn(List.of(report));
        when(qmsFaiOrderMapper.selectListBySourceReportIds(
                List.of(65L), "ADHESIVE2_REPORT", "ADHESIVE2"))
                .thenReturn(List.of(faiOrder));
        when(hcPressSlotCheckDetailMapper.selectListByReportId(65L)).thenReturn(List.of());

        HcAdhesiveReportRespVO result = service.getPressSlotReportList(105L).get(0);

        assertTrue(result.getEditBlockedReason().contains("粘胶2COA送检"));
        assertTrue(result.getEditBlockedReason().contains("FAI-ADH2-COA-001"));
    }

    @Test
    void shouldLockAdhesive2ReportAfterCutRoundFqcSubmitted() {
        HcAdhesive2ReportDO report = HcAdhesive2ReportDO.builder()
                .id(86L)
                .planOperationId(106L)
                .productionBatchNo("W26G163AP003")
                .reportStatus("CONFIRMED")
                .build();
        HcCutRoundReportDO cutRoundReport = HcCutRoundReportDO.builder()
                .id(96L)
                .sourceAdhesive2ReportId(86L)
                .productionBatchNo("W26G163AP003-C01")
                .reportStatus("DRAFT")
                .build();
        QmsFqcOrderDO fqcOrder = QmsFqcOrderDO.builder()
                .id(106L)
                .fqcNo("FQC-CUT-001")
                .sourceModule("CUT_ROUND")
                .sourceReportId(96L)
                .build();
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(106L, null, null)).thenReturn(List.of(report));
        when(hcCutRoundReportMapper.selectListBySourceAdhesive2ReportIds(List.of(86L)))
                .thenReturn(List.of(cutRoundReport));
        when(qmsFqcOrderMapper.selectListBySourceReportIds("CUT_ROUND", List.of(96L)))
                .thenReturn(List.of(fqcOrder));
        when(hcAdhesive2CheckDetailMapper.selectListByReportId(86L)).thenReturn(List.of());

        HcAdhesiveReportRespVO result = service.getAdhesive2ReportList(106L).get(0);

        assertTrue(result.getEditBlockedReason().contains("后续裁切已送检"));
        assertTrue(result.getEditBlockedReason().contains("FQC-CUT-001"));
    }

    private String attributionExtra(String processCode) {
        return "{\"ngAttributionType\":\"PRE_PROCESS_SELF_CHECK\","
                + "\"preProcessSelfCheckAbnormal\":true,"
                + "\"ngAttributionProcessCode\":\"" + processCode + "\","
                + "\"visualInspectionResult\":\"NG\"}";
    }
}
