package cn.iocoder.yudao.module.mes.service.routeprocess;

import java.util.*;

import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteRespVO;

public interface RouteProcessService {

    public void createRouteProcessList(Long routeId, List<RouteSaveReqVO.RouteProcess> list) ;

    public void updateRouteProcessList(Long routeId, List<RouteSaveReqVO.RouteProcess> list) ;

    public void deleteByRouteId(Long routeId)  ;

    public void deleteByRouteIds(Collection<Long> routeIds) ;

    public List<RouteRespVO.RouteProcess> getProcessListByRouteId(Long routeId) ;
}
