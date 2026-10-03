package cn.iocoder.yudao.module.mes.service.saleorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.saleorder.MesSaleOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.saleorder.MesSaleOrderMapper;
import cn.iocoder.yudao.module.mes.service.material.MesMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MES_MATERIAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MES_SALE_ORDER_NOT_EXISTS;

@Service
@Validated
public class MesSaleOrderServiceImpl implements MesSaleOrderService { // 类名已修改

    @Resource
    private MesSaleOrderMapper mesSaleOrderMapper; // 变量名建议也加上 mes 前缀

    @Resource
    private MesMaterialService mesMaterialService;

    @Override
    public Long createSaleOrder(SaleOrderCreateReqVO createReqVO) {
        // 1. 校验产品是否存在，并自动回填冗余字段
        MesMaterialDO material = mesMaterialService.getMesMaterial(createReqVO.getProductId());
        if (material == null) {
            throw exception(MES_MATERIAL_NOT_EXISTS);
        }

        MesSaleOrderDO saleOrder = BeanUtils.toBean(createReqVO, MesSaleOrderDO.class);
        saleOrder.setProductCode(material.getCode());
        saleOrder.setProductName(material.getName());

        if (saleOrder.getStatus() == null) {
            saleOrder.setStatus("PENDING");
        }
        mesSaleOrderMapper.insert(saleOrder);
        return saleOrder.getId();
    }

    // 新增：模拟 ERP 同步接口
    public void syncErpOrders() {
        // 模拟从 ERP 获取数据列表
        List<MesSaleOrderDO> erpOrders = new ArrayList<>();
        // 实际逻辑：HttpClient 调用 ERP 接口 -> 解析 JSON -> 循环插入/更新
        // 这里仅做 Demo 示意
        for (MesSaleOrderDO remoteOrder : erpOrders) {
            MesSaleOrderDO exist = mesSaleOrderMapper.selectOne("order_no", remoteOrder.getOrderNo());
            if (exist == null) {
                mesSaleOrderMapper.insert(remoteOrder);
            } else {
                mesSaleOrderMapper.updateById(remoteOrder); // 更新状态或数量
            }
        }
    }

    @Override
    public void updateSaleOrder(SaleOrderUpdateReqVO updateReqVO) {
        validateSaleOrderExists(updateReqVO.getId());
        MesSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, MesSaleOrderDO.class);
        mesSaleOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteSaleOrder(Long id) {
        validateSaleOrderExists(id);
        mesSaleOrderMapper.deleteById(id);
    }

    @Override
    public void deleteSaleOrderList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        mesSaleOrderMapper.deleteBatchIds(ids);
    }

    @Override
    public MesSaleOrderDO getSaleOrder(Long id) {
        return mesSaleOrderMapper.selectById(id);
    }

    @Override
    public List<MesSaleOrderDO> getSaleOrderList(Collection<Long> ids) {
        return mesSaleOrderMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<MesSaleOrderDO> getSaleOrderPage(SaleOrderPageReqVO pageReqVO) {
        return mesSaleOrderMapper.selectPage(pageReqVO);
    }

    private void validateSaleOrderExists(Long id) {
        if (mesSaleOrderMapper.selectById(id) == null) {
            throw exception(MES_SALE_ORDER_NOT_EXISTS);
        }
    }
}
