package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiBindStandardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRetentionDestroyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRetentionExpireTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSheetTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiStandardItemCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFaiService;
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

@Tag(name = "管理后台 - FAI首件检验单")
@RestController
@RequestMapping("/mes/quality/fai")
@Validated
public class QmsFaiController {

    private static final String SOURCE_MODULE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String RETENTION_STATUS_RETAINED = "RETAINED";

    @Resource
    private QmsFaiService qmsFaiService;

    @PostMapping("/create")
    @Operation(summary = "创建FAI首件检验单")
    public CommonResult<Long> createFai(@Valid @RequestBody QmsFaiSaveReqVO createReqVO) {
        return success(qmsFaiService.createFai(createReqVO));
    }

    @PutMapping("/bind-standard")
    @Operation(summary = "为待检FAI任务选择检验标准")
    public CommonResult<QmsFaiRespVO> bindStandard(@Valid @RequestBody QmsFaiBindStandardReqVO bindReqVO) {
        return success(qmsFaiService.bindStandard(bindReqVO));
    }

    @GetMapping("/standard-candidates")
    @Operation(summary = "获取FAI检验记录候选标准")
    public CommonResult<List<QmsInspectionStandardCandidateRespVO>> getStandardCandidates(
            @RequestParam("id") Long id) {
        return success(qmsFaiService.getStandardCandidates(id));
    }

    @GetMapping("/standard-item-candidates")
    @Operation(summary = "获取FAI复检标准检验项目候选")
    public CommonResult<List<QmsFaiStandardItemCandidateRespVO>> getStandardItemCandidates(
            @RequestParam("id") Long id, @RequestParam("standardId") Long standardId) {
        return success(qmsFaiService.getStandardItemCandidates(id, standardId));
    }

