package cn.iocoder.yudao.module.mes.service.mockworkorder;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.mockworkorder.MockWorkOrderDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 模拟生产工单表（用于AI大模型MCP调用测试） Service 接口
 *
 * @author 演示管理员
 */
public interface MockWorkOrderService {

    /**
     * 创建模拟生产工单表（用于AI大模型MCP调用测试）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMockWorkOrder(@Valid MockWorkOrderSaveReqVO createReqVO);

    /**
     * 更新模拟生产工单表（用于AI大模型MCP调用测试）
     *
     * @param updateReqVO 更新信息
     */
    void updateMockWorkOrder(@Valid MockWorkOrderSaveReqVO updateReqVO);

    /**
     * 删除模拟生产工单表（用于AI大模型MCP调用测试）
     *
     * @param id 编号
     */
    void deleteMockWorkOrder(Long id);

    /**
    * 批量删除模拟生产工单表（用于AI大模型MCP调用测试）
    *
    * @param ids 编号
    */
    void deleteMockWorkOrderListByIds(List<Long> ids);

    /**
     * 获得模拟生产工单表（用于AI大模型MCP调用测试）
     *
     * @param id 编号
     * @return 模拟生产工单表（用于AI大模型MCP调用测试）
     */
    MockWorkOrderDO getMockWorkOrder(Long id);

    /**
     * 获得模拟生产工单表（用于AI大模型MCP调用测试）分页
     *
     * @param pageReqVO 分页查询
     * @return 模拟生产工单表（用于AI大模型MCP调用测试）分页
     */
    PageResult<MockWorkOrderDO> getMockWorkOrderPage(MockWorkOrderPageReqVO pageReqVO);

}