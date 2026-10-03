package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import java.lang.reflect.Method;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingManualPieceImportTest {

    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private HcBomMapper hcBomMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Mock
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Mock
    private HcFinishedStockMapper hcFinishedStockMapper;

    @Test
    void shouldNotQueryProductBomForBulkImportedHistoryPiece() throws Exception {
        PackagingManualPieceCreateReqVO reqVO = new PackagingManualPieceCreateReqVO();
        reqVO.setSliceBatchNo("HISTORY-001");
        reqVO.setMaterialCode("LEGACY-MATERIAL");
        reqVO.setSegmentBatchNo("LEGACY-MATERIAL");
        reqVO.setModelCode("LEGACY-MODEL");
        reqVO.setProductionDate(LocalDate.of(2026, 7, 1));
        reqVO.setExpiryDate(LocalDate.of(2027, 4, 30));
        reqVO.setInspectionResult("OK");
        reqVO.setCoaInspectionResult("OK");
        reqVO.setBackfillReason("MES上线前历史片补录");

        HcPackagingManualPieceDO piece = prepare(reqVO, false);

        assertEquals("LEGACY-MATERIAL", piece.getMaterialCode());
        assertEquals("LEGACY-MATERIAL", piece.getSegmentBatchNo());
        assertEquals("LEGACY-MATERIAL", piece.getMaterialName());
        assertEquals("LEGACY-MODEL", piece.getModelCode());
        assertNull(piece.getProductSize());
        verifyNoInteractions(hcBomMapper);
    }

    @Test
    void shouldKeepManualSegmentBatchNoInsteadOfInferringIt() throws Exception {
        PackagingManualPieceCreateReqVO reqVO = new PackagingManualPieceCreateReqVO();
        reqVO.setSliceBatchNo("HISTORY-002-77");
        reqVO.setSegmentBatchNo("MANUAL-SEGMENT-002");
        reqVO.setMaterialCode("MATERIAL-002");
        reqVO.setModelCode("MODEL-002");
        reqVO.setProductionDate(LocalDate.of(2026, 7, 1));
        reqVO.setExpiryDate(LocalDate.of(2027, 4, 30));
        reqVO.setInspectionResult("OK");
        reqVO.setCoaInspectionResult("OK");
        reqVO.setBackfillReason("MES上线前历史片补录");

        HcPackagingManualPieceDO piece = prepare(reqVO, false);

        assertEquals("MANUAL-SEGMENT-002", piece.getSegmentBatchNo());
        assertEquals("MATERIAL-002", piece.getMaterialCode());
    }

    private HcPackagingManualPieceDO prepare(PackagingManualPieceCreateReqVO reqVO,
                                              boolean requireEnabledProductBom) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod(
                "preparePackagingManualPiece", PackagingManualPieceCreateReqVO.class, boolean.class);
        method.setAccessible(true);
        return (HcPackagingManualPieceDO) method.invoke(service, reqVO, requireEnabledProductBom);
    }
}
