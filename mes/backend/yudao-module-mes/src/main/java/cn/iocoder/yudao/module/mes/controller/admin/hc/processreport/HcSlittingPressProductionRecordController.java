package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcSlittingPressProductionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 分切&压槽生产记录表")
@RestController
@RequestMapping("/mes/hc/plan/slitting-press-production-record")
@Validated
public class HcSlittingPressProductionRecordController {
    @Resource private HcSlittingPressProductionRecordService reportService;
    @GetMapping("/page") @Operation(summary = "获得分切&压槽生产记录表分页")
    public CommonResult<PageResult<HcSlittingPressProductionRecordRespVO>> page(@Valid HcSlittingPressProductionRecordPageReqVO req){req.setStatus(null);return success(reportService.getPage(req));}
    @GetMapping("/export-excel") @ApiAccessLog(operateType = EXPORT)
    public void export(@Valid HcSlittingPressProductionRecordPageReqVO req,HttpServletResponse response)throws IOException{
        req.setPageSize(PageParam.PAGE_SIZE_NONE);
        req.setStatus(null);
        ExcelUtils.write(response,"分切&压槽生产记录表.xlsx","分切&压槽生产记录表",
                HcSlittingPressProductionRecordExcelVO.class,reportService.getList(req).stream().map(this::toExcel).toList());
    }

    private HcSlittingPressProductionRecordExcelVO toExcel(HcSlittingPressProductionRecordRespVO source) {
        HcSlittingPressProductionRecordExcelVO target = new HcSlittingPressProductionRecordExcelVO();
        target.setReportDate(source.getRecordTime() != null
                ? source.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : source.getReportDate() == null ? "未记录具体时间"
                : source.getReportDate() + "（未记录具体时间）");
        target.setModelCode(source.getModelCode());
        target.setPadType(source.getPadType());
        target.setMaterialCode(source.getMaterialCode());
        target.setBatchNo(source.getBatchNo());
        target.setSlittingInputM(source.getSlittingInputM());
        target.setSlittingOutputPcs(source.getSlittingOutputPcs());
        target.setSlittingNgPcs(source.getSlittingNgPcs());
        target.setPressSlotActualInputPcs(source.getPressSlotActualInputPcs());
        target.setPressSlotActualOutputPcs(source.getPressSlotActualOutputPcs());
        target.setPressSlotOutputPcs(source.getPressSlotOutputPcs());
        target.setRollerCleanAccumulatedPcs(source.getRollerCleanAccumulatedPcs());
        target.setRollerCleanUseDays(source.getRollerCleanUseDays());
        target.setBearingReplaceAccumulatedPcs(source.getBearingReplaceAccumulatedPcs());
        target.setBearingReplaceUseDays(source.getBearingReplaceUseDays());
        target.setRecorderName(source.getRecorderName());
        target.setRecordSource(source.getRecordSource());
        target.setRecordTime(source.getRecordTime() == null ? null
                : source.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        target.setRemark(source.getRemark());
        return target;
    }
}
