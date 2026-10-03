package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiBindStandardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsGlueBoardFaiCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcProcessReportService;
import cn.iocoder.yudao.module.mes.service.qms.QmsFaiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_NOT_EXISTS;

@Tag(name = "管理后台 - 胶板检验")
@RestController
@RequestMapping("/mes/quality/glue-board-fai")
@Validated
public class QmsGlueBoardFaiController {

    private static final String SOURCE_MODULE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String DEFAULT_MACHINE_CODE = "MANUAL_GLUE_BOARD";
    private static final String DEFAULT_SUBMISSION_TYPE = "MASS_SHIPMENT";
    private static final String DEFAULT_TRIGGER_REASON = "NEW_ORDER";
    private static final String QUALITY_STATUS_NORMAL = "NORMAL";
    private static final String QUALITY_STATUS_WAITING = "WAITING";
    private static final String QUALITY_STATUS_ABNORMAL = "ABNORMAL";
    private static final String STATUS_PENDING = "PENDING";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String GLUE_BOARD_FAI_PROCESS_ADHESIVE1 = "GLUE_1";
    private static final String GLUE_BOARD_FAI_PROCESS_ADHESIVE2 = "GLUE_2";
    private static final String GLUE_BOARD_FAI_OPERATION_ADHESIVE1 = "粘胶1";
    private static final String GLUE_BOARD_FAI_OPERATION_ADHESIVE2 = "粘胶2";

    @Resource
    private QmsFaiService qmsFaiService;
    @Resource
    private HcAdhesiveGlueBoardStockMapper glueBoardStockMapper;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private HcProcessReportService hcProcessReportService;

    @PostMapping("/create")
    @Operation(summary = "创建胶板检验单")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> createGlueBoardFai(@Valid @RequestBody QmsGlueBoardFaiCreateReqVO createReqVO) {
        return success(createGlueBoardFaiInternal(createReqVO, false));
    }

    @PostMapping("/create-from-adhesive1")
    @Operation(summary = "粘胶1看板创建胶板检验单")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> createGlueBoardFaiFromAdhesive1(@Valid @RequestBody QmsGlueBoardFaiCreateReqVO createReqVO) {
        deductGlueBoardInspectionSample(createReqVO, null);
        Long id = createGlueBoardFaiInternal(createReqVO, true);
        return success(id);
    }

    @PostMapping("/create-from-adhesive2")
    @Operation(summary = "粘胶2看板创建胶板检验单")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> createGlueBoardFaiFromAdhesive2(@Valid @RequestBody QmsGlueBoardFaiCreateReqVO createReqVO) {
        deductGlueBoardInspectionSample(createReqVO, null);
        Long id = createGlueBoardFaiInternal(createReqVO, true);
        return success(id);
    }

    @PutMapping("/bind-standard")
    @Operation(summary = "为待检胶板任务选择检验标准")
    public CommonResult<QmsFaiRespVO> bindStandard(@Valid @RequestBody QmsFaiBindStandardReqVO bindReqVO) {
        validateGlueBoardFai(bindReqVO.getId());
        return success(qmsFaiService.bindStandard(bindReqVO));
    }

    @GetMapping("/standard-candidates")
    @Operation(summary = "获取胶板检验记录候选标准")
    public CommonResult<List<QmsInspectionStandardCandidateRespVO>> getStandardCandidates(
            @RequestParam("id") Long id) {
        validateGlueBoardFai(id);
        return success(qmsFaiService.getStandardCandidates(id));
    }

