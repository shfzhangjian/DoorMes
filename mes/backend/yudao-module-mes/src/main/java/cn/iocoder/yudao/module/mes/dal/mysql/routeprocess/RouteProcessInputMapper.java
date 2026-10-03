package cn.iocoder.yudao.module.mes.dal.mysql.routeprocess;

import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessInputDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface RouteProcessInputMapper extends BaseMapperX<RouteProcessInputDO> {

    default List<RouteProcessInputDO> selectListByRouteProcessId(Long routeProcessId) {
        return selectList(RouteProcessInputDO::getRouteProcessId, routeProcessId);
    }

    default int deleteByRouteProcessId(Long routeProcessId) {
        return delete(new LambdaQueryWrapper<RouteProcessInputDO>()
                .eq(RouteProcessInputDO::getRouteProcessId, routeProcessId));
    }
}
