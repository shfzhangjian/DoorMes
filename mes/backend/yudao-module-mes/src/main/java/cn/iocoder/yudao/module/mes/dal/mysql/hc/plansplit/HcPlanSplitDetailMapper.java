package cn.iocoder.yudao.module.mes.dal.mysql.hc.plansplit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.plansplit.HcPlanSplitDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPlanSplitDetailMapper extends BaseMapperX<HcPlanSplitDetailDO> {

    default List<HcPlanSplitDetailDO> selectListBySplitOrderId(Long splitOrderId) {
        return selectList(new LambdaQueryWrapperX<HcPlanSplitDetailDO>()
                .eq(HcPlanSplitDetailDO::getSplitOrderId, splitOrderId)
                .eq(HcPlanSplitDetailDO::getDeleted, false)
                .orderByAsc(HcPlanSplitDetailDO::getId));
    }

}
