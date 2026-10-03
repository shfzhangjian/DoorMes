package cn.iocoder.yudao.module.mes.controller.admin.saleorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.saleorder.MesSaleOrderDO;
import cn.iocoder.yudao.module.mes.service.saleorder.MesSaleOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 销售订单")
@RestController
@RequestMapping("/mes/sale-order")
@Validated
public class MesSaleOrderController {

    @Resource
    private MesSaleOrderService mesSaleOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建销售订单")
    public CommonResult<Long> createSaleOrder(@Valid @RequestBody SaleOrderCreateReqVO createReqVO) {
        return success(mesSaleOrderService.createSaleOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售订单")
    public CommonResult<Boolean> updateSaleOrder(@Valid @RequestBody SaleOrderUpdateReqVO updateReqVO) {
        mesSaleOrderService.updateSaleOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售订单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteSaleOrder(@RequestParam("id") Long id) {
        mesSaleOrderService.deleteSaleOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<SaleOrderRespVO> getSaleOrder(@RequestParam("id") Long id) {
        MesSaleOrderDO saleOrder = mesSaleOrderService.getSaleOrder(id);
        return success(BeanUtils.toBean(saleOrder, SaleOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售订单分页")
    public CommonResult<PageResult<SaleOrderRespVO>> getSaleOrderPage(@Valid SaleOrderPageReqVO pageReqVO) {
        PageResult<MesSaleOrderDO> pageResult = mesSaleOrderService.getSaleOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SaleOrderRespVO.class));
    }
}