    @PutMapping("/select-standard")
    @Operation(summary = "为FAI检验记录挂接或切换标准")
    public CommonResult<QmsFaiRespVO> selectStandard(
            @Valid @RequestBody QmsInspectionStandardSelectReqVO reqVO) {
        return success(qmsFaiService.selectStandard(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新FAI首件检验单")
    public CommonResult<Boolean> updateFai(@Valid @RequestBody QmsFaiSaveReqVO updateReqVO) {
        qmsFaiService.updateFai(updateReqVO);
        return success(true);
    }

    @PutMapping("/operator-submit")
    @Operation(summary = "提交FAI机长自检结果")
    public CommonResult<Boolean> operatorSubmit(@Valid @RequestBody QmsFaiSubmitReqVO submitReqVO) {
        qmsFaiService.operatorSubmit(submitReqVO);
        return success(true);
    }

    @PutMapping("/qa-submit")
    @Operation(summary = "提交FAI品质复核结果")
    public CommonResult<Boolean> qaSubmit(@Valid @RequestBody QmsFaiSubmitReqVO submitReqVO) {
        qmsFaiService.qaSubmit(submitReqVO);
        return success(true);
    }

    @PutMapping("/suspend")
    @Operation(summary = "挂起FAI首件检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> suspendFai(@RequestParam("id") Long id) {
        qmsFaiService.suspendFai(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除FAI首件检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteFai(@RequestParam("id") Long id) {
        qmsFaiService.deleteFai(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除FAI首件检验单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteFaiList(@RequestParam("ids") List<Long> ids) {
        qmsFaiService.deleteFaiList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取FAI首件检验单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsFaiRespVO> getFai(@RequestParam("id") Long id) {
        return success(qmsFaiService.getFaiResp(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取FAI首件检验单分页")
    public CommonResult<PageResult<QmsFaiRespVO>> getFaiPage(@Valid QmsFaiPageReqVO pageReqVO) {
        pageReqVO.setExcludedSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        PageResult<QmsFaiOrderDO> pageResult = qmsFaiService.getFaiPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsFaiRespVO.class));
    }

    @GetMapping("/retention/get")
    @Operation(summary = "获取FAI首件留样记录详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsFaiRespVO> getFaiRetention(@RequestParam("id") Long id) {
        return success(qmsFaiService.getFaiRetentionResp(id));
    }

    @GetMapping("/retention/page")
    @Operation(summary = "获取FAI首件留样记录分页")
    public CommonResult<PageResult<QmsFaiRespVO>> getFaiRetentionPage(@Valid QmsFaiPageReqVO pageReqVO) {
        pageReqVO.setRetentionStatus(RETENTION_STATUS_RETAINED);
        pageReqVO.setRetentionActiveOnly(true);
        pageReqVO.setExcludedSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        PageResult<QmsFaiOrderDO> pageResult = qmsFaiService.getFaiPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsFaiRespVO.class));
    }

    @GetMapping("/retention/expired/page")
    @Operation(summary = "获取FAI首件过期留样销毁台账分页")
    public CommonResult<PageResult<QmsFaiRespVO>> getFaiRetentionExpiredPage(@Valid QmsFaiPageReqVO pageReqVO) {
        pageReqVO.setRetentionStatus(RETENTION_STATUS_RETAINED);
        pageReqVO.setRetentionExpiredOnly(true);
        pageReqVO.setExcludedSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        PageResult<QmsFaiOrderDO> pageResult = qmsFaiService.getFaiPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsFaiRespVO.class));
    }

    @PutMapping("/retention/expire-time")
    @Operation(summary = "设置FAI首件留样过期时间")
    public CommonResult<QmsFaiRespVO> updateFaiRetentionExpireTime(
            @Valid @RequestBody QmsFaiRetentionExpireTimeReqVO reqVO) {
        return success(qmsFaiService.updateFaiRetentionExpireTime(reqVO));
    }

    @PutMapping("/retention/destroy")
    @Operation(summary = "批量销毁FAI首件过期留样")
    public CommonResult<Boolean> destroyFaiRetention(@Valid @RequestBody QmsFaiRetentionDestroyReqVO reqVO) {
        qmsFaiService.destroyFaiRetention(reqVO);
        return success(true);
    }

    @GetMapping("/pending-list")
    @Operation(summary = "获取FAI待检/检验中/挂起队列")
    public CommonResult<List<QmsFaiRespVO>> getPendingFaiList() {
        return success(qmsFaiService.getPendingFaiList());
    }

    @PostMapping("/scan/resolve")
    @Operation(summary = "解析FAI扫码并定位质检表")
    public CommonResult<QmsFaiScanRespVO> resolveScan(@Valid @RequestBody QmsFaiScanReqVO scanReqVO) {
        return success(qmsFaiService.resolveScan(scanReqVO));
    }

    @GetMapping("/standard-by-material")
    @Operation(summary = "按物料与工序带出FAI检验标准")
    @Parameter(name = "materialCode", description = "物料编码", required = true)
    public CommonResult<QmsFaiStandardRespVO> getFaiStandardByMaterial(@RequestParam("materialCode") String materialCode,
                                                                       @RequestParam(value = "operationCode", required = false) String operationCode,
                                                                       @RequestParam(value = "operationName", required = false) String operationName) {
        return success(qmsFaiService.getFaiStandard(materialCode, operationCode, operationName));
    }

    @GetMapping("/sheet-template/list")
    @Operation(summary = "获取FAI原始记录表模板列表")
    public CommonResult<List<QmsFaiSheetTemplateRespVO>> getSheetTemplateList(
            @RequestParam(value = "productModel", required = false) String productModel) {
        return success(qmsFaiService.getSheetTemplateList(productModel));
    }

    @GetMapping("/sheet-template/get")
    @Operation(summary = "获取FAI原始记录表模板详情")
    public CommonResult<QmsFaiSheetTemplateRespVO> getSheetTemplate(@RequestParam("id") Long id) {
        return success(qmsFaiService.getSheetTemplate(id));
    }

    @PutMapping("/select-sheet-template")
    @Operation(summary = "选择FAI原始记录表模板")
    public CommonResult<QmsFaiRespVO> selectSheetTemplate(@RequestParam("id") Long id,
                                                          @RequestParam("templateId") Long templateId) {
        return success(qmsFaiService.selectSheetTemplate(id, templateId));
    }

    @PutMapping("/save-sheet-entry")
    @Operation(summary = "保存FAI类Excel手填数据")
    public CommonResult<QmsFaiRespVO> saveSheetEntry(@Valid @RequestBody QmsFaiSaveReqVO saveReqVO) {
        return success(qmsFaiService.saveSheetEntry(saveReqVO));
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存FAI程序化录入草稿")
    public CommonResult<QmsFaiRespVO> saveProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        return success(qmsFaiService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/recalculate")
    @Operation(summary = "重算FAI程序化录入判定")
    public CommonResult<QmsFaiRespVO> recalculateProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        return success(qmsFaiService.recalculateProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/item-audit")
    @Operation(summary = "审核FAI程序化录入当前检验项")
    public CommonResult<QmsFaiRespVO> auditProgramEntryItem(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        return success(qmsFaiService.auditProgramEntryItem(saveReqVO));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交FAI程序化录入检测结果")
    public CommonResult<QmsFaiRespVO> submitProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        return success(qmsFaiService.submitProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/audit")
    @Operation(summary = "整单审核FAI程序化录入检测结果")
    public CommonResult<QmsFaiRespVO> auditProgramEntry(@Valid @RequestBody QmsFaiAuditReqVO auditReqVO) {
        return success(qmsFaiService.auditProgramEntry(auditReqVO));
    }

    @PutMapping("/one-click-pass")
    @Operation(summary = "一键合格FAI首件检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsFaiRespVO> oneClickPass(@RequestParam("id") Long id) {
        return success(qmsFaiService.oneClickPass(id));
    }

    @PutMapping("/one-click-fail")
    @Operation(summary = "一键不合格FAI首件检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsFaiRespVO> oneClickFail(@RequestParam("id") Long id) {
        return success(qmsFaiService.oneClickFail(id));
    }

    @PostMapping("/recheck/apply")
    @Operation(summary = "申请FAI复检")
    public CommonResult<QmsFaiRecheckApplyRespVO> applyRecheckFai(
            @Valid @RequestBody QmsFaiRecheckApplyReqVO reqVO) {
        return success(qmsFaiService.applyRecheckFai(reqVO));
    }

    @PostMapping("/recheck/audit")
    @Operation(summary = "审核FAI复检申请")
    public CommonResult<QmsFaiRecheckApplyRespVO> auditRecheckFai(
            @Valid @RequestBody QmsFaiRecheckAuditReqVO reqVO) {
        return success(qmsFaiService.auditRecheckFai(reqVO));
    }

    @PutMapping("/program-entry/return")
    @Operation(summary = "退回FAI程序化录入修改")
    public CommonResult<QmsFaiRespVO> returnProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        return success(qmsFaiService.returnProgramEntry(saveReqVO));
    }

    @PostMapping("/import-origin-sheet")
    @Operation(summary = "导入FAI历史原始记录表")
    public CommonResult<QmsFaiImportRespVO> importOriginSheet(@RequestParam("id") Long id,
                                                              @RequestParam("templateId") Long templateId,
                                                              @RequestParam(value = "mode", required = false) String mode,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsFaiService.importOriginSheet(id, templateId, mode, file));
    }

    @GetMapping("/item-template/download")
    @Operation(summary = "下载FAI检验项明细导入模板")
    @ApiAccessLog(operateType = EXPORT)
    public void downloadItemTemplate(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeExcelBytes(response, "FAI检验项明细导入模板.xlsx", qmsFaiService.buildItemImportTemplateExcel(id));
    }

    @GetMapping("/item-export")
    @Operation(summary = "导出FAI检验项明细")
    @ApiAccessLog(operateType = EXPORT)
    public void exportItemValues(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeExcelBytes(response, "FAI检验项明细.xlsx", qmsFaiService.buildItemExportExcel(id));
    }

    @PostMapping("/item-import/preview")
    @Operation(summary = "预览校验FAI检验项明细导入")
    public CommonResult<QmsFaiImportRespVO> previewItemImport(@RequestParam("id") Long id,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsFaiService.previewItemImport(id, file));
    }

    @PostMapping("/item-import/confirm")
    @Operation(summary = "确认导入FAI检验项明细")
    public CommonResult<QmsFaiImportRespVO> confirmItemImport(@RequestParam("id") Long id,
                                                              @RequestParam(value = "allowOverwrite", required = false) Boolean allowOverwrite,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsFaiService.confirmItemImport(id, allowOverwrite, file));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出FAI首件检验单 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFaiExcel(@Valid QmsFaiPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        pageReqVO.setExcludedSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        List<QmsFaiOrderDO> list = qmsFaiService.getFaiPage(pageReqVO).getList();
        ExcelUtils.write(response, "FAI首件检验单.xls", "数据", QmsFaiRespVO.class,
                BeanUtils.toBean(list, QmsFaiRespVO.class));
    }

    @GetMapping("/retention/export-excel")
    @Operation(summary = "导出FAI首件留样记录 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFaiRetentionExcel(@Valid QmsFaiPageReqVO pageReqVO,
                                        HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        pageReqVO.setRetentionStatus(RETENTION_STATUS_RETAINED);
        pageReqVO.setRetentionActiveOnly(true);
        pageReqVO.setExcludedSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        List<QmsFaiOrderDO> list = qmsFaiService.getFaiPage(pageReqVO).getList();
        ExcelUtils.write(response, "FAI首件留样记录.xls", "数据", QmsFaiRespVO.class,
                BeanUtils.toBean(list, QmsFaiRespVO.class));
    }

    private void writeExcelBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.getOutputStream().write(data);
    }
}
