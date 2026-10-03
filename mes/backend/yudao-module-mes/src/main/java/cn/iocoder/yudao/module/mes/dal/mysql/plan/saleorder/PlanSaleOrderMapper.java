package cn.iocoder.yudao.module.mes.dal.mysql.plan.saleorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.saleorder.PlanSaleOrderDO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlanSaleOrderMapper extends BaseMapperX<PlanSaleOrderDO> {

    default PageResult<PlanSaleOrderDO> selectPage(PlanSaleOrderPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<PlanSaleOrderDO> selectList(PlanSaleOrderPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<PlanSaleOrderDO> buildQuery(PlanSaleOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<PlanSaleOrderDO> queryWrapper = new LambdaQueryWrapperX<PlanSaleOrderDO>()
                .likeIfPresent(PlanSaleOrderDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(PlanSaleOrderDO::getErpNo, reqVO.getErpNo())
                .likeIfPresent(PlanSaleOrderDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(PlanSaleOrderDO::getMaterialCode, reqVO.getProductCode())
                .likeIfPresent(PlanSaleOrderDO::getProductName, reqVO.getProductName())
                .likeIfPresent(PlanSaleOrderDO::getSizeSpec, reqVO.getProductSpec())
                .eqIfPresent(PlanSaleOrderDO::getStatus, reqVO.getStatus());
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper.like(PlanSaleOrderDO::getOrderNo, reqVO.getKeyword())
                    .or()
                    .like(PlanSaleOrderDO::getErpNo, reqVO.getKeyword())
                    .or()
                    .like(PlanSaleOrderDO::getCustomerName, reqVO.getKeyword())
                    .or()
                    .like(PlanSaleOrderDO::getMaterialCode, reqVO.getKeyword())
                    .or()
                    .like(PlanSaleOrderDO::getMaterialName, reqVO.getKeyword())
                    .or()
                    .like(PlanSaleOrderDO::getSizeSpec, reqVO.getKeyword()));
        }
        if (Boolean.TRUE.equals(reqVO.getPlanSelectable())) {
            queryWrapper.in(PlanSaleOrderDO::getStatus, List.of("APPROVED", "PART_PLANNED"))
                    .gt(PlanSaleOrderDO::getRemainQty, BigDecimal.ZERO);
        }
        queryWrapper.orderByDesc(PlanSaleOrderDO::getDeliveryDate)
                .orderByDesc(PlanSaleOrderDO::getId);
        return queryWrapper;
    }

}
