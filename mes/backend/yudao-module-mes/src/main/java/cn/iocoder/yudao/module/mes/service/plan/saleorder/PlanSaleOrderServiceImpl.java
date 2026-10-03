package cn.iocoder.yudao.module.mes.service.plan.saleorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.saleorder.PlanSaleOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.plan.saleorder.PlanSaleOrderMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PlanSaleOrderServiceImpl implements PlanSaleOrderService {

    private static final String FORCE_DELETE_PERMISSION = "mes:sale-order:delete-force";

    @Resource
    private PlanSaleOrderMapper planSaleOrderMapper;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    public Long createSaleOrder(PlanSaleOrderSaveReqVO createReqVO) {
        PlanSaleOrderDO saleOrder = BeanUtils.toBean(createReqVO, PlanSaleOrderDO.class);
        normalizeMaterialSnapshot(saleOrder);
        saleOrder.setOrderNo(StrUtil.blankToDefault(createReqVO.getOrderNo(),
                "SO-" + LocalDate.now().toString().replace("-", "") + "-" + System.currentTimeMillis() % 100000));
        saleOrder.setPlannedQty(defaultZero(createReqVO.getPlannedQty()));
        saleOrder.setRemainQty(defaultZero(createReqVO.getQuantity()).subtract(saleOrder.getPlannedQty()).max(BigDecimal.ZERO));
        saleOrder.setStatus("DRAFT");
        saleOrder.setStatusName("草稿");
        planSaleOrderMapper.insert(saleOrder);
        return saleOrder.getId();
    }

    @Override
    public boolean updateSaleOrder(PlanSaleOrderSaveReqVO updateReqVO) {
        PlanSaleOrderDO exists = planSaleOrderMapper.selectById(updateReqVO.getId());
        if (exists == null || !"DRAFT".equals(exists.getStatus())) {
            return false;
        }
        PlanSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, PlanSaleOrderDO.class);
        normalizeMaterialSnapshot(updateObj);
        updateObj.setStatus("DRAFT");
        updateObj.setStatusName("草稿");
        updateObj.setPlannedQty(defaultZero(updateReqVO.getPlannedQty()));
        updateObj.setRemainQty(defaultZero(updateReqVO.getQuantity()).subtract(updateObj.getPlannedQty()).max(BigDecimal.ZERO));
        planSaleOrderMapper.updateById(updateObj);
        return true;
    }

    @Override
    public boolean auditSaleOrder(Long id) {
        PlanSaleOrderDO exists = planSaleOrderMapper.selectById(id);
        if (exists == null || !"DRAFT".equals(exists.getStatus())) {
            return false;
        }
        PlanSaleOrderDO updateObj = new PlanSaleOrderDO();
        updateObj.setId(id);
        updateObj.setStatus("APPROVED");
        updateObj.setStatusName("已审核");
        updateObj.setAuditorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setAuditorName(StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system"));
        updateObj.setAuditTime(LocalDateTime.now());
        planSaleOrderMapper.updateById(updateObj);
        return true;
    }

    @Override
    public boolean deleteSaleOrder(Long id) {
        PlanSaleOrderDO exists = planSaleOrderMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        boolean forceDeleteAllowed = securityFrameworkService.hasPermission(FORCE_DELETE_PERMISSION);
        if (!"DRAFT".equals(exists.getStatus()) && !forceDeleteAllowed) {
            return false;
        }
        return planSaleOrderMapper.deleteById(id) > 0;
    }

    @Override
    public PlanSaleOrderDO getSaleOrder(Long id) {
        return planSaleOrderMapper.selectById(id);
    }

    @Override
    public PageResult<PlanSaleOrderDO> getSaleOrderPage(PlanSaleOrderPageReqVO pageReqVO) {
        return planSaleOrderMapper.selectPage(pageReqVO);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void normalizeMaterialSnapshot(PlanSaleOrderDO saleOrder) {
        saleOrder.setMaterialId(saleOrder.getMaterialId() == null ? saleOrder.getProductId() : saleOrder.getMaterialId());
        saleOrder.setMaterialCode(StrUtil.blankToDefault(saleOrder.getMaterialCode(), saleOrder.getProductCode()));
        saleOrder.setMaterialName(StrUtil.blankToDefault(saleOrder.getMaterialName(), saleOrder.getProductName()));
        saleOrder.setSizeSpec(StrUtil.blankToDefault(saleOrder.getSizeSpec(), saleOrder.getProductSpec()));
        saleOrder.setUnit(StrUtil.blankToDefault(saleOrder.getUnit(), saleOrder.getUnitCode()));
        saleOrder.setProductId(saleOrder.getMaterialId());
        saleOrder.setProductCode(saleOrder.getMaterialCode());
        saleOrder.setProductName(saleOrder.getMaterialName());
        saleOrder.setProductSpec(saleOrder.getSizeSpec());
    }

}
