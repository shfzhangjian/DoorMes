package cn.iocoder.yudao.module.mes.dal.mysql.hc.formtemplate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcFormTemplateVersionMapper extends BaseMapperX<HcFormTemplateVersionDO> {

    default List<HcFormTemplateVersionDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcFormTemplateVersionDO>().eq(HcFormTemplateVersionDO::getTemplateId, parentId).orderByAsc(HcFormTemplateVersionDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcFormTemplateVersionDO>().eq(HcFormTemplateVersionDO::getTemplateId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcFormTemplateVersionDO>().in(HcFormTemplateVersionDO::getTemplateId, parentIds));
    }
}