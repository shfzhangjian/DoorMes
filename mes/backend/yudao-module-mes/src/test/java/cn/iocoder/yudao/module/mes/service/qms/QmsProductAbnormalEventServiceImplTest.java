package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventDetailRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsProductAbnormalEventServiceImplTest {

    @InjectMocks
    private QmsProductAbnormalEventServiceImpl service;

    @Mock
    private QmsFqcItemMapper qmsFqcItemMapper;

    @Test
    void shouldPreferCutRoundPieceReasonOverItemLabelForSamePiece() throws Exception {
        Long fqcId = 21L;
        List<QmsCutRoundFqcRespVO.SubmissionDetail> details = List.of(
                ngDetail(394L, "W26H150AQ046A", "外观7：缺陷：BD-01 - 黑点（白垫）"),
                ngDetail(399L, "W26H150AQ032A", "外观7：缺陷：BD-01 - 黑点（白垫）"),
                ngDetail(402L, "W26H150AQ027A", "外观9：缺陷：BGZ-02 - 条纹；补充：离型纸条纹"));
        when(qmsFqcItemMapper.selectListByFqcId(fqcId)).thenReturn(List.of(
                ngItem(394L, "W26H150AQ046A", "外观7"),
                ngItem(399L, "W26H150AQ032A", "外观7"),
                ngItem(402L, "W26H150AQ027A", "外观9")));

        List<QmsProductAbnormalEventDetailRespVO.AbnormalItem> result =
                invokeBuildCutRoundAbnormalItems(fqcId, details);

        assertEquals(3, result.size());
        assertIterableEquals(List.of("W26H150AQ046A", "W26H150AQ032A", "W26H150AQ027A"),
                result.stream().map(QmsProductAbnormalEventDetailRespVO.AbnormalItem::getTargetNo).toList());
        assertIterableEquals(List.of("外观7：缺陷：BD-01 - 黑点（白垫）", "外观7：缺陷：BD-01 - 黑点（白垫）",
                        "外观9：缺陷：BGZ-02 - 条纹；补充：离型纸条纹"),
                result.stream().map(QmsProductAbnormalEventDetailRespVO.AbnormalItem::getInspectionItem).toList());
    }

    @SuppressWarnings("unchecked")
    private List<QmsProductAbnormalEventDetailRespVO.AbnormalItem> invokeBuildCutRoundAbnormalItems(
            Long fqcId, List<QmsCutRoundFqcRespVO.SubmissionDetail> details) throws Exception {
        Method method = QmsProductAbnormalEventServiceImpl.class.getDeclaredMethod("buildCutRoundAbnormalItems",
                Long.class, List.class);
        method.setAccessible(true);
        return (List<QmsProductAbnormalEventDetailRespVO.AbnormalItem>) method.invoke(service, fqcId, details);
    }

    private QmsCutRoundFqcRespVO.SubmissionDetail ngDetail(Long id, String productionBatchNo, String reason) {
        QmsCutRoundFqcRespVO.SubmissionDetail detail = new QmsCutRoundFqcRespVO.SubmissionDetail();
        detail.setId(id);
        detail.setProductionBatchNo(productionBatchNo);
        detail.setRowJudgment("NG");
        detail.setNgReason(reason);
        return detail;
    }

    private QmsFqcItemDO ngItem(Long submissionDetailId, String productionBatchNo, String inspectionItem) {
        return QmsFqcItemDO.builder()
                .submissionDetailId(submissionDetailId)
                .productionBatchNo(productionBatchNo)
                .sheetMetricName(inspectionItem)
                .itemResult("NG")
                .build();
    }
}
