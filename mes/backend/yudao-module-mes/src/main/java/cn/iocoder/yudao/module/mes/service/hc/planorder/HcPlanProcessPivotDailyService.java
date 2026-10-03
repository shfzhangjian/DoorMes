package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailyCompareRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import java.util.List;

public interface HcPlanProcessPivotDailyService {

    PageResult<HcPlanProcessPivotRespVO> getPage(HcPlanProcessPivotPageReqVO pageReqVO);

    List<HcPlanProcessPivotRespVO> getList(HcPlanProcessPivotPageReqVO pageReqVO);

    HcPlanProcessPivotDailySyncStatusRespVO getSyncStatus();

    HcPlanProcessPivotDailySyncRespVO syncLatestSnapshot();

    HcPlanProcessPivotDailyCompareRespVO compareWithSource(HcPlanProcessPivotPageReqVO pageReqVO);

}
