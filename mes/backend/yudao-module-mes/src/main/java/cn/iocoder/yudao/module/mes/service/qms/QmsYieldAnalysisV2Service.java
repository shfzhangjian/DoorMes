package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncStatusRespVO;
import java.util.List;

public interface QmsYieldAnalysisV2Service {

    PageResult<QmsMotherRollGoodStatisticsRespVO> getPage(QmsYieldAnalysisV2PageReqVO reqVO);

    List<QmsMotherRollGoodStatisticsRespVO> getList(QmsYieldAnalysisV2PageReqVO reqVO);

    QmsYieldAnalysisRespVO getOverview(QmsYieldAnalysisV2PageReqVO reqVO);

    PageResult<QmsYieldAnalysisDetailRespVO> getDetailPage(QmsYieldAnalysisV2PageReqVO reqVO);

    List<String> getTargetModelOptions();

    QmsYieldAnalysisV2SyncRespVO syncUnsettledHistory();

    QmsYieldAnalysisV2SyncStatusRespVO getSyncStatus();

}
