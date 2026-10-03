package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisExportVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisSourcePreviewRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsYieldAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 良品率分析")
@RestController
@RequestMapping("/mes/quality/statistics/yield-analysis")
@Validated
public class QmsYieldAnalysisController {

    @Resource
    private QmsYieldAnalysisService qmsYieldAnalysisService;

    @GetMapping("/overview")
    @Operation(summary = "获取良品率分析汇总")
    public CommonResult<QmsYieldAnalysisRespVO> getOverview(@Valid QmsYieldAnalysisReqVO reqVO) {
        return success(qmsYieldAnalysisService.getOverview(reqVO));
    }

    @GetMapping("/target-model-options")
    @Operation(summary = "获取良品率分析目标配置型号下拉")
    public CommonResult<List<String>> getTargetModelOptions() {
        return success(qmsYieldAnalysisService.getTargetModelOptions());
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获取良品率分析片号详情分页")
    public CommonResult<PageResult<QmsYieldAnalysisDetailRespVO>> getDetailPage(@Valid QmsYieldAnalysisReqVO reqVO) {
        return success(qmsYieldAnalysisService.getDetailPage(reqVO));
    }

    @GetMapping("/source-preview")
    @Operation(summary = "获取良品率分析原始记录预览")
    public CommonResult<QmsYieldAnalysisSourcePreviewRespVO> getSourcePreview(@RequestParam("sourceTable") String sourceTable,
                                                                              @RequestParam("sourceId") Long sourceId,
                                                                              @RequestParam(value = "processCode", required = false) String processCode) {
        return success(qmsYieldAnalysisService.getSourcePreview(sourceTable, sourceId, processCode));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出良品率分析")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid QmsYieldAnalysisReqVO reqVO, HttpServletResponse response) throws IOException {
        List<QmsYieldAnalysisExportVO> list = qmsYieldAnalysisService.getExportList(reqVO);
        ExcelUtils.write(response, "良品率分析.xls", "数据", QmsYieldAnalysisExportVO.class, list);
    }
}
