package cn.iocoder.yudao.module.mes.dal.mysql.saleorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo.SaleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.saleorder.MesSaleOrderDO; // 注意引用更新
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesSaleOrderMapper extends BaseMapperX<MesSaleOrderDO> { // 类名已修改

    default PageResult<MesSaleOrderDO> selectPage(SaleOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesSaleOrderDO>()
                .likeIfPresent(MesSaleOrderDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(MesSaleOrderDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(MesSaleOrderDO::getProductName, reqVO.getProductName())
                .eqIfPresent(MesSaleOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesSaleOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesSaleOrderDO::getId));
    }
}
