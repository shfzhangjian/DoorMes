package cn.iocoder.yudao.module.mes.service.route;

import java.util.*;

import cn.iocoder.yudao.module.mes.controller.admin.route.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * MES工艺路线主 Service 接口
 *
 * @author 演示管理员
 */
public interface RouteService {

    public Long createRoute(RouteSaveReqVO createReqVO);

    public void updateRoute(RouteSaveReqVO updateReqVO) ;

    public void deleteRoute(Long id);

    public void deleteRouteList(Collection<Long> ids) ;

    public RouteDO getRoute(Long id) ;

    public PageResult<RouteDO> getRoutePage(RoutePageReqVO pageReqVO) ;
}
