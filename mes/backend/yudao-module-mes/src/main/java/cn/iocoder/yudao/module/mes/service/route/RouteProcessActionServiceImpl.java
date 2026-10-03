// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.route.RouteProcessActionServiceImpl.java
package cn.iocoder.yudao.module.mes.service.route;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX; // 🎯 使用 Yudao 增强 Wrapper
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessActionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessActionMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource; // 🎯 严格使用 jakarta

/**
 * 工序SOP动作定义 Service 实现类
 */
@Service
@Validated
public class RouteProcessActionServiceImpl implements RouteProcessActionService {

    @Resource
    private RouteProcessActionMapper routeProcessActionMapper;

    @Override
    public Long createRouteProcessAction(RouteProcessActionSaveReqVO createReqVO) {
        if (ObjectUtil.isNull(createReqVO.getRouteProcessId())) {
            throw new RuntimeException("关联工序ID不能为空！");
        }
        // 🎯 SaveReqVO 已存在，BeanUtils 类型推断明确，告别编译错误
        RouteProcessActionDO actionDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, RouteProcessActionDO.class);
        routeProcessActionMapper.insert(actionDO);
        return actionDO.getId();
    }

    @Override
    public void updateRouteProcessAction(RouteProcessActionSaveReqVO updateReqVO) {
        if (routeProcessActionMapper.selectById(updateReqVO.getId()) == null) {
            throw new RuntimeException("该SOP动作定义不存在！");
        }
        RouteProcessActionDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, RouteProcessActionDO.class);
        routeProcessActionMapper.updateById(updateObj);
    }

    @Override
    public void deleteRouteProcessAction(Long id) {
        if (routeProcessActionMapper.selectById(id) == null) {
            throw new RuntimeException("该SOP动作定义不存在！");
        }
        routeProcessActionMapper.deleteById(id);
    }

    @Override
    public RouteProcessActionDO getRouteProcessAction(Long id) {
        return routeProcessActionMapper.selectById(id);
    }

    @Override
    public PageResult<RouteProcessActionDO> getRouteProcessActionPage(RouteProcessActionPageReqVO pageReqVO) {
        // 🎯 使用 LambdaQueryWrapperX，完美支持 eqIfPresent / likeIfPresent
        return routeProcessActionMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<RouteProcessActionDO>()
                .eqIfPresent(RouteProcessActionDO::getRouteProcessId, pageReqVO.getRouteProcessId())
                .eqIfPresent(RouteProcessActionDO::getActionCode, pageReqVO.getActionCode())
                .likeIfPresent(RouteProcessActionDO::getActionName, pageReqVO.getActionName())
                .eqIfPresent(RouteProcessActionDO::getStatus, pageReqVO.getStatus())
                .orderByAsc(RouteProcessActionDO::getSort) // 按配置顺序返回
                .orderByDesc(RouteProcessActionDO::getId));
    }
}
