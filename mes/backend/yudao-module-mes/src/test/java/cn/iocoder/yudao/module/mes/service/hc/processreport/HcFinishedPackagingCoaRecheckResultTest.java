package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSlicePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceSegmentRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedPackagingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 覆盖 COA 复检链的最终判定优先级。
 */
@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingCoaRecheckResultTest {

    private static final String SEGMENT_BATCH_NO = "W26H151AP003A";

    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Mock
    private HcFinishedPackagingMapper hcFinishedPackagingMapper;

    @Mock
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;

    @Test
    void shouldUseCompletedRecheckOkInsteadOfHistoricalNg() throws Exception {
        QmsFaiOrderDO recheckOk = faiOrder(798L, "FAI-20260818-004", "COMPLETED", "OK", 1L, 1);
        QmsFaiOrderDO originalNg = faiOrder(690L, "FAI-20260807-011", "REJECTED", "NG", 1L, 0);
        when(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos("ADHESIVE2_REPORT", Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of(recheckOk, originalNg));

        Object meta = getCoaInspectionMeta(SEGMENT_BATCH_NO);

        assertEquals("OK", getMetaField(meta, "result"));
        assertEquals("FAI-20260818-004", getMetaField(meta, "faiNo"));
    }

    @Test
    void shouldUseLatestRecheckPendingInsteadOfHistoricalNg() throws Exception {
        QmsFaiOrderDO recheckPending = faiOrder(799L, "FAI-20260818-005", "INSPECTING", "PENDING", 1L, 2);
        QmsFaiOrderDO originalNg = faiOrder(690L, "FAI-20260807-011", "REJECTED", "NG", 1L, 0);
        when(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos("ADHESIVE2_REPORT", Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of(recheckPending, originalNg));

        Object meta = getCoaInspectionMeta(SEGMENT_BATCH_NO);

        assertEquals("PENDING", getMetaField(meta, "result"));
        assertEquals("FAI-20260818-005", getMetaField(meta, "faiNo"));
    }

    @Test
    void shouldKeepIndependentCoaNgAsEffectiveNg() throws Exception {
        QmsFaiOrderDO recheckOk = faiOrder(798L, "FAI-20260818-004", "COMPLETED", "OK", 1L, 1);
        QmsFaiOrderDO originalNg = faiOrder(690L, "FAI-20260807-011", "REJECTED", "NG", 1L, 0);
        QmsFaiOrderDO independentNg = faiOrder(800L, "FAI-20260818-006", "REJECTED", "NG", null, null);
        when(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos("ADHESIVE2_REPORT", Set.of(SEGMENT_BATCH_NO)))
                .thenReturn(List.of(recheckOk, independentNg, originalNg));

        Object meta = getCoaInspectionMeta(SEGMENT_BATCH_NO);

        assertEquals("NG", getMetaField(meta, "result"));
        assertEquals("FAI-20260818-006", getMetaField(meta, "faiNo"));
    }

    @Test
    void shouldListRecheckOkSegmentInQualifiedWaitPackagingPage() {
        InspectionSliceRespVO row = new InspectionSliceRespVO();
        row.setSourceCutRoundReportId(1L);
        row.setParentProductionBatchNo(SEGMENT_BATCH_NO);
        row.setProductionBatchNo(SEGMENT_BATCH_NO);
        row.setSliceBatchNo(SEGMENT_BATCH_NO);
        row.setInspectionStatus("COMPLETED");
        row.setInspectionResult("OK");

        when(hcFinishedPackagingMapper.countInspectionSlicePage(any(), eq("COMPLETED"))).thenReturn(1L);
        when(hcFinishedPackagingMapper.selectInspectionSlicePage(any(), eq("COMPLETED"), eq(0), eq(1000)))
                .thenReturn(List.of(row));
        when(hcPackagingManualPieceMapper.selectWaitPackagingList(any())).thenReturn(Collections.emptyList());
        when(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos(eq("ADHESIVE2_REPORT"), anySet()))
                .thenReturn(List.of(
                        faiOrder(798L, "FAI-20260818-004", "COMPLETED", "OK", 1L, 1),
                        faiOrder(690L, "FAI-20260807-011", "REJECTED", "NG", 1L, 0)));

        InspectionSlicePageReqVO reqVO = new InspectionSlicePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setPackagingQualityStatus("OK");
        PageResult<InspectionSliceSegmentRespVO> page = service.getInboundWaitSegmentPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals("W26H151AP", page.getList().get(0).getSegmentBatchNo());
        assertEquals("OK", page.getList().get(0).getPackagingQualityStatus());
        verify(hcFinishedPackagingMapper).countInspectionSlicePage(
                org.mockito.ArgumentMatchers.argThat(query -> {
                    assertNull(query.getPackagingQualityStatus());
                    return true;
                }), eq("COMPLETED"));
    }

    @SuppressWarnings("unchecked")
    private Object getCoaInspectionMeta(String segmentBatchNo) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod("buildCoaInspectionMap", Set.class);
        method.setAccessible(true);
        Map<String, Object> inspectionMap = (Map<String, Object>) method.invoke(service, Set.of(segmentBatchNo));
        return inspectionMap.get(segmentBatchNo);
    }

    private Object getMetaField(Object meta, String fieldName) throws Exception {
        Field field = meta.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(meta);
    }

    private QmsFaiOrderDO faiOrder(Long id, String faiNo, String status, String judgment,
                                   Long recheckGroupId, Integer recheckRoundNo) {
        return QmsFaiOrderDO.builder()
                .id(id)
                .faiNo(faiNo)
                .productBatchNo(SEGMENT_BATCH_NO)
                .status(status)
                .judgment(judgment)
                .recheckGroupId(recheckGroupId)
                .recheckRoundNo(recheckRoundNo)
                .build();
    }
}
