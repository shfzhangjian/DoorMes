// 完整路径: cn.iocoder.yudao.module.mes.service.route.RouteServiceImpl
package cn.iocoder.yudao.module.mes.service.route;

import cn.iocoder.yudao.module.mes.dal.mysql.route.RouteMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.mes.service.routeprocess.RouteProcessService;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@Service
@Validated
public class RouteServiceImpl implements RouteService {

    @Resource
    private RouteMapper routeMapper;
    @Resource
    private RouteProcessService routeProcessService;
    @Resource
    private RouteProcessMapper routeProcessMapper; // 用于批量删除

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoute(RouteSaveReqVO createReqVO) {
        RouteDO route = BeanUtils.toBean(createReqVO, RouteDO.class);
        routeMapper.insert(route);

        // [关键] 传递 VO List 给子表 Service，保留孙表数据
        routeProcessService.createRouteProcessList(route.getId(), createReqVO.getProcesses());
        return route.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(RouteSaveReqVO updateReqVO) {
        validateRouteExists(updateReqVO.getId());
        RouteDO updateObj = BeanUtils.toBean(updateReqVO, RouteDO.class);
        routeMapper.updateById(updateObj);

        // [关键] 传递 VO List
        routeProcessService.updateRouteProcessList(updateReqVO.getId(), updateReqVO.getProcesses());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long id) {
        validateRouteExists(id);
        routeMapper.deleteById(id);
        // 级联删除子表和孙表
        routeProcessService.deleteByRouteId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRouteList(Collection<Long> ids) {
        routeMapper.deleteBatchIds(ids);
        // 使用 ProcessService 的批量删除方法
        routeProcessService.deleteByRouteIds(ids);
    }

    private void validateRouteExists(Long id) {
        if (routeMapper.selectById(id) == null) {
            throw exception(ROUTE_NOT_EXISTS);
        }
    }

    @Override
    public RouteDO getRoute(Long id) {
        return routeMapper.selectById(id);
    }

    @Override
    public PageResult<RouteDO> getRoutePage(RoutePageReqVO pageReqVO) {
        return routeMapper.selectPage(pageReqVO);
    }
}
