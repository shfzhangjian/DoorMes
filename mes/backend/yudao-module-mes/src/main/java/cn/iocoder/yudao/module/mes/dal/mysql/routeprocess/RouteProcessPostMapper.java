package cn.iocoder.yudao.module.mes.dal.mysql.routeprocess;

import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessPostDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface RouteProcessPostMapper extends BaseMapperX<RouteProcessPostDO> {

    default List<RouteProcessPostDO> selectListByRouteProcessId(Long routeProcessId) {
        return selectList(RouteProcessPostDO::getRouteProcessId, routeProcessId);
    }

    default int deleteByRouteProcessId(Long routeProcessId) {
        return delete(new LambdaQueryWrapper<RouteProcessPostDO>()
                .eq(RouteProcessPostDO::getRouteProcessId, routeProcessId));
    }
}
