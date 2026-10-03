package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgInboundOrderItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFgInboundOrderItemMapper extends BaseMapperX<HcFgInboundOrderItemDO> {

    default List<HcFgInboundOrderItemDO> selectListByInboundOrderId(Long inboundOrderId) {
        return selectList(new LambdaQueryWrapperX<HcFgInboundOrderItemDO>()
                .eq(HcFgInboundOrderItemDO::getInboundOrderId, inboundOrderId)
                .eq(HcFgInboundOrderItemDO::getDeleted, false)
                .orderByAsc(HcFgInboundOrderItemDO::getId));
    }

    default List<HcFgInboundOrderItemDO> selectListByOuterBoxIdForUpdate(Long outerBoxId) {
        return selectList(new LambdaQueryWrapperX<HcFgInboundOrderItemDO>()
                .eq(HcFgInboundOrderItemDO::getOuterBoxId, outerBoxId)
                .eq(HcFgInboundOrderItemDO::getDeleted, false)
                .orderByAsc(HcFgInboundOrderItemDO::getId)
                .last("FOR UPDATE"));
    }
}
