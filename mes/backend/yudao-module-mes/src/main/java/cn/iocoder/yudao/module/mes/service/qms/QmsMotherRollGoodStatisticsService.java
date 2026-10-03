package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsRespVO;
import java.util.List;

public interface QmsMotherRollGoodStatisticsService {

    PageResult<QmsMotherRollGoodStatisticsRespVO> getMotherRollGoodStatisticsPage(
            QmsMotherRollGoodStatisticsPageReqVO pageReqVO);

    List<QmsMotherRollGoodStatisticsRespVO> getMotherRollGoodStatisticsList(
            QmsMotherRollGoodStatisticsPageReqVO pageReqVO);

}