    @PutMapping("/select-standard")
    @Operation(summary = "为胶板检验记录挂接或切换标准")
    public CommonResult<QmsFaiRespVO> selectStandard(
            @Valid @RequestBody QmsInspectionStandardSelectReqVO reqVO) {
        validateGlueBoardFai(reqVO.getId());
        return success(qmsFaiService.selectStandard(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取胶板检验单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsFaiRespVO> getGlueBoardFai(@RequestParam("id") Long id) {
        validateGlueBoardFai(id);
        return success(qmsFaiService.getFaiResp(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取胶板检验单分页")
    public CommonResult<PageResult<QmsFaiRespVO>> getGlueBoardFaiPage(@Valid QmsFaiPageReqVO pageReqVO) {
        pageReqVO.setSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        PageResult<QmsFaiOrderDO> pageResult = qmsFaiService.getFaiPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsFaiRespVO.class));
    }

    @GetMapping("/pending-list")
    @Operation(summary = "获取胶板检验待检/检验中/挂起队列")
    public CommonResult<List<QmsFaiRespVO>> getPendingGlueBoardFaiList() {
        return success(qmsFaiService.getPendingFaiListBySourceModule(SOURCE_MODULE_GLUE_BOARD_FAI));
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存胶板检验程序化录入草稿")
    public CommonResult<QmsFaiRespVO> saveProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        normalizeGlueBoardSaveReq(saveReqVO);
        validateGlueBoardFai(saveReqVO.getId());
        return success(qmsFaiService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/recalculate")
    @Operation(summary = "重算胶板检验程序化录入判定")
    public CommonResult<QmsFaiRespVO> recalculateProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        normalizeGlueBoardSaveReq(saveReqVO);
        validateGlueBoardFai(saveReqVO.getId());
        return success(qmsFaiService.recalculateProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交胶板检验检测结果")
    public CommonResult<QmsFaiRespVO> submitProgramEntry(@RequestBody QmsFaiSaveReqVO saveReqVO) {
        normalizeGlueBoardSaveReq(saveReqVO);
        validateGlueBoardFai(saveReqVO.getId());
        return success(qmsFaiService.submitProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/audit")
    @Operation(summary = "整单审核胶板检验检测结果")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<QmsFaiRespVO> auditProgramEntry(@Valid @RequestBody QmsFaiAuditReqVO auditReqVO) {
        validateGlueBoardFai(auditReqVO.getId());
        QmsFaiRespVO respVO = qmsFaiService.auditProgramEntry(auditReqVO);
        syncGlueBoardStockInspectionResult(respVO.getId());
        return success(respVO);
    }

    @PutMapping("/one-click-pass")
    @Operation(summary = "一键合格胶板检验单")
    @Parameter(name = "id", description = "编号", required = true)
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<QmsFaiRespVO> oneClickPass(@RequestParam("id") Long id) {
        validateGlueBoardFai(id);
        QmsFaiRespVO respVO = qmsFaiService.oneClickPass(id);
        syncGlueBoardStockInspectionResult(respVO.getId());
        return success(respVO);
    }

    @GetMapping("/item-template/download")
    @Operation(summary = "下载胶板检验项明细导入模板")
    public void downloadItemTemplate(@RequestParam("id") Long id, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        validateGlueBoardFai(id);
        response.addHeader("Content-Disposition", "attachment;filename="
                + cn.iocoder.yudao.framework.common.util.http.HttpUtils.encodeUtf8("胶板检验项明细导入模板.xlsx"));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.getOutputStream().write(qmsFaiService.buildItemImportTemplateExcel(id));
    }

    @GetMapping("/item-export")
    @Operation(summary = "导出胶板检验项明细")
    public void exportItemValues(@RequestParam("id") Long id, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        validateGlueBoardFai(id);
        response.addHeader("Content-Disposition", "attachment;filename="
                + cn.iocoder.yudao.framework.common.util.http.HttpUtils.encodeUtf8("胶板检验项明细.xlsx"));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.getOutputStream().write(qmsFaiService.buildItemExportExcel(id));
    }

    private Long createGlueBoardFaiInternal(QmsGlueBoardFaiCreateReqVO createReqVO,
                                            boolean allowPendingStandard) {
        HcAdhesiveGlueBoardStockDO stock = validateGlueBoardStockCreateAllowed(createReqVO);
        QmsFaiSaveReqVO saveReqVO = buildCreateReqVO(createReqVO, stock);
        Long id = allowPendingStandard
                ? qmsFaiService.createFaiForInspectionPush(saveReqVO)
                : qmsFaiService.createFai(saveReqVO);
        QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(id);
        markGlueBoardStockSubmitted(stock, order);
        return id;
    }

    private void deductGlueBoardInspectionSample(QmsGlueBoardFaiCreateReqVO createReqVO, Long faiId) {
        QmsFaiOrderDO order = faiId == null ? null : qmsFaiOrderMapper.selectById(faiId);
        hcProcessReportService.deductAdhesiveGlueBoardInspectionSample(
                createReqVO.getGlueBoardUsageId(),
                order != null && order.getGlueBoardStockId() != null
                        ? order.getGlueBoardStockId()
                        : createReqVO.getGlueBoardStockId(),
                createReqVO.getGluePlateBatchNo(),
                createReqVO.getSampleStartPosition(),
                createReqVO.getSampleLength(),
                order == null ? createReqVO.getSubmissionTime() : order.getSubmissionTime());
    }

    private QmsFaiSaveReqVO buildCreateReqVO(QmsGlueBoardFaiCreateReqVO createReqVO,
                                             HcAdhesiveGlueBoardStockDO stock) {
        QmsFaiSaveReqVO saveReqVO = BeanUtils.toBean(createReqVO, QmsFaiSaveReqVO.class);
        String segmentBatchNo = defaultIfBlank(createReqVO.getSourceProductionBatchNo(), createReqVO.getSourceReportNo());
        saveReqVO.setPlanOrderId(createReqVO.getPlanOrderId());
        saveReqVO.setSourceReportNo(defaultIfBlank(segmentBatchNo, saveReqVO.getSourceReportNo()));
        saveReqVO.setGlueBoardStockId(stock.getId());
        saveReqVO.setGlueBoardModel(defaultIfBlank(stock.getGlueBoardModel(), createReqVO.getGlueBoardModel()));
        saveReqVO.setGluePlateBatchNo(defaultIfBlank(stock.getGlueBoardBatchNo(), createReqVO.getGluePlateBatchNo()));
        String glueBoardMaterialCode = defaultIfBlank(createReqVO.getGlueBoardMaterialCode(), stock.getGlueBoardMaterialCode());
        saveReqVO.setGlueBoardMaterialCode(glueBoardMaterialCode);
        saveReqVO.setMaterialCode(defaultIfBlank(glueBoardMaterialCode, stock.getGlueBoardMaterialCode()));
        saveReqVO.setMaterialName(defaultIfBlank(createReqVO.getGlueBoardMaterialName(),
                defaultIfBlank(stock.getGlueBoardMaterialName(), saveReqVO.getMaterialCode())));
        saveReqVO.setSpecification(defaultIfBlank(saveReqVO.getSpecification(), saveReqVO.getGlueBoardModel()));
        normalizeGlueBoardSaveReq(saveReqVO);
        saveReqVO.setId(null);
        saveReqVO.setWorkOrderNo(defaultIfBlank(createReqVO.getWorkOrderNo(), saveReqVO.getGluePlateBatchNo()));
        saveReqVO.setProductBatchNo(saveReqVO.getGluePlateBatchNo());
        saveReqVO.setSubmissionTime(createReqVO.getSubmissionTime() != null ? createReqVO.getSubmissionTime() : LocalDateTime.now());
        saveReqVO.setSubmissionType(DEFAULT_SUBMISSION_TYPE);
        saveReqVO.setTriggerReason(DEFAULT_TRIGGER_REASON);
        saveReqVO.setStatus(STATUS_PENDING);
        saveReqVO.setJudgment(JUDGMENT_PENDING);
        return saveReqVO;
    }

    private void normalizeGlueBoardSaveReq(QmsFaiSaveReqVO saveReqVO) {
        saveReqVO.setSourceModule(SOURCE_MODULE_GLUE_BOARD_FAI);
        saveReqVO.setProductModel(defaultIfBlank(saveReqVO.getProductModel(), saveReqVO.getGlueBoardModel()));
        saveReqVO.setProductBatchNo(defaultIfBlank(saveReqVO.getProductBatchNo(), saveReqVO.getGluePlateBatchNo()));
        saveReqVO.setSubmissionType(defaultIfBlank(saveReqVO.getSubmissionType(), DEFAULT_SUBMISSION_TYPE));
        saveReqVO.setTriggerReason(defaultIfBlank(saveReqVO.getTriggerReason(), DEFAULT_TRIGGER_REASON));
        saveReqVO.setMachineCode(defaultIfBlank(saveReqVO.getMachineCode(), DEFAULT_MACHINE_CODE));
        saveReqVO.setOperationCode(defaultIfBlank(saveReqVO.getOperationCode(), saveReqVO.getProcessCategory()));
        saveReqVO.setOperationName(defaultIfBlank(saveReqVO.getOperationName(), saveReqVO.getProcessCategory()));
    }

    private void markGlueBoardStockSubmitted(HcAdhesiveGlueBoardStockDO stock, QmsFaiOrderDO order) {
        HcAdhesiveGlueBoardStockDO update = HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId())
                .qualityStatus(QUALITY_STATUS_WAITING)
                .inspectionSubmitTime(order != null && order.getSubmissionTime() != null
                        ? order.getSubmissionTime()
                        : LocalDateTime.now())
                .latestInspectionId(order == null ? null : order.getId())
                .latestInspectionNo(order == null ? null : order.getFaiNo())
                .latestInspectionResult(JUDGMENT_PENDING)
                .build();
        glueBoardStockMapper.updateById(update);
    }

    private HcAdhesiveGlueBoardStockDO validateGlueBoardStockCreateAllowed(QmsGlueBoardFaiCreateReqVO createReqVO) {
        HcAdhesiveGlueBoardStockDO stock = selectGlueBoardStock(createReqVO);
        if (stock == null) {
            throw invalidParamException("未找到对应胶板边库记录，请先在胶板边库完成领用登记");
        }
        LocalDateTime submissionTime = createReqVO.getSubmissionTime() == null
                ? LocalDateTime.now()
                : createReqVO.getSubmissionTime();
        LocalDateTime dayStart = submissionTime.toLocalDate().atStartOfDay();
        String segmentBatchNo = defaultIfBlank(createReqVO.getSourceProductionBatchNo(), createReqVO.getSourceReportNo());
        QmsFaiOrderDO latestToday = qmsFaiOrderMapper.selectLatestGlueBoardFaiToday(
                createReqVO.getPlanOrderId(), segmentBatchNo,
                stock.getId(), stock.getGlueBoardBatchNo(), stock.getGlueBoardModel(),
                resolveGlueBoardFaiProcessCategory(createReqVO),
                resolveGlueBoardFaiOperationKeyword(createReqVO),
                dayStart, dayStart.plusDays(1));
        if (latestToday != null && !JUDGMENT_NG.equals(latestToday.getJudgment())) {
            throw invalidParamException("当前胶板今日已提交检验，请勿重复送检；如今日检验不合格可重新送检");
        }
        return stock;
    }

    private String resolveGlueBoardFaiProcessCategory(QmsGlueBoardFaiCreateReqVO createReqVO) {
        String processCategory = createReqVO.getProcessCategory();
        String operationCode = createReqVO.getOperationCode();
        String operationName = createReqVO.getOperationName();
        if (isGlueBoardFaiProcess(processCategory, operationCode, operationName,
                GLUE_BOARD_FAI_PROCESS_ADHESIVE2, GLUE_BOARD_FAI_OPERATION_ADHESIVE2)) {
            return GLUE_BOARD_FAI_PROCESS_ADHESIVE2;
        }
        if (isGlueBoardFaiProcess(processCategory, operationCode, operationName,
                GLUE_BOARD_FAI_PROCESS_ADHESIVE1, GLUE_BOARD_FAI_OPERATION_ADHESIVE1)) {
            return GLUE_BOARD_FAI_PROCESS_ADHESIVE1;
        }
        return processCategory;
    }

    private String resolveGlueBoardFaiOperationKeyword(QmsGlueBoardFaiCreateReqVO createReqVO) {
        String processCategory = resolveGlueBoardFaiProcessCategory(createReqVO);
        if (GLUE_BOARD_FAI_PROCESS_ADHESIVE2.equals(processCategory)) {
            return GLUE_BOARD_FAI_OPERATION_ADHESIVE2;
        }
        if (GLUE_BOARD_FAI_PROCESS_ADHESIVE1.equals(processCategory)) {
            return GLUE_BOARD_FAI_OPERATION_ADHESIVE1;
        }
        return createReqVO.getOperationName();
    }

    private boolean isGlueBoardFaiProcess(String processCategory, String operationCode, String operationName,
                                          String expectedProcess, String expectedName) {
        String operationNameText = StringUtils.trimWhitespace(operationName);
        return expectedProcess.equalsIgnoreCase(StringUtils.trimWhitespace(processCategory))
                || expectedProcess.equalsIgnoreCase(StringUtils.trimWhitespace(operationCode))
                || (operationNameText != null && operationNameText.contains(expectedName));
    }

    private HcAdhesiveGlueBoardStockDO selectGlueBoardStock(QmsGlueBoardFaiCreateReqVO createReqVO) {
        if (createReqVO.getGlueBoardStockId() == null) {
            return null;
        }
        HcAdhesiveGlueBoardStockDO stock = glueBoardStockMapper.selectById(createReqVO.getGlueBoardStockId());
        return isGlueBoardStock(stock) ? stock : null;
    }

    private boolean isGlueBoardStock(HcAdhesiveGlueBoardStockDO stock) {
        return stock != null
                && !Boolean.TRUE.equals(stock.getDeleted())
                && (!StringUtils.hasText(stock.getAccessoryCategory())
                || HcAdhesiveGlueBoardStockMapper.ACCESSORY_CATEGORY_GLUE_BOARD.equals(stock.getAccessoryCategory()));
    }

    private void syncGlueBoardStockInspectionResult(Long faiId) {
        QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(faiId);
        if (order == null || !SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())) {
            return;
        }
        HcAdhesiveGlueBoardStockDO stock = glueBoardStockMapper.selectByLatestInspectionId(faiId);
        if (stock == null && order.getGlueBoardStockId() != null) {
            HcAdhesiveGlueBoardStockDO candidate = glueBoardStockMapper.selectById(order.getGlueBoardStockId());
            if (candidate != null
                    && !Boolean.TRUE.equals(candidate.getDeleted())
                    && (candidate.getLatestInspectionId() == null
                    || Objects.equals(candidate.getLatestInspectionId(), faiId))) {
                stock = candidate;
            }
        }
        if (stock == null) {
            return;
        }
        HcAdhesiveGlueBoardStockDO update = HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId())
                .qualityStatus(resolveGlueBoardQualityStatus(order.getJudgment()))
                .inspectionSubmitTime(order.getSubmissionTime())
                .latestInspectionId(order.getId())
                .latestInspectionNo(order.getFaiNo())
                .latestInspectionResult(defaultIfBlank(order.getJudgment(), JUDGMENT_PENDING))
                .build();
        glueBoardStockMapper.updateById(update);
        if (JUDGMENT_NG.equals(order.getJudgment())) {
            hcProcessReportService.markAdhesiveReportsAbnormalByGlueBoardInspection(
                    stock.getId(), order.getSubmissionTime(),
                    "胶板检验不合格" + (StringUtils.hasText(order.getFaiNo()) ? "：" + order.getFaiNo() : ""));
        } else if (JUDGMENT_OK.equals(order.getJudgment())) {
            hcProcessReportService.releaseAdhesiveReportsAbnormalByGlueBoardInspection(stock.getId());
        }
    }

    private String resolveGlueBoardQualityStatus(String judgment) {
        if (JUDGMENT_OK.equals(judgment)) {
            return QUALITY_STATUS_NORMAL;
        }
        if (JUDGMENT_NG.equals(judgment)) {
            return QUALITY_STATUS_ABNORMAL;
        }
        return QUALITY_STATUS_WAITING;
    }

    private void validateGlueBoardFai(Long id) {
        QmsFaiOrderDO order = qmsFaiService.getFai(id);
        if (order == null || !SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())) {
            throw exception(HCFAI_NOT_EXISTS);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
