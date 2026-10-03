package cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcLotRuleSegmentMapper extends BaseMapperX<HcLotRuleSegmentDO> {

    default List<HcLotRuleSegmentDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcLotRuleSegmentDO>()
                .eq(HcLotRuleSegmentDO::getRuleId, parentId)
                .orderByAsc(HcLotRuleSegmentDO::getSort)
                .orderByAsc(HcLotRuleSegmentDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcLotRuleSegmentDO>().eq(HcLotRuleSegmentDO::getRuleId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcLotRuleSegmentDO>().in(HcLotRuleSegmentDO::getRuleId, parentIds));
    }
}
