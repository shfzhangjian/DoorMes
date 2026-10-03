// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsStockServiceImpl.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wms.WmsStockMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;


@Service
@Validated
class WmsStockServiceImpl implements WmsStockService {

    @Resource
    private WmsStockMapper wmsStockMapper;

    @Override
    public PageResult<WmsStockDO> getStockPage(WmsStockPageReqVO pageReqVO) {
        return wmsStockMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<WmsStockDO>()
                .eqIfPresent(WmsStockDO::getMaterialId, pageReqVO.getMaterialId())
                .eqIfPresent(WmsStockDO::getLotNo, pageReqVO.getLotNo())
                .eqIfPresent(WmsStockDO::getWarehouseCode, pageReqVO.getWarehouseCode())
                .eqIfPresent(WmsStockDO::getQualityStatus, pageReqVO.getQualityStatus())
                // 仅显示有库存的记录
                .gt(WmsStockDO::getQuantity, java.math.BigDecimal.ZERO)
                .orderByDesc(WmsStockDO::getId));
    }
}
