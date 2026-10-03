package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisExportVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisSourcePreviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourceRow;
import java.util.List;

public interface QmsYieldAnalysisService {

    QmsYieldAnalysisRespVO getOverview(QmsYieldAnalysisReqVO reqVO);

    QmsYieldAnalysisRespVO getOverviewFromSourceRows(QmsYieldAnalysisReqVO reqVO,
                                                     List<QmsYieldAnalysisSourceRow> sourceRows);

    PageResult<QmsYieldAnalysisDetailRespVO> getDetailPageFromSourceRows(
            QmsYieldAnalysisReqVO reqVO,
            List<QmsYieldAnalysisSourceRow> sourceRows);

    List<String> getTargetModelOptions();

    PageResult<QmsYieldAnalysisDetailRespVO> getDetailPage(QmsYieldAnalysisReqVO reqVO);

    QmsYieldAnalysisSourcePreviewRespVO getSourcePreview(String sourceTable, Long sourceId, String processCode);

    List<QmsYieldAnalysisExportVO> getExportList(QmsYieldAnalysisReqVO reqVO);
}
