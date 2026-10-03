package cn.iocoder.yudao.module.mes.service.hc.plansplit;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.service.hc.nginventory.HcNgInventoryService;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcPlanSplitServiceImplPieceEligibilityTest {

    @InjectMocks
    private HcPlanSplitServiceImpl service;

    @Mock
    private HcNgInventoryService hcNgInventoryService;
    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Test
    void shouldRejectSlittingNgAndManagedPiecesForPressSlotSplit() throws Exception {
        assertTrue(isEligibleSlittingPiece(HcSlittingSliceRecordDO.builder()
                .id(10L)
                .selfCheck("OK")
                .build()));
        assertFalse(isEligibleSlittingPiece(HcSlittingSliceRecordDO.builder()
                .id(11L)
                .selfCheck("NG")
                .build()));

        HcSlittingSliceRecordDO managedPiece = HcSlittingSliceRecordDO.builder()
                .id(12L)
                .selfCheck("OK")
                .build();
        when(hcNgInventoryService.isNgPieceManaged("SLITTING", 12L)).thenReturn(true);

        assertFalse(isEligibleSlittingPiece(managedPiece));
    }

    @Test
    void shouldRejectPressSlotNgAndManagedPiecesForAdhesive2Split() throws Exception {
        assertTrue(isEligiblePressSlotPiece(HcPressSlotReportDO.builder()
                .id(20L)
                .selfCheck("OK")
                .build()));
        assertFalse(isEligiblePressSlotPiece(HcPressSlotReportDO.builder()
                .id(21L)
                .selfCheck("OK")
                .defectCode("气泡")
                .build()));

        HcPressSlotReportDO managedPiece = HcPressSlotReportDO.builder()
                .id(22L)
                .selfCheck("OK")
                .build();
        when(hcNgInventoryService.isNgPieceManaged("PRESS_SLOT", 22L)).thenReturn(true);

        assertFalse(isEligiblePressSlotPiece(managedPiece));
    }

    @Test
    void shouldIdentifyPressSlotInspectionPiecesByPieceCode() throws Exception {
        when(qmsFaiOrderMapper.selectListByProductBatchNos(anyList(), eq("PRESS_SLOT_REPORT"), eq("PRESS_SLOT")))
                .thenReturn(List.of(QmsFaiOrderDO.builder()
                        .productBatchNo("w26g029ap002a")
                        .build()));

        Set<String> inspectionCodes = findPressSlotInspectionPieceCodes(List.of("W26G029AP001A", "W26G029AP002A"));

        assertTrue(inspectionCodes.contains("W26G029AP002A"));
    }

    private boolean isEligibleSlittingPiece(HcSlittingSliceRecordDO slice) throws Exception {
        return (boolean) invoke("isEligibleSlittingPieceForPressSlotSplit", HcSlittingSliceRecordDO.class, slice);
    }

    private boolean isEligiblePressSlotPiece(HcPressSlotReportDO report) throws Exception {
        return (boolean) invoke("isEligiblePressSlotPieceForAdhesive2Split", HcPressSlotReportDO.class, report);
    }

    @SuppressWarnings("unchecked")
    private Set<String> findPressSlotInspectionPieceCodes(List<String> pieceCodes) throws Exception {
        return (Set<String>) invoke("findPressSlotInspectionPieceCodes", Collection.class, pieceCodes);
    }

    private Object invoke(String methodName, Class<?> parameterType, Object value) throws Exception {
        Method method = HcPlanSplitServiceImpl.class.getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(service, value);
    }
}
