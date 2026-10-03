package cn.iocoder.yudao.module.mes.dal.mysql.hc.bom;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcBomItemMapper extends BaseMapperX<HcBomItemDO> {

    default List<HcBomItemDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcBomItemDO>().eq(HcBomItemDO::getBomId, parentId).orderByAsc(HcBomItemDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcBomItemDO>().eq(HcBomItemDO::getBomId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcBomItemDO>().in(HcBomItemDO::getBomId, parentIds));
    }
}