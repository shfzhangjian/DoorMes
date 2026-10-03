package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCategoryAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCategoryAnalysisRespVO;
import java.util.List;

public interface QmsDefectCategoryAnalysisService {

    QmsDefectCategoryAnalysisRespVO getOverview(QmsDefectCategoryAnalysisReqVO reqVO);

    PageResult<QmsDefectCategoryAnalysisRespVO.DetailRow> getDetailPage(QmsDefectCategoryAnalysisReqVO reqVO);

    List<String> getDefectCategoryOptions(QmsDefectCategoryAnalysisReqVO reqVO);
}
