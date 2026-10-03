package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundBoxRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingPendingNgReasonTest {

    private static final String SEGMENT_BATCH_NO = "W26H159AP";

    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Mock
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Mock
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Test
    void shouldReturnCoaReasonWithoutMisreportingPackagingQualityAsFqcNg() {
        HcCutRoundReportDO report = HcCutRoundReportDO.builder()
                .id(201L)
                .parentProductionBatchNo(SEGMENT_BATCH_NO)
                .productionBatchNo("W26H159AP001A")
                .inspectionResult("OK")
                .build();
        QmsFaiOrderDO coaNg = QmsFaiOrderDO.builder()
                .id(301L)
                .faiNo("FAI-20260902-001")
                .productBatchNo(SEGMENT_BATCH_NO)
                .status("REJECTED")
                .judgment("NG")
                .lastReturnReason("硬度检测不合格")
                .build();

        mockPendingPackage(report);
        when(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos("ADHESIVE2_REPORT", Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of(coaNg));
        when(qmsFaiOrderMapper.selectPackagingCoaListBySegmentBatchNos(Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of());

        PageResult<InboundBoxRespVO> page = service.getInboundPackagePage(null, "PENDING", "NG", 1, 20);

        assertEquals(1L, page.getTotal());
        assertEquals("OK", page.getList().get(0).getItems().get(0).getInspectionResult());
        assertEquals("NG", page.getList().get(0).getItems().get(0).getCoaInspectionResult());
        assertEquals("COA：硬度检测不合格", page.getList().get(0).getItems().get(0).getNgReason());
    }

    @Test
    void shouldReturnFqcInspectionRemarkAsPendingPackageNgReason() {
        HcCutRoundReportDO report = HcCutRoundReportDO.builder()
                .id(201L)
                .parentProductionBatchNo(SEGMENT_BATCH_NO)
                .productionBatchNo("W26H159AP001A")
                .inspectionResult("NG")
                .inspectionRemark("表面存在明显划伤")
                .build();

        mockPendingPackage(report);
        when(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos("ADHESIVE2_REPORT", Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of());
        when(qmsFaiOrderMapper.selectPackagingCoaListBySegmentBatchNos(Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of());

        PageResult<InboundBoxRespVO> page = service.getInboundPackagePage(null, "PENDING", "NG", 1, 20);

        assertEquals("NG", page.getList().get(0).getItems().get(0).getInspectionResult());
        assertEquals("FQC：表面存在明显划伤", page.getList().get(0).getItems().get(0).getNgReason());
    }

    private void mockPendingPackage(HcCutRoundReportDO report) {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(101L)
                .innerUnitNo("IP-20260902-001")
                .batchNo(SEGMENT_BATCH_NO)
                .unitStatus("PACKED")
                .build();
        HcInnerPackUnitItemDO item = HcInnerPackUnitItemDO.builder()
                .id(102L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sourceType("CUT_ROUND_REPORT")
                .sourceCutRoundReportId(report.getId())
                .sliceBatchNo(report.getProductionBatchNo())
                .productionBatchNo(report.getProductionBatchNo())
                .qualityStatus("NG")
                .build();

        when(hcInnerPackUnitMapper.selectFgInboundPackagePageCount(null, "PENDING", "NG"))
                .thenReturn(1L);
        when(hcInnerPackUnitMapper.selectFgInboundPackagePage(null, "PENDING", "NG", 0, 20))
                .thenReturn(List.of(box));
        when(hcFinishedStockMapper.selectListByInnerUnitNo(box.getInnerUnitNo())).thenReturn(List.of());
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId())).thenReturn(List.of(item));
        when(hcCutRoundReportMapper.selectById(report.getId())).thenReturn(report);
        when(hcCutRoundReportMapper.selectListByIds(List.of(report.getId()))).thenReturn(List.of(report));
    }
}
