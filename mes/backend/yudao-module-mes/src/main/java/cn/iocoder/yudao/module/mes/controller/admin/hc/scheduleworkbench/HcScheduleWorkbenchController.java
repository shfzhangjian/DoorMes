package cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.service.hc.scheduleworkbench.HcScheduleWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - HC Schedule Workbench")
@RestController
@RequestMapping("/mes/hc/plan/schedule-workbench")
@Validated
public class HcScheduleWorkbenchController {

    @Resource
    private HcScheduleWorkbenchService scheduleWorkbenchService;

    @GetMapping("/get")
    @Operation(summary = "Get schedule workbench data")
    public CommonResult<HcScheduleWorkbenchRespVO> getWorkbench(@Valid HcScheduleWorkbenchReqVO reqVO) {
        return success(scheduleWorkbenchService.getWorkbench(reqVO));
    }

    @GetMapping("/changeover-detail")
    @Operation(summary = "Get schedule workbench changeover detail")
    public CommonResult<List<HcScheduleWorkbenchRespVO.ChangeoverDetail>> getChangeoverDetail(
            @RequestParam("planId") Long planId,
            @RequestParam("type") String type) {
        return success(scheduleWorkbenchService.getChangeoverDetails(planId, type));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export schedule workbench data")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWorkbenchExcel(@Valid HcScheduleWorkbenchReqVO reqVO,
            HttpServletResponse response) throws IOException {
        HcScheduleWorkbenchExportRespVO exportData = scheduleWorkbenchService.getWorkbenchExportData(reqVO);
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("排程工作台.xlsx"));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        FastExcelFactory.write(response.getOutputStream())
                .autoCloseStream(false)
                .head(exportData.getHead())
                .registerWriteHandler(new HcScheduleWorkbenchExcelStyleHandler(exportData))
                .registerConverter(new LongStringConverter())
                .sheet("排程工作台")
                .doWrite(exportData.getRows());
    }

}
