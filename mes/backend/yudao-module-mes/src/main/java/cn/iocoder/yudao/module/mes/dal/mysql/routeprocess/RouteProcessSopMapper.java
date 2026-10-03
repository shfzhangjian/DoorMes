package cn.iocoder.yudao.module.mes.dal.mysql.routeprocess;

import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessSopDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface RouteProcessSopMapper extends BaseMapperX<RouteProcessSopDO> {

    default List<RouteProcessSopDO> selectListByRouteProcessId(Long routeProcessId) {
        return selectList(RouteProcessSopDO::getRouteProcessId, routeProcessId);
    }

    default int deleteByRouteProcessId(Long routeProcessId) {
        return delete(new LambdaQueryWrapper<RouteProcessSopDO>()
                .eq(RouteProcessSopDO::getRouteProcessId, routeProcessId));
    }
}
