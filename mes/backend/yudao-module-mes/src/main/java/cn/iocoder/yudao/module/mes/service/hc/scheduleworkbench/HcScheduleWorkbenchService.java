package cn.iocoder.yudao.module.mes.service.hc.scheduleworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchRespVO;
import java.util.List;

public interface HcScheduleWorkbenchService {

    HcScheduleWorkbenchRespVO getWorkbench(HcScheduleWorkbenchReqVO reqVO);

    HcScheduleWorkbenchExportRespVO getWorkbenchExportData(HcScheduleWorkbenchReqVO reqVO);

    List<HcScheduleWorkbenchRespVO.ChangeoverDetail> getChangeoverDetails(Long planId, String type);

}
