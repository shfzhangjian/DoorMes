// 完整路径: cn.iocoder.yudao.module.mes.service.routeprocessparam.RouteProcessParamServiceImpl
package cn.iocoder.yudao.module.mes.service.routeprocessparam;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocessparam.RouteProcessParamDO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocessparam.RouteProcessParamMapper;

@Service
@Validated
public class RouteProcessParamServiceImpl implements RouteProcessParamService {

    @Resource
    private RouteProcessParamMapper routeProcessParamMapper;

    @Override
    public void createParamList(Long routeId, Long routeProcessId, List<RouteSaveReqVO.RouteProcessParam> list) {
        if (CollUtil.isEmpty(list)) return;

        List<RouteProcessParamDO> dos = BeanUtils.toBean(list, RouteProcessParamDO.class);
        dos.forEach(o -> {
            o.setRouteId(routeId);
            o.setRouteProcessId(routeProcessId);
        });
        routeProcessParamMapper.insertBatch(dos);
    }

    @Override
    public void updateParamList(Long routeId, Long routeProcessId, List<RouteSaveReqVO.RouteProcessParam> list) {
        // 全量替换策略：先删后加
        deleteByRouteProcessId(routeProcessId);
        createParamList(routeId, routeProcessId, list);
    }

    @Override
    public void deleteByRouteProcessId(Long routeProcessId) {
        routeProcessParamMapper.deleteByRouteProcessId(routeProcessId);
    }

    @Override
    public void deleteByRouteId(Long routeId) {
        routeProcessParamMapper.deleteByRouteId(routeId);
    }

    @Override
    public List<RouteRespVO.RouteProcessParam> getParamVOListByRouteProcessId(Long routeProcessId) {
        List<RouteProcessParamDO> list = routeProcessParamMapper.selectListByRouteProcessId(routeProcessId);
        return BeanUtils.toBean(list, RouteRespVO.RouteProcessParam.class);
    }
}
