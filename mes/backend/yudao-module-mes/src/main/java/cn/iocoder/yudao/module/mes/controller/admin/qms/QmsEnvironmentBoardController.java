package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentBoardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentBoardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsEnvironmentBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 温湿度录入看板")
@RestController
@RequestMapping("/mes/quality/environment-board")
@Validated
public class QmsEnvironmentBoardController {

    @Resource
    private QmsEnvironmentBoardService environmentBoardService;

    @GetMapping("/board")
    @Operation(summary = "获取温湿度录入看板")
    public CommonResult<QmsEnvironmentBoardRespVO> getBoard(@Valid QmsEnvironmentBoardReqVO reqVO) {
        return success(environmentBoardService.getBoard(reqVO));
    }

    @PostMapping("/standard/save")
    @Operation(summary = "保存温湿度标准范围")
    public CommonResult<QmsEnvironmentBoardRespVO.Standard> saveStandard(
            @Valid @RequestBody QmsEnvironmentStandardSaveReqVO reqVO) {
        return success(environmentBoardService.saveStandard(reqVO));
    }

    @PostMapping("/record/save")
    @Operation(summary = "保存每日温湿度记录")
    public CommonResult<QmsEnvironmentBoardRespVO.Record> saveRecord(
            @Valid @RequestBody QmsEnvironmentRecordSaveReqVO reqVO) {
        return success(environmentBoardService.saveRecord(reqVO));
    }

    @PostMapping("/record/correct")
    @Operation(summary = "修正历史温湿度记录")
    public CommonResult<QmsEnvironmentBoardRespVO.Record> correctRecord(
            @Valid @RequestBody QmsEnvironmentRecordCorrectReqVO reqVO) {
        return success(environmentBoardService.correctRecord(reqVO));
    }

    @PostMapping("/record/confirm")
    @Operation(summary = "确认每日温湿度记录")
    public CommonResult<QmsEnvironmentBoardRespVO.Record> confirmRecord(
            @Valid @RequestBody QmsEnvironmentRecordConfirmReqVO reqVO) {
        return success(environmentBoardService.confirmRecord(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出温湿度记录 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid QmsEnvironmentBoardReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        List<QmsEnvironmentRecordExcelVO> list = environmentBoardService.buildExportList(reqVO);
        ExcelUtils.write(response, "温湿度记录.xlsx", "温湿度记录", QmsEnvironmentRecordExcelVO.class, list);
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入温湿度记录 Excel")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<QmsEnvironmentImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("workshopCode") String workshopCode,
            @RequestParam(value = "workshopName", required = false) String workshopName,
            @RequestParam("recordMonth") String recordMonth,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "operatorUsername", required = false) String operatorUsername,
            @RequestParam("operatorName") String operatorName) throws IOException {
        return success(environmentBoardService.importRecords(workshopCode, workshopName, recordMonth, file,
                operatorId, operatorUsername, operatorName));
    }
}
