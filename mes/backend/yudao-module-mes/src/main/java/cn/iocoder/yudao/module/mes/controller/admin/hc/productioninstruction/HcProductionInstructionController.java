package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionMessagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRevokeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.service.hc.productioninstruction.HcProductionInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - HC 生产指令")
@RestController
@RequestMapping("/mes/hc/plan/production-instruction")
@Validated
public class HcProductionInstructionController {

    @Resource
    private HcProductionInstructionService hcProductionInstructionService;

    @PostMapping("/issue")
    @Operation(summary = "下达生产指令")
    public CommonResult<Long> issueProductionInstruction(
            @Valid @RequestBody HcProductionInstructionSaveReqVO reqVO) {
        return success(hcProductionInstructionService.issueProductionInstruction(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改已下达未确认的生产指令")
    public CommonResult<Boolean> updateProductionInstruction(
            @Valid @RequestBody HcProductionInstructionSaveReqVO reqVO) {
        hcProductionInstructionService.updateProductionInstruction(reqVO);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认生产指令")
    public CommonResult<Boolean> confirmProductionInstruction(@RequestParam("id") Long id) {
        hcProductionInstructionService.confirmProductionInstruction(id);
        return success(true);
    }

    @PutMapping("/revoke")
    @Operation(summary = "撤下生产指令")
    public CommonResult<Boolean> revokeProductionInstruction(
            @Valid @RequestBody HcProductionInstructionRevokeReqVO reqVO) {
        hcProductionInstructionService.revokeProductionInstruction(reqVO);
        return success(true);
    }

    @PutMapping("/changeover/start")
    @Operation(summary = "开始执行换型生产指令")
    public CommonResult<HcProductionInstructionRespVO> startChangeoverInstruction(
            @Valid @RequestBody HcProductionInstructionChangeoverStartReqVO reqVO) {
        return success(BeanUtils.toBean(
                hcProductionInstructionService.startChangeoverInstruction(reqVO),
                HcProductionInstructionRespVO.class));
    }

    @PostMapping("/changeover/piece")
    @Operation(summary = "记录换型生产指令扫码片号")
    public CommonResult<HcProductionInstructionRespVO> recordChangeoverPiece(
            @Valid @RequestBody HcProductionInstructionChangeoverPieceReqVO reqVO) {
        return success(BeanUtils.toBean(
                hcProductionInstructionService.recordChangeoverPiece(reqVO),
                HcProductionInstructionRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除未确认生产指令")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> deleteProductionInstruction(@RequestParam("id") Long id) {
        hcProductionInstructionService.deleteProductionInstruction(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取生产指令")
    @Parameter(name = "id", required = true)
    public CommonResult<HcProductionInstructionRespVO> getProductionInstruction(@RequestParam("id") Long id) {
        HcProductionInstructionDO entity = hcProductionInstructionService.getProductionInstruction(id);
        return success(BeanUtils.toBean(entity, HcProductionInstructionRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取生产指令列表")
    public CommonResult<List<HcProductionInstructionRespVO>> getProductionInstructionList(
            @Valid HcProductionInstructionPageReqVO reqVO) {
        return success(BeanUtils.toBean(
                hcProductionInstructionService.getProductionInstructionList(reqVO),
                HcProductionInstructionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取生产指令分页")
    public CommonResult<PageResult<HcProductionInstructionRespVO>> getProductionInstructionPage(
            @Valid HcProductionInstructionPageReqVO pageReqVO) {
        PageResult<HcProductionInstructionDO> pageResult =
                hcProductionInstructionService.getProductionInstructionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcProductionInstructionRespVO.class));
    }

    @GetMapping("/operation/list")
    @Operation(summary = "工序获取活动生产指令")
    public CommonResult<List<HcProductionInstructionRespVO>> getOperationInstructionList(
            @Valid HcProductionInstructionOperationReqVO reqVO) {
        return success(BeanUtils.toBean(
                hcProductionInstructionService.getOperationInstructionList(reqVO),
                HcProductionInstructionRespVO.class));
    }

    @GetMapping("/message/page")
    @Operation(summary = "工序获取生产指令消息分页")
    public CommonResult<PageResult<HcProductionInstructionRespVO>> getCurrentUserMessagePage(
            @Valid HcProductionInstructionMessagePageReqVO pageReqVO) {
        return success(hcProductionInstructionService.getCurrentUserMessagePage(pageReqVO));
    }

    @GetMapping("/message/unread-count")
    @Operation(summary = "工序获取有效生产指令数")
    public CommonResult<Long> getCurrentUserUnreadMessageCount(
            @Valid HcProductionInstructionMessagePageReqVO reqVO) {
        return success(hcProductionInstructionService.getCurrentUserUnreadMessageCount(reqVO));
    }

    @PutMapping("/message/read")
    @Operation(summary = "当前用户标记生产指令消息已读")
    public CommonResult<Boolean> markCurrentUserMessageRead(@RequestParam("id") Long id) {
        hcProductionInstructionService.markCurrentUserMessageRead(id);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出生产指令")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductionInstructionExcel(@Valid HcProductionInstructionPageReqVO pageReqVO,
                                                 HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcProductionInstructionDO> list =
                hcProductionInstructionService.getProductionInstructionPage(pageReqVO).getList();
        ExcelUtils.write(response, "生产指令.xls", "data", HcProductionInstructionRespVO.class,
                BeanUtils.toBean(list, HcProductionInstructionRespVO.class));
    }

}
