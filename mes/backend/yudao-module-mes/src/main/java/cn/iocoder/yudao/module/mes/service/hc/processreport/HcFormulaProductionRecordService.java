package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordRespVO;
import java.util.List;

public interface HcFormulaProductionRecordService {

    PageResult<HcFormulaProductionRecordRespVO> getPage(HcFormulaProductionRecordPageReqVO reqVO);

    List<HcFormulaProductionRecordRespVO> getList(HcFormulaProductionRecordPageReqVO reqVO);

}
