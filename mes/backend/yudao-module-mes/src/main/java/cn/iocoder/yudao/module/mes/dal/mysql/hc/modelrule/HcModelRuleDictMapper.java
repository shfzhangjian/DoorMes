package cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcModelRuleDictMapper extends BaseMapperX<HcModelRuleDictDO> {

    default List<HcModelRuleDictDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcModelRuleDictDO>().eq(HcModelRuleDictDO::getRuleId, parentId).orderByAsc(HcModelRuleDictDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcModelRuleDictDO>().eq(HcModelRuleDictDO::getRuleId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcModelRuleDictDO>().in(HcModelRuleDictDO::getRuleId, parentIds));
    }
}