// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsStockServiceImpl.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsStockDO;

public interface WmsStockService {
    PageResult<WmsStockDO> getStockPage(WmsStockPageReqVO pageReqVO);
    // 预留：核心扣减与增加库存接口
    // void addStock(...);
    // void reduceStock(...);
}
