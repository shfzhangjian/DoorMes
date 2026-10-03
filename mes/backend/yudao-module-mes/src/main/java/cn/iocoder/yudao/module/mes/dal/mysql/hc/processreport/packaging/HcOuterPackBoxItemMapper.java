package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcOuterPackBoxItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcOuterPackBoxItemMapper extends BaseMapperX<HcOuterPackBoxItemDO> {

    default List<HcOuterPackBoxItemDO> selectListByOuterBoxId(Long outerBoxId) {
        return selectList(new LambdaQueryWrapperX<HcOuterPackBoxItemDO>()
                .eq(HcOuterPackBoxItemDO::getOuterBoxId, outerBoxId)
                .eq(HcOuterPackBoxItemDO::getDeleted, false)
                .orderByAsc(HcOuterPackBoxItemDO::getId));
    }

    default List<HcOuterPackBoxItemDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcOuterPackBoxItemDO>()
                .eq(HcOuterPackBoxItemDO::getPlanOperationId, planOperationId)
                .eq(HcOuterPackBoxItemDO::getDeleted, false)
                .orderByAsc(HcOuterPackBoxItemDO::getId));
    }

    default HcOuterPackBoxItemDO selectByInnerUnitNo(String innerUnitNo) {
        return selectOne(new LambdaQueryWrapperX<HcOuterPackBoxItemDO>()
                .eq(HcOuterPackBoxItemDO::getInnerUnitNo, innerUnitNo)
                .eq(HcOuterPackBoxItemDO::getDeleted, false)
                .orderByDesc(HcOuterPackBoxItemDO::getId)
                .last("LIMIT 1"));
    }
}
