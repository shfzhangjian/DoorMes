package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotFirstInspectionSampleClaimMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportConfirmReqVO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplPressSlotFirstInspectionFilterTest {

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Mock
    private HcPressSlotFirstInspectionSampleClaimMapper hcPressSlotFirstInspectionSampleClaimMapper;

    @Mock
    private HcPressSlotReportMapper hcPressSlotReportMapper;

    @Test
    void shouldRejectNormalReportingForPendingFirstInspectionSample() throws Exception {
        when(hcPressSlotFirstInspectionSampleClaimMapper.selectOccupiedSourceIds(113L)).thenReturn(List.of(3402L));
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> validateNormalSource(3402L, null));
        assertInstanceOf(ServiceException.class, error.getCause());
        assertTrue(error.getCause().getMessage().contains("首检样片"));
    }

    @Test
    void shouldAllowOtherUnclaimedSlicesInSameSegment() throws Exception {
        when(hcPressSlotFirstInspectionSampleClaimMapper.selectOccupiedSourceIds(113L)).thenReturn(List.of(3402L));
        validateNormalSource(3403L, null);
    }

    @Test
    void shouldPreserveExistingSampleRecordMaintenance() throws Exception {
        validateNormalSource(3402L, HcPressSlotReportDO.builder()
                .extraJson("{\"faiFirstInspectionSample\":true}").build());
        verifyNoInteractions(hcPressSlotFirstInspectionSampleClaimMapper);
    }

    @Test
    void shouldRejectDirectConfirmationOfDraftCreatedBeforeSampleClaim() {
        HcPressSlotReportDO draft = HcPressSlotReportDO.builder().id(99L).planId(113L)
                .sourceSlittingSliceId(3402L).reportStatus("DRAFT").build();
        when(hcPressSlotReportMapper.selectById(99L)).thenReturn(draft);
        when(hcPressSlotFirstInspectionSampleClaimMapper.selectOccupiedSourceIds(113L)).thenReturn(List.of(3402L));
        HcAdhesiveReportConfirmReqVO req = new HcAdhesiveReportConfirmReqVO();
        req.setId(99L);
        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmPressSlotReport(req));
        assertTrue(error.getMessage().contains("首检样片"));
    }

    private void validateNormalSource(Long sourceId, HcPressSlotReportDO report) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "validatePressSlotNormalReportSourceNotSample", Long.class, Long.class, HcPressSlotReportDO.class);
        method.setAccessible(true);
        method.invoke(service, 113L, sourceId, report);
    }

    @Test
    void shouldNotTreatAbnormalReleaseFaiAsPressSlotFirstInspectionSample() throws Exception {
        HcPressSlotReportDO source = pressSlotSource();
        QmsFaiOrderDO abnormalReleaseFai = terminalFai("20260716-003-06-ABNORMAL-RELEASE-W26G029AP018A");
        when(qmsFaiOrderMapper.selectLatestTerminalPressSlotFirstInspection(
                "PRESS_SLOT_REPORT", 89L, "W26G029AP018A")).thenReturn(abnormalReleaseFai);

        assertNull(findTerminalPressSlotFirstInspectionSample(source));
    }

    @Test
    void shouldKeepActualPressSlotFirstInspectionSampleExcluded() throws Exception {
        HcPressSlotReportDO source = pressSlotSource();
        QmsFaiOrderDO firstInspectionFai = terminalFai("20260716-003-06");
        when(qmsFaiOrderMapper.selectLatestTerminalPressSlotFirstInspection(
                "PRESS_SLOT_REPORT", 89L, "W26G029AP018A")).thenReturn(firstInspectionFai);

        assertSame(firstInspectionFai, findTerminalPressSlotFirstInspectionSample(source));
    }

    private HcPressSlotReportDO pressSlotSource() {
        return HcPressSlotReportDO.builder()
                .planId(89L)
                .productionBatchNo("W26G029AP018A")
                .build();
    }

    private QmsFaiOrderDO terminalFai(String sourceReportNo) {
        return QmsFaiOrderDO.builder()
                .sourceModule("PRESS_SLOT_REPORT")
                .processCategory("PRESS_SLOT")
                .sourceReportNo(sourceReportNo)
                .status("COMPLETED")
                .judgment("OK")
                .build();
    }

    private QmsFaiOrderDO findTerminalPressSlotFirstInspectionSample(HcPressSlotReportDO source) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "findTerminalPressSlotFirstInspectionSample", HcPressSlotReportDO.class);
        method.setAccessible(true);
        return (QmsFaiOrderDO) method.invoke(service, source);
    }
}
