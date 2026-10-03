package cn.iocoder.yudao.module.mes.dal.mysql.hc.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcRouteOperationMapper extends BaseMapperX<HcRouteOperationDO> {

    default List<HcRouteOperationDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcRouteOperationDO>()
                .eq(HcRouteOperationDO::getRouteId, parentId)
                .last("ORDER BY CASE WHEN seq_no IS NULL OR seq_no <= 0 THEN 999999 ELSE seq_no END ASC, id ASC"));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcRouteOperationDO>().eq(HcRouteOperationDO::getRouteId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcRouteOperationDO>().in(HcRouteOperationDO::getRouteId, parentIds));
    }
}
