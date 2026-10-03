package cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.saleorder.PlanSaleOrderDO;
import cn.iocoder.yudao.module.mes.service.plan.saleorder.PlanSaleOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 计划用销售订单")
@RestController
@RequestMapping("/mes/plan/sale-order")
@Validated
public class PlanSaleOrderController {

    @Resource
    private PlanSaleOrderService planSaleOrderService;

    @GetMapping("/page")
    @Operation(summary = "获得计划用销售订单分页")
    public CommonResult<PageResult<PlanSaleOrderRespVO>> getSaleOrderPage(@Valid PlanSaleOrderPageReqVO pageReqVO) {
        PageResult<PlanSaleOrderDO> pageResult = planSaleOrderService.getSaleOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PlanSaleOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得计划用销售订单")
    @Parameter(name = "id", required = true)
    public CommonResult<PlanSaleOrderRespVO> getSaleOrder(@RequestParam("id") Long id) {
        PlanSaleOrderDO saleOrder = planSaleOrderService.getSaleOrder(id);
        return success(BeanUtils.toBean(saleOrder, PlanSaleOrderRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新增计划用销售订单草稿")
    public CommonResult<Long> createSaleOrder(@Valid @RequestBody PlanSaleOrderSaveReqVO reqVO) {
        return success(planSaleOrderService.createSaleOrder(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改计划用销售订单草稿")
    public CommonResult<Boolean> updateSaleOrder(@Valid @RequestBody PlanSaleOrderSaveReqVO reqVO) {
        return success(planSaleOrderService.updateSaleOrder(reqVO));
    }

    @PutMapping("/audit")
    @Operation(summary = "审核确认计划用销售订单")
    public CommonResult<Boolean> auditSaleOrder(@RequestParam("id") Long id) {
        return success(planSaleOrderService.auditSaleOrder(id));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除计划用销售订单草稿")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> deleteSaleOrder(@RequestParam("id") Long id) {
        return success(planSaleOrderService.deleteSaleOrder(id));
    }

}
