package cn.iocoder.yudao.module.mes.service.mockworkorder;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.mockworkorder.MockWorkOrderDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.mes.dal.mysql.mockworkorder.MockWorkOrderMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * 模拟生产工单表（用于AI大模型MCP调用测试） Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class MockWorkOrderServiceImpl implements MockWorkOrderService {

    @Resource
    private MockWorkOrderMapper mockWorkOrderMapper;

    @Override
    public Long createMockWorkOrder(MockWorkOrderSaveReqVO createReqVO) {
        // 插入
        MockWorkOrderDO mockWorkOrder = BeanUtils.toBean(createReqVO, MockWorkOrderDO.class);
        mockWorkOrderMapper.insert(mockWorkOrder);

        // 返回
        return mockWorkOrder.getId();
    }

    @Override
    public void updateMockWorkOrder(MockWorkOrderSaveReqVO updateReqVO) {
        // 校验存在
        validateMockWorkOrderExists(updateReqVO.getId());
        // 更新
        MockWorkOrderDO updateObj = BeanUtils.toBean(updateReqVO, MockWorkOrderDO.class);
        mockWorkOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteMockWorkOrder(Long id) {
        // 校验存在
        validateMockWorkOrderExists(id);
        // 删除
        mockWorkOrderMapper.deleteById(id);
    }

    @Override
    public void deleteMockWorkOrderListByIds(List<Long> ids) {
        // 删除
        mockWorkOrderMapper.deleteByIds(ids);
    }


    private void validateMockWorkOrderExists(Long id) {
        if (mockWorkOrderMapper.selectById(id) == null) {
            throw exception(MOCK_WORK_ORDER_NOT_EXISTS);
        }
    }

    @Override
    public MockWorkOrderDO getMockWorkOrder(Long id) {
        return mockWorkOrderMapper.selectById(id);
    }

    @Override
    public PageResult<MockWorkOrderDO> getMockWorkOrderPage(MockWorkOrderPageReqVO pageReqVO) {
        return mockWorkOrderMapper.selectPage(pageReqVO);
    }

}
