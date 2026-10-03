package cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcModelRuleItemMapper extends BaseMapperX<HcModelRuleItemDO> {

    default List<HcModelRuleItemDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcModelRuleItemDO>().eq(HcModelRuleItemDO::getRuleId, parentId).orderByAsc(HcModelRuleItemDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcModelRuleItemDO>().eq(HcModelRuleItemDO::getRuleId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcModelRuleItemDO>().in(HcModelRuleItemDO::getRuleId, parentIds));
    }
}