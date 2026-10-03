package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionDestroyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionExpireTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsIqcService;
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

@Tag(name = "管理后台 - IQC进料检验单")
@RestController
@RequestMapping("/mes/quality/iqc")
@Validated
public class QmsIqcController {

    @Resource
    private QmsIqcService qmsIqcService;

    @PostMapping("/create")
    @Operation(summary = "创建IQC进料检验单")
    public CommonResult<Long> createIqc(@Valid @RequestBody QmsIqcSaveReqVO createReqVO) {
        return success(qmsIqcService.createIqc(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新IQC进料检验单")
    public CommonResult<Boolean> updateIqc(@Valid @RequestBody QmsIqcSaveReqVO updateReqVO) {
        qmsIqcService.updateIqc(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交IQC检验结果")
    public CommonResult<Boolean> submitIqc(@Valid @RequestBody QmsIqcSubmitReqVO submitReqVO) {
        qmsIqcService.submitIqc(submitReqVO);
        return success(true);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核IQC检验结果")
    public CommonResult<Boolean> auditIqc(@Valid @RequestBody QmsIqcAuditReqVO auditReqVO) {
        qmsIqcService.auditIqc(auditReqVO);
        return success(true);
    }

    @GetMapping("/standard-candidates")
    @Operation(summary = "获取IQC检验记录候选标准")
    public CommonResult<List<QmsInspectionStandardCandidateRespVO>> getStandardCandidates(
            @RequestParam("id") Long id) {
        return success(qmsIqcService.getStandardCandidates(id));
    }

    @GetMapping("/standard-candidates-by-material")
    @Operation(summary = "按物料获取IQC候选检验标准")
    public CommonResult<List<QmsInspectionStandardCandidateRespVO>> getStandardCandidatesByMaterial(
            @RequestParam(value = "materialId", required = false) Long materialId,
            @RequestParam("materialCode") String materialCode) {
        return success(qmsIqcService.getStandardCandidatesByMaterial(materialId, materialCode));
    }

    @PutMapping("/select-standard")
    @Operation(summary = "为IQC检验记录挂接或切换标准")
    public CommonResult<QmsIqcRespVO> selectStandard(
            @Valid @RequestBody QmsInspectionStandardSelectReqVO reqVO) {
        return success(qmsIqcService.selectStandard(reqVO));
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存IQC程序化录入草稿")
    public CommonResult<QmsIqcRespVO> saveProgramEntry(@RequestBody QmsIqcSaveReqVO saveReqVO) {
        return success(qmsIqcService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/recalculate")
    @Operation(summary = "重算IQC程序化录入判定")
    public CommonResult<QmsIqcRespVO> recalculateProgramEntry(@RequestBody QmsIqcSaveReqVO saveReqVO) {
        return success(qmsIqcService.recalculateProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交IQC程序化录入并送审")
    public CommonResult<QmsIqcRespVO> submitProgramEntry(@RequestBody QmsIqcSaveReqVO saveReqVO) {
        return success(qmsIqcService.submitProgramEntry(saveReqVO));
    }

    @PostMapping("/scan/resolve")
    @Operation(summary = "解析IQC扫码并定位进料检验单")
    public CommonResult<QmsIqcScanRespVO> resolveScan(@Valid @RequestBody QmsIqcScanReqVO scanReqVO) {
        return success(qmsIqcService.resolveScan(scanReqVO));
    }

    @PutMapping("/suspend")
    @Operation(summary = "挂起IQC检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> suspendIqc(@RequestParam("id") Long id) {
        qmsIqcService.suspendIqc(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除IQC进料检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteIqc(@RequestParam("id") Long id) {
        qmsIqcService.deleteIqc(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除IQC进料检验单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteIqcList(@RequestParam("ids") List<Long> ids) {
        qmsIqcService.deleteIqcList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取IQC进料检验单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsIqcRespVO> getIqc(@RequestParam("id") Long id) {
        return success(qmsIqcService.getIqcResp(id));
    }

    @GetMapping("/retention/get")
    @Operation(summary = "获取IQC进料留样记录详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsIqcRespVO> getIqcRetention(@RequestParam("id") Long id) {
        return success(qmsIqcService.getIqcRetentionResp(id));
    }

    @GetMapping("/print-detail")
    @Operation(summary = "获取IQC进料送检单打印详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsIqcRespVO> getIqcPrintDetail(@RequestParam("id") Long id) {
        return success(qmsIqcService.getIqcResp(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取IQC进料检验单分页")
    public CommonResult<PageResult<QmsIqcRespVO>> getIqcPage(@Valid QmsIqcPageReqVO pageReqVO) {
        PageResult<QmsIqcOrderDO> pageResult = qmsIqcService.getIqcPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsIqcRespVO.class));
    }

    @GetMapping("/retention/page")
    @Operation(summary = "获取IQC进料留样记录分页")
    public CommonResult<PageResult<QmsIqcRespVO>> getIqcRetentionPage(@Valid QmsIqcPageReqVO pageReqVO) {
        pageReqVO.setRetentionStatus("RETAINED");
        pageReqVO.setRetentionActiveOnly(true);
        PageResult<QmsIqcOrderDO> pageResult = qmsIqcService.getIqcPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsIqcRespVO.class));
    }

    @GetMapping("/retention/expired/page")
    @Operation(summary = "获取IQC进料过期留样销毁台账分页")
    public CommonResult<PageResult<QmsIqcRespVO>> getIqcRetentionExpiredPage(@Valid QmsIqcPageReqVO pageReqVO) {
        pageReqVO.setRetentionStatus("RETAINED");
        pageReqVO.setRetentionExpiredOnly(true);
        PageResult<QmsIqcOrderDO> pageResult = qmsIqcService.getIqcPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsIqcRespVO.class));
    }

    @PutMapping("/retention/confirm")
    @Operation(summary = "补确认IQC进料留样")
    public CommonResult<QmsIqcRespVO> confirmIqcRetention(@Valid @RequestBody QmsIqcRetentionConfirmReqVO reqVO) {
        return success(qmsIqcService.confirmIqcRetention(reqVO));
    }

    @PutMapping("/retention/expire-time")
    @Operation(summary = "设置IQC进料留样过期时间")
    public CommonResult<QmsIqcRespVO> updateIqcRetentionExpireTime(
            @Valid @RequestBody QmsIqcRetentionExpireTimeReqVO reqVO) {
        return success(qmsIqcService.updateIqcRetentionExpireTime(reqVO));
    }

    @PutMapping("/retention/destroy")
    @Operation(summary = "批量销毁IQC进料过期留样")
    public CommonResult<Boolean> destroyIqcRetention(@Valid @RequestBody QmsIqcRetentionDestroyReqVO reqVO) {
        qmsIqcService.destroyIqcRetention(reqVO);
        return success(true);
    }

    @GetMapping("/pending-list")
    @Operation(summary = "获取IQC待检/执行中队列")
    public CommonResult<List<QmsIqcRespVO>> getPendingIqcList() {
        return success(qmsIqcService.getPendingIqcList());
    }

    @GetMapping("/standard-by-material")
    @Operation(summary = "按物料带出IQC检验标准")
    @Parameter(name = "materialCode", description = "物料编码", required = true)
    public CommonResult<QmsIqcStandardRespVO> getIqcStandardByMaterial(@RequestParam("materialCode") String materialCode) {
        return success(qmsIqcService.getIqcStandardByMaterial(materialCode));
    }

    @GetMapping("/item-template/download")
    @Operation(summary = "下载IQC检验项明细导入模板")
    @ApiAccessLog(operateType = EXPORT)
    public void downloadItemTemplate(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeExcelBytes(response, "IQC检验项明细导入模板.xlsx", qmsIqcService.buildItemImportTemplateExcel(id));
    }

    @GetMapping("/item-export")
    @Operation(summary = "导出IQC检验项明细")
    @ApiAccessLog(operateType = EXPORT)
    public void exportItemValues(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeExcelBytes(response, "IQC检验项明细.xlsx", qmsIqcService.buildItemExportExcel(id));
    }

    @GetMapping("/coa-word")
    @Operation(summary = "导出IQC COA Word")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCoaWord(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        writeWordBytes(response, "IQC_COA_" + id + ".docx", qmsIqcService.buildCoaWord(id));
    }

    @PostMapping("/item-import/preview")
    @Operation(summary = "预览校验IQC检验项明细导入")
    public CommonResult<QmsIqcImportRespVO> previewItemImport(@RequestParam("id") Long id,
                                                             @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsIqcService.previewItemImport(id, file));
    }

    @PostMapping("/item-import/confirm")
    @Operation(summary = "确认导入IQC检验项明细")
    public CommonResult<QmsIqcImportRespVO> confirmItemImport(@RequestParam("id") Long id,
                                                             @RequestParam(value = "allowOverwrite", required = false) Boolean allowOverwrite,
                                                             @RequestParam("file") MultipartFile file) throws IOException {
        return success(qmsIqcService.confirmItemImport(id, allowOverwrite, file));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出IQC进料检验单 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportIqcExcel(@Valid QmsIqcPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsIqcOrderDO> list = qmsIqcService.getIqcPage(pageReqVO).getList();
        ExcelUtils.write(response, "IQC进料检验单.xls", "数据", QmsIqcRespVO.class,
                BeanUtils.toBean(list, QmsIqcRespVO.class));
    }

    @GetMapping("/retention/export-excel")
    @Operation(summary = "导出IQC进料留样记录 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportIqcRetentionExcel(@Valid QmsIqcPageReqVO pageReqVO,
                                        HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        pageReqVO.setRetentionStatus("RETAINED");
        pageReqVO.setRetentionActiveOnly(true);
        List<QmsIqcOrderDO> list = qmsIqcService.getIqcPage(pageReqVO).getList();
        ExcelUtils.write(response, "IQC进料留样记录.xls", "数据", QmsIqcRespVO.class,
                BeanUtils.toBean(list, QmsIqcRespVO.class));
    }

    private void writeExcelBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    private void writeWordBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document;charset=UTF-8");
        response.getOutputStream().write(data);
    }
}
