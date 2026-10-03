// 完整路径: cn.iocoder.yudao.module.mes.dal.mysql.routeprocessparam.RouteProcessParamMapper
package cn.iocoder.yudao.module.mes.dal.mysql.routeprocessparam;

import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocessparam.RouteProcessParamDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface RouteProcessParamMapper extends BaseMapperX<RouteProcessParamDO> {

    default List<RouteProcessParamDO> selectListByRouteProcessId(Long routeProcessId) {
        return selectList(RouteProcessParamDO::getRouteProcessId, routeProcessId);
    }

    default int deleteByRouteProcessId(Long routeProcessId) {
        return delete(new LambdaQueryWrapper<RouteProcessParamDO>()
                .eq(RouteProcessParamDO::getRouteProcessId, routeProcessId));
    }

    // 根据主表ID删除 (级联删除优化)
    default int deleteByRouteId(Long routeId) {
        return delete(new LambdaQueryWrapper<RouteProcessParamDO>()
                .eq(RouteProcessParamDO::getRouteId, routeId));
    }
}
