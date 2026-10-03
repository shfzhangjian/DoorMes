// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/workorder/MesWorkOrderService.java
package cn.iocoder.yudao.module.mes.service.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import jakarta.validation.Valid;

/**
 * 生产工单 Service 接口
 * 核心职责：管理工单全生命周期（创建、拆解、开工、报工、完工）
 */
public interface MesWorkOrderService {

    /**
     * 创建生产工单 (支持自动拆解)
     * 对应场景：Scenario 1 (混合制造拆解) & Scenario 2 (线边库拉动)
     *
     * @param createReqVO 创建信息
     * @return 工单编号ID
     */
    Long createWorkOrder(@Valid MesWorkOrderSaveReqVO createReqVO);

    /**
     * 更新生产工单
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkOrder(@Valid MesWorkOrderSaveReqVO updateReqVO);

    /**
     * 删除生产工单
     *
     * @param id 编号
     */
    void deleteWorkOrder(Long id);

    /**
     * 获得生产工单
     *
     * @param id 编号
     * @return 生产工单
     */
    MesWorkOrderDO getWorkOrder(Long id);

    /**
     * 获得生产工单分页
     *
     * @param pageReqVO 分页查询
     * @return 生产工单分页
     */
    PageResult<MesWorkOrderDO> getWorkOrderPage(MesWorkOrderPageReqVO pageReqVO);

    // ================== 核心业务动作 ==================

    /**
     * [执行] 开工 (Start)
     * 对应场景：Scenario 3 (Q-Time 强制阻断)
     * 说明：操作工在现场点击“开工”时触发，需校验前置条件（物料、设备、Q-Time）
     *
     * @param id 工单编号
     */
    void startWorkOrder(Long id);
}
