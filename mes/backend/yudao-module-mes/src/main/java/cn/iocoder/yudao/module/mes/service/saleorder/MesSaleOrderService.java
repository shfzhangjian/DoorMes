package cn.iocoder.yudao.module.mes.service.saleorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo.SaleOrderCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo.SaleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo.SaleOrderUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.saleorder.MesSaleOrderDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 销售订单 Service 接口
 *
 * @author 芋道源码
 */
public interface MesSaleOrderService {

    /**
     * 创建销售订单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSaleOrder(@Valid SaleOrderCreateReqVO createReqVO);

    /**
     * 更新销售订单
     *
     * @param updateReqVO 更新信息
     */
    void updateSaleOrder(@Valid SaleOrderUpdateReqVO updateReqVO);

    /**
     * 删除销售订单
     *
     * @param id 编号
     */
    void deleteSaleOrder(Long id);

    /**
     * 获得销售订单
     *
     * @param id 编号
     * @return 销售订单
     */
    MesSaleOrderDO getSaleOrder(Long id);

    /**
     * 获得销售订单列表
     *
     * @param ids 编号
     * @return 销售订单列表
     */
    List<MesSaleOrderDO> getSaleOrderList(Collection<Long> ids);

    /**
     * 获得销售订单分页
     *
     * @param pageReqVO 分页查询
     * @return 销售订单分页
     */
    PageResult<MesSaleOrderDO> getSaleOrderPage(SaleOrderPageReqVO pageReqVO);

    // 在接口中追加方法
    void deleteSaleOrderList(Collection<Long> ids);
}
