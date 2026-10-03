// 完整路径: cn.iocoder.yudao.module.mes.service.routeprocessparam.RouteProcessParamService
package cn.iocoder.yudao.module.mes.service.routeprocessparam;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteRespVO;

/**
 * 工艺路线参数 Service 接口
 */
public interface RouteProcessParamService {

    /**
     * 批量创建参数列表
     */
    void createParamList(Long routeId, Long routeProcessId, List<RouteSaveReqVO.RouteProcessParam> list);

    /**
     * 更新参数列表
     */
    void updateParamList(Long routeId, Long routeProcessId, List<RouteSaveReqVO.RouteProcessParam> list);

    /**
     * 根据工序ID删除参数
     */
    void deleteByRouteProcessId(Long routeProcessId);

    /**
     * [修正] 根据路线ID删除参数 (用于级联删除)
     */
    void deleteByRouteId(Long routeId);

    /**
     * 获取工序对应的参数列表VO
     */
    List<RouteRespVO.RouteProcessParam> getParamVOListByRouteProcessId(Long routeProcessId);
}
