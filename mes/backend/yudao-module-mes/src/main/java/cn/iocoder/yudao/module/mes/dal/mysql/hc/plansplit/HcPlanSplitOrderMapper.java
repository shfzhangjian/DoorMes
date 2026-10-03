package cn.iocoder.yudao.module.mes.dal.mysql.hc.plansplit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.plansplit.HcPlanSplitOrderDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPlanSplitOrderMapper extends BaseMapperX<HcPlanSplitOrderDO> {

    default List<HcPlanSplitOrderDO> selectListBySourcePlanId(Long sourcePlanId) {
        return selectList(new LambdaQueryWrapperX<HcPlanSplitOrderDO>()
                .eq(HcPlanSplitOrderDO::getSourcePlanId, sourcePlanId)
                .eq(HcPlanSplitOrderDO::getDeleted, false)
                .orderByDesc(HcPlanSplitOrderDO::getId));
    }

}
