package cn.iocoder.yudao.module.mes.dal.mysql.mockworkorder;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.mockworkorder.MockWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo.*;

/**
 * 模拟生产工单表（用于AI大模型MCP调用测试） Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface MockWorkOrderMapper extends BaseMapperX<MockWorkOrderDO> {

    default PageResult<MockWorkOrderDO> selectPage(MockWorkOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MockWorkOrderDO>()
                .eqIfPresent(MockWorkOrderDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(MockWorkOrderDO::getProductName, reqVO.getProductName())
                .eqIfPresent(MockWorkOrderDO::getQuantity, reqVO.getQuantity())
                .eqIfPresent(MockWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MockWorkOrderDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(MockWorkOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MockWorkOrderDO::getId));
    }

}