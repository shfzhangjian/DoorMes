package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionfactadjust.HcProductionFactAdjustDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductionFactAdjustDetailMapper extends BaseMapperX<HcProductionFactAdjustDetailDO> {

    default List<HcProductionFactAdjustDetailDO> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<HcProductionFactAdjustDetailDO>()
                .eq(HcProductionFactAdjustDetailDO::getAdjustOrderId, orderId)
                .orderByAsc(HcProductionFactAdjustDetailDO::getSeqNo));
    }
}
