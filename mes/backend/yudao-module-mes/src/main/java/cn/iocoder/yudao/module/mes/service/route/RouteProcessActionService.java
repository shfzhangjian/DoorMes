// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.route.RouteProcessActionService.java
package cn.iocoder.yudao.module.mes.service.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessActionDO;
import jakarta.validation.Valid;


/**
 * 工序SOP动作定义 Service 接口
 *
 * @author 资深后端架构智能体
 */
public interface RouteProcessActionService {

    /**
     * 创建 SOP 动作定义
     * 🚨 架构师注：入参 VO 中包含 Map 类型的 actionConfig (JSON) 和 Boolean 类型的 mandatory (无 is_ 前缀)
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createRouteProcessAction(@Valid RouteProcessActionSaveReqVO createReqVO);

    /**
     * 更新 SOP 动作定义
     *
     * @param updateReqVO 更新信息
     */
    void updateRouteProcessAction(@Valid RouteProcessActionSaveReqVO updateReqVO);

    /**
     * 删除 SOP 动作定义
     *
     * @param id 编号
     */
    void deleteRouteProcessAction(Long id);

    /**
     * 获得 SOP 动作定义
     *
     * @param id 编号
     * @return SOP 动作定义 DO
     */
    RouteProcessActionDO getRouteProcessAction(Long id);

    /**
     * 获得 SOP 动作定义分页
     *
     * @param pageReqVO 分页查询
     * @return SOP 动作定义分页 DO
     */
    PageResult<RouteProcessActionDO> getRouteProcessActionPage(RouteProcessActionPageReqVO pageReqVO);

}
