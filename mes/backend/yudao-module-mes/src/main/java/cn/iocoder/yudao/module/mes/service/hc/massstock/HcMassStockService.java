package cn.iocoder.yudao.module.mes.service.hc.massstock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockManualSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockShippingDetailRespVO;

import java.util.List;

public interface HcMassStockService {

    PageResult<HcMassStockRespVO> getMassStockPage(HcMassStockPageReqVO pageReqVO);

    List<HcMassStockRespVO> getMassStockList(HcMassStockPageReqVO reqVO);

    Boolean saveManual(HcMassStockManualSaveReqVO reqVO);

    List<HcMassStockGoodStockRespVO> getGoodStockList(String modelCode, String motherBatchNo,
                                                      String motherSegmentBatchNo);

    List<HcMassStockShippingDetailRespVO> getShippingDetailList(String metric, String modelCode,
                                                                String motherBatchNo,
                                                                String motherSegmentBatchNo);

}
