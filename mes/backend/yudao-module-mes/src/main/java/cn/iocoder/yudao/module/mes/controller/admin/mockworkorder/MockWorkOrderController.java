package cn.iocoder.yudao.module.mes.controller.admin.mockworkorder;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.mockworkorder.MockWorkOrderDO;
import cn.iocoder.yudao.module.mes.service.mockworkorder.MockWorkOrderService;

@Tag(name = "管理后台 - 模拟生产工单表（用于AI大模型MCP调用测试）")
@RestController
@RequestMapping("/mes/mock-work-order")
@Validated
public class MockWorkOrderController {

    @Resource
    private MockWorkOrderService mockWorkOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建模拟生产工单表（用于AI大模型MCP调用测试）")
    public CommonResult<Long> createMockWorkOrder(@Valid @RequestBody MockWorkOrderSaveReqVO createReqVO) {
        return success(mockWorkOrderService.createMockWorkOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新模拟生产工单表（用于AI大模型MCP调用测试）")
    public CommonResult<Boolean> updateMockWorkOrder(@Valid @RequestBody MockWorkOrderSaveReqVO updateReqVO) {
        mockWorkOrderService.updateMockWorkOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模拟生产工单表（用于AI大模型MCP调用测试）")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteMockWorkOrder(@RequestParam("id") Long id) {
        mockWorkOrderService.deleteMockWorkOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除模拟生产工单表（用于AI大模型MCP调用测试）")
    public CommonResult<Boolean> deleteMockWorkOrderList(@RequestParam("ids") List<Long> ids) {
        mockWorkOrderService.deleteMockWorkOrderListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得模拟生产工单表（用于AI大模型MCP调用测试）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<MockWorkOrderRespVO> getMockWorkOrder(@RequestParam("id") Long id) {
        MockWorkOrderDO mockWorkOrder = mockWorkOrderService.getMockWorkOrder(id);
        return success(BeanUtils.toBean(mockWorkOrder, MockWorkOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得模拟生产工单表（用于AI大模型MCP调用测试）分页")
    public CommonResult<PageResult<MockWorkOrderRespVO>> getMockWorkOrderPage(@Valid MockWorkOrderPageReqVO pageReqVO) {
        PageResult<MockWorkOrderDO> pageResult = mockWorkOrderService.getMockWorkOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MockWorkOrderRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出模拟生产工单表（用于AI大模型MCP调用测试） Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMockWorkOrderExcel(@Valid MockWorkOrderPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MockWorkOrderDO> list = mockWorkOrderService.getMockWorkOrderPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "模拟生产工单表（用于AI大模型MCP调用测试）.xls", "数据", MockWorkOrderRespVO.class,
                BeanUtils.toBean(list, MockWorkOrderRespVO.class));
    }

}
