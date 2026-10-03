// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.workorder.MesWorkOrderController.java
package cn.iocoder.yudao.module.mes.controller.admin.workorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.service.workorder.MesWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 生产主工单")
@RestController
@RequestMapping("/mes/work-order")
@Validated
public class MesWorkOrderController {

    @Resource
    private MesWorkOrderService mesWorkOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建生产工单")
    public CommonResult<Long> createWorkOrder(@Valid @RequestBody MesWorkOrderSaveReqVO createReqVO) {
        return success(mesWorkOrderService.createWorkOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改生产工单")
    public CommonResult<Boolean> updateWorkOrder(@Valid @RequestBody MesWorkOrderSaveReqVO updateReqVO) {
        mesWorkOrderService.updateWorkOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除生产工单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteWorkOrder(@RequestParam("id") Long id) {
        mesWorkOrderService.deleteWorkOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得生产工单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<MesWorkOrderDO> getWorkOrder(@RequestParam("id") Long id) {
        return success(mesWorkOrderService.getWorkOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得生产工单分页")
    public CommonResult<PageResult<MesWorkOrderDO>> getWorkOrderPage(@Valid MesWorkOrderPageReqVO pageVO) {
        return success(mesWorkOrderService.getWorkOrderPage(pageVO));
    }
}
