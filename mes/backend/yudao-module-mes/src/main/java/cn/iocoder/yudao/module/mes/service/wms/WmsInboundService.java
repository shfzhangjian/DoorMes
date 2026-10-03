// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsInboundServiceImpl.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsInboundDO;

public interface WmsInboundService {
    Long createInbound(WmsInboundSaveReqVO createReqVO);
    void updateInbound(WmsInboundSaveReqVO updateReqVO);
    PageResult<WmsInboundDO> getInboundPage(WmsInboundPageReqVO pageReqVO);
}
