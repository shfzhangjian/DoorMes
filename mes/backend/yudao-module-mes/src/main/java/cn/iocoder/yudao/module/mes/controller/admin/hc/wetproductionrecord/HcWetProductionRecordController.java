package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.service.hc.wetproductionrecord.HcWetProductionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 湿法生产记录表")
@RestController
@RequestMapping("/mes/hc/execution/wet-production-record")
@Validated
public class HcWetProductionRecordController {

    @Resource
    private HcWetProductionRecordService hcWetProductionRecordService;

    @GetMapping("/page")
    @Operation(summary = "湿法生产记录分页")
    public CommonResult<PageResult<HcWetProductionRecordRespVO>> page(@Valid HcWetProductionRecordPageReqVO reqVO) {
        reqVO.setStatus(null);
        return success(hcWetProductionRecordService.getReportPage(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "湿法生产记录导出")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcWetProductionRecordPageReqVO reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setStatus(null);
        ExcelUtils.write(response, "湿法生产记录表.xlsx", "湿法生产记录", HcWetProductionRecordImportExcelVO.class,
                hcWetProductionRecordService.buildReportExportList(reqVO));
    }
}
