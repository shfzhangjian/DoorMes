package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundBoxItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFgOutboundBoxItemMapper extends BaseMapperX<HcFgOutboundBoxItemDO> {

    default List<HcFgOutboundBoxItemDO> selectListByOutboundOrderId(Long outboundOrderId) {
        return selectList(new LambdaQueryWrapperX<HcFgOutboundBoxItemDO>()
                .eq(HcFgOutboundBoxItemDO::getOutboundOrderId, outboundOrderId)
                .eq(HcFgOutboundBoxItemDO::getDeleted, false)
                .orderByAsc(HcFgOutboundBoxItemDO::getId));
    }

    default List<HcFgOutboundBoxItemDO> selectListByOutboundBoxId(Long outboundBoxId) {
        return selectList(new LambdaQueryWrapperX<HcFgOutboundBoxItemDO>()
                .eq(HcFgOutboundBoxItemDO::getOutboundBoxId, outboundBoxId)
                .eq(HcFgOutboundBoxItemDO::getDeleted, false)
                .orderByAsc(HcFgOutboundBoxItemDO::getId));
    }

    default HcFgOutboundBoxItemDO selectByFinishedStockId(Long finishedStockId) {
        return selectOne(new LambdaQueryWrapperX<HcFgOutboundBoxItemDO>()
                .eq(HcFgOutboundBoxItemDO::getFinishedStockId, finishedStockId)
                .eq(HcFgOutboundBoxItemDO::getDeleted, false)
                .orderByDesc(HcFgOutboundBoxItemDO::getId)
                .last("LIMIT 1"));
    }
}
