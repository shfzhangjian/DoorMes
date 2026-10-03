package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordRespVO;
import java.util.List;

public interface HcSlittingPressProductionRecordService {

    PageResult<HcSlittingPressProductionRecordRespVO> getPage(HcSlittingPressProductionRecordPageReqVO reqVO);

    List<HcSlittingPressProductionRecordRespVO> getList(HcSlittingPressProductionRecordPageReqVO reqVO);

}
