package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSheetTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFqcService;
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
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FQC成品检验")
@RestController
@RequestMapping("/mes/quality/fqc")
@Validated
public class QmsFqcController {

    @Resource
    private QmsFqcService qmsFqcService;

    @PostMapping("/create")
    @Operation(summary = "创建FQC成品检验单")
    public CommonResult<Long> createFqc(@Valid @RequestBody QmsFqcSaveReqVO createReqVO) {
        return success(qmsFqcService.createFqc(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新FQC成品检验单")
    public CommonResult<Boolean> updateFqc(@Valid @RequestBody QmsFqcSaveReqVO updateReqVO) {
        qmsFqcService.updateFqc(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交FQC成品检验结果")
    public CommonResult<Boolean> submitFqc(@Valid @RequestBody QmsFqcSaveReqVO submitReqVO) {
        qmsFqcService.submitFqc(submitReqVO);
        return success(true);
    }

    @PutMapping("/suspend")
    @Operation(summary = "挂起FQC成品检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> suspendFqc(@RequestParam("id") Long id) {
        qmsFqcService.suspendFqc(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除FQC成品检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteFqc(@RequestParam("id") Long id) {
        qmsFqcService.deleteFqc(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除FQC成品检验单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteFqcList(@RequestParam("ids") List<Long> ids) {
        qmsFqcService.deleteFqcList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取FQC成品检验详情")
    public CommonResult<QmsFqcRespVO> getFqc(@RequestParam("id") Long id) {
        return success(qmsFqcService.getFqcResp(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取FQC成品检验分页")
    public CommonResult<PageResult<QmsFqcRespVO>> getFqcPage(@Valid QmsFqcPageReqVO pageReqVO) {
        PageResult<QmsFqcOrderDO> pageResult = qmsFqcService.getFqcPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsFqcRespVO.class));
    }

    @GetMapping("/pending-list")
    @Operation(summary = "获取FQC待检/挂起队列")
    public CommonResult<List<QmsFqcRespVO>> getPendingFqcList() {
        return success(qmsFqcService.getPendingFqcList());
    }

    @GetMapping("/defect-code-options")
    @Operation(summary = "获取FQC可选缺陷代码")
    public CommonResult<List<QmsDefectCodeRespVO>> getFqcDefectCodeOptions() {
        return success(qmsFqcService.getFqcDefectCodeOptions());
    }

    @GetMapping("/standard-by-product")
    @Operation(summary = "按物料带出FQC检验标准")
    public CommonResult<QmsFqcStandardRespVO> getFqcStandardByProduct(@RequestParam("materialCode") String materialCode) {
        return success(qmsFqcService.getFqcStandard(materialCode));
    }

    @PostMapping("/scan/resolve")
    @Operation(summary = "解析FQC扫码并定位成品检验单")
    public CommonResult<QmsFqcScanRespVO> resolveScan(@Valid @RequestBody QmsFqcScanReqVO scanReqVO) {
        return success(qmsFqcService.resolveScan(scanReqVO));
    }

    @GetMapping("/sheet-template/list")
    @Operation(summary = "获取FQC原始记录表模板列表")
    public CommonResult<List<QmsFqcSheetTemplateRespVO>> getSheetTemplateList(
            @RequestParam(value = "productModel", required = false) String productModel) {
        return success(qmsFqcService.getSheetTemplateList(productModel));
    }

    @GetMapping("/sheet-template/get")
    @Operation(summary = "获取FQC原始记录表模板详情")
    public CommonResult<QmsFqcSheetTemplateRespVO> getSheetTemplate(@RequestParam("id") Long id) {
        return success(qmsFqcService.getSheetTemplate(id));
    }

    @PutMapping("/select-sheet-template")
    @Operation(summary = "选择FQC原始记录表模板")
    public CommonResult<QmsFqcRespVO> selectSheetTemplate(@RequestParam("id") Long id,
                                                          @RequestParam("templateId") Long templateId) {
        return success(qmsFqcService.selectSheetTemplate(id, templateId));
    }

    @PutMapping("/save-sheet-entry")
    @Operation(summary = "保存FQC固定表格式录入")
    public CommonResult<QmsFqcRespVO> saveSheetEntry(@Valid @RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFqcService.saveSheetEntry(saveReqVO));
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存FQC程序化录入草稿")
    public CommonResult<QmsFqcRespVO> saveProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFqcService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/recalculate")
    @Operation(summary = "重算FQC程序化录入判定")
    public CommonResult<QmsFqcRespVO> recalculateProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFqcService.recalculateProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/item-audit")
    @Operation(summary = "审核FQC当前检验项")
    public CommonResult<QmsFqcRespVO> auditProgramEntryItem(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFqcService.auditProgramEntryItem(saveReqVO));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交FQC概览确认")
    public CommonResult<QmsFqcRespVO> submitProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFqcService.submitProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/audit")
    @Operation(summary = "审核FQC成品放行/冻结")
    public CommonResult<QmsFqcRespVO> auditProgramEntry(@Valid @RequestBody QmsFqcAuditReqVO auditReqVO) {
        return success(qmsFqcService.auditProgramEntry(auditReqVO));
    }

    @PutMapping("/program-entry/return")
    @Operation(summary = "退回FQC修改")
    public CommonResult<QmsFqcRespVO> returnProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFqcService.returnProgramEntry(saveReqVO));
    }

    @PostMapping("/import-origin-sheet")
    @Operation(summary = "导入FQC历史原始记录表")
    public CommonResult<QmsFqcImportRespVO> importOriginSheet(@RequestParam("id") Long id,
                                                              @RequestParam("templateId") Long templateId,
                                                              @RequestParam(value = "mode", required = false) String mode,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsFqcService.importOriginSheet(id, templateId, mode, file));
    }

    @GetMapping("/item-template/download")
    @Operation(summary = "下载FQC检验项明细导入模板")
    @ApiAccessLog(operateType = EXPORT)
    public void downloadItemTemplate(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeExcelBytes(response, "FQC检验项明细导入模板.xlsx", qmsFqcService.buildItemImportTemplateExcel(id));
    }

    @GetMapping("/item-export")
    @Operation(summary = "导出FQC检验项明细")
    @ApiAccessLog(operateType = EXPORT)
    public void exportItemValues(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeExcelBytes(response, "FQC检验项明细.xlsx", qmsFqcService.buildItemExportExcel(id));
    }

    @PostMapping("/item-import/preview")
    @Operation(summary = "预览校验FQC检验项明细导入")
    public CommonResult<QmsFqcImportRespVO> previewItemImport(@RequestParam("id") Long id,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsFqcService.previewItemImport(id, file));
    }

    @PostMapping("/item-import/confirm")
    @Operation(summary = "确认导入FQC检验项明细")
    public CommonResult<QmsFqcImportRespVO> confirmItemImport(@RequestParam("id") Long id,
                                                              @RequestParam(value = "allowOverwrite", required = false) Boolean allowOverwrite,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsFqcService.confirmItemImport(id, allowOverwrite, file));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出FQC成品检验 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFqcExcel(@Valid QmsFqcPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsFqcOrderDO> list = qmsFqcService.getFqcPage(pageReqVO).getList();
        ExcelUtils.write(response, "FQC成品检验.xls", "数据", QmsFqcRespVO.class,
                BeanUtils.toBean(list, QmsFqcRespVO.class));
    }

    private void writeExcelBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.getOutputStream().write(data);
    }
}
