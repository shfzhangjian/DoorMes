package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcItemPhotoRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcItemPhotoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcSubmissionDetailPhotoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcSubmissionDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditRevokeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsCutRoundFqcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 裁切成品检验")
@RestController
@RequestMapping("/mes/quality/cut-round-fqc")
@Validated
public class QmsCutRoundFqcController {

    @Resource
    private QmsCutRoundFqcService qmsCutRoundFqcService;

    @GetMapping("/page")
    @Operation(summary = "获取裁切成品检验分页")
    public CommonResult<PageResult<QmsCutRoundFqcRespVO>> getPage(@Valid QmsCutRoundFqcPageReqVO pageReqVO) {
        return success(qmsCutRoundFqcService.getPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取裁切成品检验详情")
    @Parameter(name = "id", description = "FQC主单ID", required = true)
    public CommonResult<QmsCutRoundFqcRespVO> get(@RequestParam("id") Long id) {
        return success(qmsCutRoundFqcService.get(id));
    }

    @GetMapping("/get-light")
    @Operation(summary = "获取裁切成品检验轻量详情")
    @Parameter(name = "id", description = "FQC主单ID", required = true)
    public CommonResult<QmsCutRoundFqcRespVO> getLight(@RequestParam("id") Long id) {
        return success(qmsCutRoundFqcService.getLight(id));
    }

    @GetMapping("/submission-detail/items")
    @Operation(summary = "获取裁切成品检验送检片号检验项目")
    public CommonResult<List<QmsFqcRespVO.FqcItem>> getSubmissionDetailItems(
            @RequestParam("id") Long id,
            @RequestParam("submissionDetailId") Long submissionDetailId) {
        return success(qmsCutRoundFqcService.getSubmissionDetailItems(id, submissionDetailId));
    }

    @GetMapping("/submission-detail/item-photos")
    @Operation(summary = "获取裁切成品检验项目照片")
    public CommonResult<List<QmsCutRoundFqcItemPhotoRespVO>> getItemPhotos(
            @RequestParam("id") Long id,
            @RequestParam("submissionDetailId") Long submissionDetailId,
            @RequestParam("fqcItemId") Long fqcItemId) {
        return success(qmsCutRoundFqcService.getItemPhotos(id, submissionDetailId, fqcItemId));
    }

    @PutMapping("/submission-detail/item-photo/save")
    @Operation(summary = "保存裁切成品检验项目照片")
    public CommonResult<QmsCutRoundFqcItemPhotoRespVO> saveItemPhoto(
            @Valid @RequestBody QmsCutRoundFqcItemPhotoSaveReqVO reqVO) {
        return success(qmsCutRoundFqcService.saveItemPhoto(reqVO));
    }

    @DeleteMapping("/submission-detail/item-photo/delete")
    @Operation(summary = "删除裁切成品检验项目照片")
    public CommonResult<Boolean> deleteItemPhoto(@RequestParam("id") Long id,
                                                  @RequestParam("photoId") Long photoId) {
        qmsCutRoundFqcService.deleteItemPhoto(id, photoId);
        return success(true);
    }

    @GetMapping("/defect-code-options")
    @Operation(summary = "获取裁切成品检验可选缺陷代码和发生原因")
    public CommonResult<List<QmsDefectCodeRespVO>> getDefectCodeOptions() {
        return success(qmsCutRoundFqcService.getDefectCodeOptions());
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存裁切成品检验项目录入")
    public CommonResult<QmsCutRoundFqcRespVO> saveProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsCutRoundFqcService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/start")
    @Operation(summary = "开始裁切成品检验")
    public CommonResult<QmsCutRoundFqcRespVO> startProgramEntry(@RequestParam("id") Long id) {
        return success(qmsCutRoundFqcService.startProgramEntry(id));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交裁切成品检验检测结果")
    public CommonResult<QmsCutRoundFqcRespVO> submitProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsCutRoundFqcService.submitProgramEntry(saveReqVO));
    }

    @PutMapping("/submission-detail/save")
    @Operation(summary = "保存裁切成品检验片级判定")
    public CommonResult<QmsCutRoundFqcRespVO> saveSubmissionDetails(
            @Valid @RequestBody QmsCutRoundFqcSubmissionDetailSaveReqVO reqVO) {
        return success(qmsCutRoundFqcService.saveSubmissionDetails(reqVO));
    }

    @PutMapping("/submission-detail/photos/save")
    @Operation(summary = "保存裁切成品检验片级照片")
    public CommonResult<QmsCutRoundFqcRespVO> saveSubmissionDetailPhotos(
            @Valid @RequestBody QmsCutRoundFqcSubmissionDetailPhotoSaveReqVO reqVO) {
        return success(qmsCutRoundFqcService.saveSubmissionDetailPhotos(reqVO));
    }

    @PutMapping("/program-entry/audit")
    @Operation(summary = "审核裁切成品检验片级放行")
    public CommonResult<QmsCutRoundFqcRespVO> audit(@Valid @RequestBody QmsFqcAuditReqVO auditReqVO) {
        return success(qmsCutRoundFqcService.audit(auditReqVO));
    }

    @PutMapping("/program-entry/revoke-audit")
    @Operation(summary = "撤销裁切成品检验审核并退回审核前")
    public CommonResult<QmsCutRoundFqcRespVO> revokeAudit(
            @Valid @RequestBody QmsFqcAuditRevokeReqVO revokeReqVO) {
        return success(qmsCutRoundFqcService.revokeAudit(revokeReqVO));
    }

    @PostMapping("/scan/resolve")
    @Operation(summary = "解析裁切成品检验扫码并定位送检明细")
    public CommonResult<QmsCutRoundFqcScanRespVO> resolveScan(@Valid @RequestBody QmsFqcScanReqVO scanReqVO) {
        return success(qmsCutRoundFqcService.resolveScan(scanReqVO));
    }
}
