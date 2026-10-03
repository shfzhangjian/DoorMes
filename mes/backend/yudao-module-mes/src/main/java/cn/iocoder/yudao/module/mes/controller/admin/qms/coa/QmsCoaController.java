package cn.iocoder.yudao.module.mes.controller.admin.qms.coa;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaReportItemExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaReportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.FaiStandardResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportActionReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportAuditReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportCorrectionReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportGenerateReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.MotherBatchPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.MotherBatchResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueApplyReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueCandidatePageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueCandidateResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.StandardItemPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.StandardItemResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateAuditReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplatePageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateSaveReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateStatusReq;
import cn.iocoder.yudao.module.mes.service.qms.coa.QmsCoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - COA报告管理")
@RestController
@RequestMapping("/mes/quality/coa")
@Validated
public class QmsCoaController {

    @Resource
    private QmsCoaService qmsCoaService;

    @GetMapping("/template/page")
    @Operation(summary = "获取COA模板分页")
    public CommonResult<PageResult<TemplateResp>> getTemplatePage(@Valid TemplatePageReq reqVO) {
        return success(qmsCoaService.getTemplatePage(reqVO));
    }

    @GetMapping("/template/get")
    @Operation(summary = "获取COA模板详情")
    public CommonResult<TemplateResp> getTemplate(@RequestParam("id") Long id) {
        return success(qmsCoaService.getTemplate(id));
    }

    @PostMapping("/template/create")
    @Operation(summary = "创建COA模板")
    public CommonResult<Long> createTemplate(@Valid @RequestBody TemplateSaveReq reqVO) {
        return success(qmsCoaService.createTemplate(reqVO));
    }

    @PutMapping("/template/update")
    @Operation(summary = "修改COA模板")
    public CommonResult<Boolean> updateTemplate(@Valid @RequestBody TemplateSaveReq reqVO) {
        qmsCoaService.updateTemplate(reqVO);
        return success(true);
    }

    @PutMapping("/template/submit")
    @Operation(summary = "提交COA模板审核")
    public CommonResult<Boolean> submitTemplate(@RequestParam("id") Long id) {
        qmsCoaService.submitTemplate(id);
        return success(true);
    }

    @PutMapping("/template/audit")
    @Operation(summary = "审核COA模板")
    public CommonResult<Boolean> auditTemplate(@Valid @RequestBody TemplateAuditReq reqVO) {
        qmsCoaService.auditTemplate(reqVO);
        return success(true);
    }

    @PutMapping("/template/status")
    @Operation(summary = "启停COA模板")
    public CommonResult<Boolean> changeTemplateStatus(@Valid @RequestBody TemplateStatusReq reqVO) {
        qmsCoaService.changeTemplateStatus(reqVO);
        return success(true);
    }

    @PostMapping("/template/upgrade")
    @Operation(summary = "升级COA模板版本")
    public CommonResult<Long> upgradeTemplate(@RequestParam("id") Long id) {
        return success(qmsCoaService.upgradeTemplate(id));
    }

