package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRejectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRecheckHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsProductAbnormalEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 产品异常事件")
@RestController
@RequestMapping("/mes/quality/product-abnormal-event")
@Validated
public class QmsProductAbnormalEventController {

    @Resource
    private QmsProductAbnormalEventService qmsProductAbnormalEventService;

    @GetMapping("/page")
    @Operation(summary = "获取产品异常事件分页")
    public CommonResult<PageResult<QmsProductAbnormalEventRespVO>> getPage(
            @Valid QmsProductAbnormalEventPageReqVO reqVO) {
        return success(qmsProductAbnormalEventService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取产品异常事件统一详情")
    @Parameter(name = "sourceType", description = "检验来源类型", required = true)
    @Parameter(name = "inspectionId", description = "检验单ID", required = true)
    public CommonResult<QmsProductAbnormalEventDetailRespVO> getDetail(
            @RequestParam("sourceType") String sourceType,
            @RequestParam("inspectionId") Long inspectionId) {
        return success(qmsProductAbnormalEventService.getDetail(sourceType, inspectionId));
    }

    @PostMapping("/reject-recheck")
    @Operation(summary = "产品异常事件驳回复检")
    public CommonResult<QmsProductAbnormalEventRejectRespVO> rejectRecheck(
            @Valid @RequestBody QmsProductAbnormalEventRejectReqVO reqVO) {
        return success(qmsProductAbnormalEventService.rejectRecheck(reqVO));
    }

    @GetMapping("/recheck-history")
    @Operation(summary = "获取产品异常事件驳回复检历史")
    @Parameter(name = "sourceType", description = "检验来源类型", required = true)
    @Parameter(name = "inspectionId", description = "检验单ID", required = true)
    public CommonResult<QmsProductAbnormalEventRecheckHistoryRespVO> getRecheckHistory(
            @RequestParam("sourceType") String sourceType,
            @RequestParam("inspectionId") Long inspectionId) {
        return success(qmsProductAbnormalEventService.getRecheckHistory(sourceType, inspectionId));
    }
}
