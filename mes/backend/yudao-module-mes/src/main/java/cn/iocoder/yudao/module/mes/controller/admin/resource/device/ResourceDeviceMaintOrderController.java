package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCandidateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCurrentMonthAppendReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderDO;
import cn.iocoder.yudao.module.mes.service.resource.device.ResourceDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备保养工单")
@RestController
@RequestMapping("/mes/resource/device/maint-order")
@Validated
public class ResourceDeviceMaintOrderController {

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @PostMapping("/create")
    @Operation(summary = "创建设备保养工单")
    public CommonResult<Long> createMaintOrder(@Valid @RequestBody ResourceDeviceMaintOrderSaveReqVO reqVO) {
        return success(resourceDeviceService.createMaintOrder(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备保养工单")
    public CommonResult<Boolean> updateMaintOrder(
            @Validated({Default.class, ResourceDeviceMaintOrderSaveReqVO.Update.class})
            @RequestBody ResourceDeviceMaintOrderSaveReqVO reqVO) {
        resourceDeviceService.updateMaintOrder(reqVO);
        return success(true);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行设备保养工单")
    public CommonResult<Long> executeMaintOrder(@Valid @RequestBody ResourceDeviceMaintOrderExecuteReqVO reqVO) {
        return success(resourceDeviceService.executeMaintOrder(reqVO));
    }

    @GetMapping("/candidate/list")
    @Operation(summary = "获得设备本月保养追加候选")
    public CommonResult<List<ResourceDeviceMaintCandidateRespVO>> getMaintCurrentMonthCandidates(
            @Valid ResourceDeviceMaintCandidateReqVO reqVO) {
        return success(resourceDeviceService.getMaintCurrentMonthCandidates(reqVO));
    }

    @PostMapping("/append-current-month")
    @Operation(summary = "追加设备本月保养检验")
    public CommonResult<Integer> appendCurrentMonthMaintOrders(
            @Valid @RequestBody ResourceDeviceMaintCurrentMonthAppendReqVO reqVO) {
        return success(resourceDeviceService.appendCurrentMonthMaintOrders(reqVO));
    }

    @PostMapping("/confirm-list")
    @Operation(summary = "批量确认设备保养记录")
    public CommonResult<Integer> confirmMaintOrders(@Valid @RequestBody ResourceDeviceMaintConfirmReqVO reqVO) {
        return success(resourceDeviceService.confirmMaintOrders(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备保养工单")
    public CommonResult<Boolean> deleteMaintOrder(@RequestParam("id") Long id) {
        resourceDeviceService.deleteMaintOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除设备保养工单")
    public CommonResult<Boolean> deleteMaintOrderList(@RequestParam("ids") List<Long> ids) {
        ids.forEach(resourceDeviceService::deleteMaintOrder);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备保养工单")
    public CommonResult<ResourceDeviceMaintOrderRespVO> getMaintOrder(@RequestParam("id") Long id) {
        return success(buildOrderResp(resourceDeviceService.getMaintOrder(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备保养工单分页")
    public CommonResult<PageResult<ResourceDeviceMaintOrderRespVO>> getMaintOrderPage(@Valid ResourceDeviceMaintOrderPageReqVO reqVO) {
        PageResult<ResourceDeviceMaintOrderDO> pageResult = resourceDeviceService.getMaintOrderPage(reqVO);
        return success(BeanUtils.toBean(pageResult, ResourceDeviceMaintOrderRespVO.class));
    }

    @GetMapping("/item/list")
    @Operation(summary = "获得设备保养工单项目列表")
    public CommonResult<List<ResourceDeviceMaintOrderRespVO.OrderItem>> getOrderItemList(@RequestParam("taskId") Long taskId) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintOrderItemList(taskId),
                ResourceDeviceMaintOrderRespVO.OrderItem.class));
    }

    @GetMapping("/part/list")
    @Operation(summary = "获得设备保养工单备件列表")
    public CommonResult<List<ResourceDeviceMaintOrderRespVO.OrderPart>> getOrderPartList(@RequestParam("taskId") Long taskId) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintOrderPartList(taskId),
                ResourceDeviceMaintOrderRespVO.OrderPart.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备保养工单 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderExcel(@Valid ResourceDeviceMaintOrderPageReqVO reqVO,
                                 HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceDeviceMaintOrderDO> list = resourceDeviceService.getMaintOrderPage(reqVO).getList();
        ExcelUtils.write(response, "设备保养工单.xls", "数据", ResourceDeviceMaintOrderRespVO.class,
                BeanUtils.toBean(list, ResourceDeviceMaintOrderRespVO.class));
    }

    private ResourceDeviceMaintOrderRespVO buildOrderResp(ResourceDeviceMaintOrderDO order) {
        ResourceDeviceMaintOrderRespVO respVO = BeanUtils.toBean(order, ResourceDeviceMaintOrderRespVO.class);
        if (respVO == null || order == null) {
            return respVO;
        }
        respVO.setItems(BeanUtils.toBean(resourceDeviceService.getMaintOrderItemList(order.getId()),
                ResourceDeviceMaintOrderRespVO.OrderItem.class));
        respVO.setParts(BeanUtils.toBean(resourceDeviceService.getMaintOrderPartList(order.getId()),
                ResourceDeviceMaintOrderRespVO.OrderPart.class));
        return respVO;
    }

}
