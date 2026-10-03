package cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.CreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.DetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.InstructionOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.OperationOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.OrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ProductOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.SegmentOptionRespVO;
import cn.iocoder.yudao.module.mes.service.hc.productionfactadjust.HcProductionFactAdjustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.UPDATE;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 生产事实调账")
@RestController
@RequestMapping("/mes/hc/plan/production-fact-adjust")
@Validated
public class HcProductionFactAdjustController {

    @Resource
    private HcProductionFactAdjustService productionFactAdjustService;

    @GetMapping("/operation-option-list")
    @Operation(summary = "查询计划工序选项")
    public CommonResult<List<OperationOptionRespVO>> getOperationOptionList(@RequestParam("planNo") String planNo) {
        return success(productionFactAdjustService.getOperationOptions(planNo));
    }

    @GetMapping("/segment-option-list")
    @Operation(summary = "查询粘胶2分段选项")
    public CommonResult<List<SegmentOptionRespVO>> getSegmentOptionList(@RequestParam("planOperationId") Long planOperationId) {
        return success(productionFactAdjustService.getSegmentOptions(planOperationId));
    }

    @GetMapping("/product-option-list")
    @Operation(summary = "查询有效型号料号组合")
    public CommonResult<List<ProductOptionRespVO>> getProductOptionList(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(productionFactAdjustService.getProductOptions(keyword));
    }

    @GetMapping("/instruction-option-list")
    @Operation(summary = "查询分段换型指令选项")
    public CommonResult<List<InstructionOptionRespVO>> getInstructionOptionList(@RequestParam("planOperationId") Long planOperationId,
                                                                                  @RequestParam("segmentBatchNo") String segmentBatchNo) {
        return success(productionFactAdjustService.getInstructionOptions(planOperationId, segmentBatchNo));
    }

    @GetMapping("/preview")
    @Operation(summary = "预览换型产品快照调账影响范围")
    public CommonResult<PreviewRespVO> preview(@Valid PreviewReqVO reqVO) {
        return success(productionFactAdjustService.preview(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "发起生产事实调账单")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Long> create(@Valid @RequestBody CreateReqVO reqVO) {
        return success(productionFactAdjustService.create(reqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "审核生产事实调账单")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> approve(@Valid @RequestBody ApproveReqVO reqVO) {
        productionFactAdjustService.approve(reqVO);
        return success(true);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行已审核通过的生产事实调账单")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> execute(@Valid @RequestBody ExecuteReqVO reqVO) {
        productionFactAdjustService.execute(reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询生产事实调账单")
    public CommonResult<PageResult<OrderRespVO>> getPage(@Valid PageReqVO reqVO) {
        return success(productionFactAdjustService.getPage(reqVO));
    }

    @GetMapping("/detail-list")
    @Operation(summary = "查询调账单逐片执行明细")
    @Parameter(name = "orderId", required = true)
    public CommonResult<List<DetailRespVO>> getDetailList(@RequestParam("orderId") Long orderId) {
        return success(productionFactAdjustService.getDetailList(orderId));
    }
}