    @GetMapping("/template/fai-standard-list")
    @Operation(summary = "获取可用于COA模板的FAI标准")
    public CommonResult<List<FaiStandardResp>> getFaiStandardList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(qmsCoaService.getFaiStandardList(keyword));
    }

    @GetMapping("/template/standard-item-page")
    @Operation(summary = "分页查询可引入的过程首检和成品检验标准明细")
    public CommonResult<PageResult<StandardItemResp>> getStandardItemPage(@Valid StandardItemPageReq reqVO) {
        return success(qmsCoaService.getStandardItemPage(reqVO));
    }

    @GetMapping("/report/page")
    @Operation(summary = "获取COA报告分页")
    public CommonResult<PageResult<ReportResp>> getReportPage(@Valid ReportPageReq reqVO) {
        return success(qmsCoaService.getReportPage(reqVO));
    }

    @GetMapping("/report/export-excel")
    @Operation(summary = "导出COA报告记录 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReportExcel(@Valid ReportPageReq reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ReportResp> list = qmsCoaService.getReportPage(reqVO).getList();
        ExcelUtils.write(response, "COA报告记录.xlsx", "COA报告记录", QmsCoaReportExcelVO.class,
                BeanUtils.toBean(list, QmsCoaReportExcelVO.class));
    }

    @GetMapping("/report/item/export-excel")
    @Operation(summary = "导出COA检验结果明细 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReportItemExcel(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        ReportResp report = qmsCoaService.getReport(id);
        List<QmsCoaReportItemExcelVO> rows = new ArrayList<>();
        List<ReportItemResp> items = report.getItems() == null ? List.of() : report.getItems();
        for (int index = 0; index < items.size(); index++) {
            ReportItemResp item = items.get(index);
            QmsCoaReportItemExcelVO row = new QmsCoaReportItemExcelVO();
            row.setRowNo(index + 1);
            row.setItemGroup(item.getItemGroup());
            row.setItemNameCn(item.getItemNameCn());
            row.setUnit(item.getUnit());
            row.setDisplayValue(item.getDisplayValue());
            row.setSpecText(item.getSpecText());
            row.setTargetValue(item.getTargetValue());
            row.setCoaSpecText(item.getCoaSpecText());
            row.setInspectionMethod(item.getInspectionMethod());
            row.setValueSourceType(valueSourceText(item.getValueSourceType()));
            row.setSourceStandard(joinText(item.getSourceStandardApplyType(), item.getSourceStandardNo()));
            row.setSourceProcessName(item.getSourceProcessName());
            // 报告项目是模板的快照；当前快照中项目名称即模板的取值项目展示名。
            row.setSourceInspectionItem(item.getItemNameCn());
            row.setValueStrategy(valueStrategyText(item.getValueStrategy()));
            rows.add(row);
        }
        ExcelUtils.write(response, report.getCoaNo() + "-检验结果.xlsx", "检验结果",
                QmsCoaReportItemExcelVO.class, rows);
    }

    @GetMapping("/report/mother-batch-page")
    @Operation(summary = "分页查询COA模板产品可选母批次")
    public CommonResult<PageResult<MotherBatchResp>> getMotherBatchPage(@Valid MotherBatchPageReq reqVO) {
        return success(qmsCoaService.getMotherBatchPage(reqVO));
    }

    @GetMapping("/report/get")
    @Operation(summary = "获取COA报告详情")
    public CommonResult<ReportResp> getReport(@RequestParam("id") Long id) {
        return success(qmsCoaService.getReport(id));
    }

    @PostMapping("/report/generate")
    @Operation(summary = "按批次生成COA报告草稿")
    public CommonResult<Long> generateReport(@Valid @RequestBody ReportGenerateReq reqVO) {
        return success(qmsCoaService.generateReport(reqVO));
    }

    @PutMapping("/report/refresh")
    @Operation(summary = "刷新COA报告的FAI和出货来源")
    public CommonResult<Boolean> refreshReport(@RequestParam("id") Long id) {
        qmsCoaService.refreshReport(id);
        return success(true);
    }

    @PutMapping("/report/item/correct")
    @Operation(summary = "修正COA报告项目值并保留历史")
    public CommonResult<Boolean> correctReportItem(@Valid @RequestBody ReportCorrectionReq reqVO) {
        qmsCoaService.correctReportItem(reqVO);
        return success(true);
    }

    @GetMapping("/report/item/value-candidate-page")
    @Operation(summary = "分页查询COA项目可选过程或成品检验实测值")
    public CommonResult<PageResult<ReportItemValueCandidateResp>> getReportItemValueCandidatePage(
            @Valid ReportItemValueCandidatePageReq reqVO) {
        return success(qmsCoaService.getReportItemValueCandidatePage(reqVO));
    }

    @PutMapping("/report/item/apply-value")
    @Operation(summary = "保存COA项目取值、照片或人工结果")
    public CommonResult<Boolean> applyReportItemValue(@Valid @RequestBody ReportItemValueApplyReq reqVO) {
        qmsCoaService.applyReportItemValue(reqVO);
        return success(true);
    }

    @PutMapping("/report/submit")
    @Operation(summary = "提交COA报告审核")
    public CommonResult<Boolean> submitReport(@RequestParam("id") Long id) {
        qmsCoaService.submitReport(id);
        return success(true);
    }

    @PutMapping("/report/confirm")
    @Operation(summary = "COA确认或退回报告")
    public CommonResult<Boolean> confirmReport(@Valid @RequestBody ReportAuditReq reqVO) {
        qmsCoaService.confirmReport(reqVO);
        return success(true);
    }

    @PutMapping("/report/audit")
    @Operation(summary = "审核COA报告")
    public CommonResult<Boolean> auditReport(@Valid @RequestBody ReportAuditReq reqVO) {
        qmsCoaService.auditReport(reqVO);
        return success(true);
    }

    @PutMapping("/report/issue")
    @Operation(summary = "正式签发COA报告")
    public CommonResult<Boolean> issueReport(@Valid @RequestBody ReportActionReq reqVO) {
        qmsCoaService.issueReport(reqVO);
        return success(true);
    }

    @PostMapping("/report/revision")
    @Operation(summary = "创建COA报告新版本")
    public CommonResult<Long> createRevision(@Valid @RequestBody ReportActionReq reqVO) {
        return success(qmsCoaService.createRevision(reqVO));
    }

    @PutMapping("/report/void")
    @Operation(summary = "作废COA报告")
    public CommonResult<Boolean> voidReport(@Valid @RequestBody ReportActionReq reqVO) {
        qmsCoaService.voidReport(reqVO);
        return success(true);
    }

    private static String joinText(String left, String right) {
        if (left == null || left.isBlank()) {
            return right == null || right.isBlank() ? null : right;
        }
        return right == null || right.isBlank() ? left : left + " / " + right;
    }

    private static String valueSourceText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("PROCESS_INSPECTION".equals(value)) {
            return "过程检验";
        }
        if ("PHOTO_UPLOAD".equals(value)) {
            return "上传照片";
        }
        return "人工填写";
    }

    private static String valueStrategyText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value) {
            case "QA_AVG" -> "QA平均值";
            case "QA_MIN" -> "QA最小值";
            case "QA_MAX" -> "QA最大值";
            case "QA_RESULT" -> "QA定性结果";
            case "LATEST_SAMPLE" -> "最新样本";
            case "VARIANCE" -> "方差";
            case "MANUAL" -> "人工结果";
            case "PHOTO" -> "图片结果";
            default -> value;
        };
    }
}
