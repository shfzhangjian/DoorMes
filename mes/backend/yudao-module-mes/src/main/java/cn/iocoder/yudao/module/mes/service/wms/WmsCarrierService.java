// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsCarrierService.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsCarrierDO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsCarrierSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsCarrierPageReqVO;

import jakarta.validation.Valid;

public interface WmsCarrierService {
    Long createCarrier(@Valid WmsCarrierSaveReqVO createReqVO);
    void updateCarrier(@Valid WmsCarrierSaveReqVO updateReqVO);
    void deleteCarrier(Long id);
    WmsCarrierDO getCarrier(Long id);
    PageResult<WmsCarrierDO> getCarrierPage(WmsCarrierPageReqVO pageReqVO);
}
