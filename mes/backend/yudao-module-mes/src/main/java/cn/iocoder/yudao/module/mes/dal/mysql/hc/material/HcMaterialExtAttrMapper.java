package cn.iocoder.yudao.module.mes.dal.mysql.hc.material;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialExtAttrDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcMaterialExtAttrMapper extends BaseMapperX<HcMaterialExtAttrDO> {

    default List<HcMaterialExtAttrDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcMaterialExtAttrDO>().eq(HcMaterialExtAttrDO::getMaterialId, parentId).orderByAsc(HcMaterialExtAttrDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcMaterialExtAttrDO>().eq(HcMaterialExtAttrDO::getMaterialId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcMaterialExtAttrDO>().in(HcMaterialExtAttrDO::getMaterialId, parentIds));
    }
}