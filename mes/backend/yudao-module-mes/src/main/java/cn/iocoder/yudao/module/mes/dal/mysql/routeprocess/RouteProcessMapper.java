// 完整路径: cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessMapper
package cn.iocoder.yudao.module.mes.dal.mysql.routeprocess;

import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface RouteProcessMapper extends BaseMapperX<RouteProcessDO> {

    default List<RouteProcessDO> selectListByRouteId(Long routeId) {
        return selectList(RouteProcessDO::getRouteId, routeId);
    }

    default int deleteByRouteId(Long routeId) {
        return delete(new LambdaQueryWrapper<RouteProcessDO>()
                .eq(RouteProcessDO::getRouteId, routeId));
    }

    default int deleteByRouteIds(Collection<Long> routeIds) {
        return delete(new LambdaQueryWrapper<RouteProcessDO>()
                .in(RouteProcessDO::getRouteId, routeIds));
    }


}
