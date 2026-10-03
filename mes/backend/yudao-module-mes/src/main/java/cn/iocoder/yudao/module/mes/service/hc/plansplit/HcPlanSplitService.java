package cn.iocoder.yudao.module.mes.service.hc.plansplit;

import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitCreateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitGraphRespVO;

public interface HcPlanSplitService {

    HcPlanSplitGraphRespVO getPlanSplitGraph(String planNo);

    HcPlanSplitCreateRespVO createPlanSplit(HcPlanSplitCreateReqVO reqVO);

}
