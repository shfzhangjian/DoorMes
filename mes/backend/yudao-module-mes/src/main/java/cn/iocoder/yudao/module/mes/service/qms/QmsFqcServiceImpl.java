package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSheetTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcReturnRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDefectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcScanRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetFieldDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetSectionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.service.hc.packagingevent.HcPackagingPieceEventLogService;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCauseMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcReturnRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleDefectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcScanRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSheetFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSheetSectionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSheetTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_FINISHED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_ITEMS_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_ITEM_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_SAMPLE_DEFECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_SAMPLE_DEFECT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_STANDARD_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFQC_STANDARD_NOT_EXISTS;

@Service
@Validated
public class QmsFqcServiceImpl implements QmsFqcService {

    private static final String APPLY_TYPE_FQC = "FQC";
    private static final Integer ENABLED = 1;
    private static final Integer AUDITED = 20;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_WAITING_QA = "WAITING_QA";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String PROCESS_PENDING = "PENDING";
    private static final String ALLOW_INBOUND = "ALLOW_INBOUND";
    private static final String FREEZE_BATCH = "FREEZE_BATCH";
    private static final String ENTRY_LAYOUT_PROGRAM_FORM = "PROGRAM_FORM";
    private static final String ENTRY_LAYOUT_SHEET_GRID = "SHEET_GRID";
    private static final String ENTRY_MODE_MANUAL = "MANUAL";
    private static final String ENTRY_MODE_EXCEL_IMPORT = "EXCEL_IMPORT";
    private static final String ENTRY_MODE_HISTORICAL_IMPORT = "HISTORICAL_IMPORT";
    private static final String INPUT_STATUS_EMPTY = "EMPTY";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final String VALUE_SOURCE_MANUAL = "MANUAL";
    private static final String ROLE_QA = "QA";
    private static final String AUDIT_PASS = "PASS";
    private static final String AUDIT_REJECT = "REJECT";
    private static final String AUDIT_FAIL = "FAIL";
    private static final String SCAN_TARGET_FQC_NO = "FQC_NO";
    private static final String SCAN_TARGET_WORK_ORDER_NO = "WORK_ORDER_NO";
    private static final String SCAN_TARGET_PRODUCT_BATCH_NO = "PRODUCT_BATCH_NO";
    private static final String SCAN_TARGET_CUT_ROUND_BATCH_NO = "CUT_ROUND_BATCH_NO";
    private static final String SCAN_TARGET_MATERIAL_CODE = "MATERIAL_CODE";
    private static final String SCAN_TARGET_UNKNOWN = "UNKNOWN";
    private static final String SCAN_RESULT_MATCHED_SINGLE = "MATCHED_SINGLE";
    private static final String SCAN_RESULT_MATCHED_MULTIPLE = "MATCHED_MULTIPLE";
    private static final String SCAN_RESULT_NOT_FOUND = "NOT_FOUND";
    private static final String OPEN_TARGET_WORKBENCH = "WORKBENCH";
    private static final String OPEN_TARGET_CANDIDATE_MODAL = "CANDIDATE_MODAL";
    private static final String TEMPLATE_STATUS_ENABLE = "ENABLE";
    private static final String DEFECT_TYPE_ITEM = "ITEM";
    private static final String SOURCE_MODULE_CUT_ROUND = "CUT_ROUND";
    private static final String CUT_ROUND_INSPECTION_STATUS_INSPECTING = "INSPECTING";
    private static final String CUT_ROUND_INSPECTION_STATUS_COMPLETED = "COMPLETED";
    private static final String CUT_ROUND_TASK_STATUS_INSPECTING = "INSPECTING";
    private static final String CUT_ROUND_TASK_STATUS_WAITING_QA = "WAITING_QA";
    private static final String CUT_ROUND_TASK_STATUS_COMPLETED = "COMPLETED";
    private static final String CUT_ROUND_TASK_STATUS_REJECTED = "REJECTED";
    private static final String CUT_ROUND_TASK_STATUS_PARTIAL_NG = "PARTIAL_NG";

    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Resource
    private QmsFqcItemMapper qmsFqcItemMapper;
    @Resource
    private QmsFqcSampleMapper qmsFqcSampleMapper;
    @Resource
    private QmsFqcSampleDefectMapper qmsFqcSampleDefectMapper;
    @Resource
    private QmsFqcAbnormalMapper qmsFqcAbnormalMapper;
    @Resource
    private QmsDefectCodeMapper qmsDefectCodeMapper;
    @Resource
    private QmsDefectCauseMapper qmsDefectCauseMapper;
    @Resource
    private QmsFqcReturnRecordMapper qmsFqcReturnRecordMapper;
    @Resource
    private QmsFqcScanRecordMapper qmsFqcScanRecordMapper;
    @Resource
    private QmsFqcSheetTemplateMapper qmsFqcSheetTemplateMapper;
    @Resource
    private QmsFqcSheetSectionMapper qmsFqcSheetSectionMapper;
    @Resource
    private QmsFqcSheetFieldMapper qmsFqcSheetFieldMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    private QmsAuditTodoNotifyService qmsAuditTodoNotifyService;
    @Resource
    private QmsAbnormalLockService qmsAbnormalLockService;
    @Resource
    private HcCutRoundInspectionDetailMapper hcCutRoundInspectionDetailMapper;
    @Resource
    private HcCutRoundInspectionTaskMapper hcCutRoundInspectionTaskMapper;
    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private HcPackagingPieceEventLogService hcPackagingPieceEventLogService;
    @Resource
    private QmsFqcItemWorkbookService qmsFqcItemWorkbookService;
    @Resource
    private QmsFqcItemImportApplyService qmsFqcItemImportApplyService;
    @Resource
    private QmsNoGeneratorService qmsNoGeneratorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFqc(QmsFqcSaveReqVO createReqVO) {
        String fqcNo = generateFqcNo();
        validateFqcNoUnique(null, fqcNo);
        QmsQualityStandardDO standard = selectFqcStandard(createReqVO);
        List<QmsQualityStandardItemDO> standardItems = selectStandardItems(standard.getId());
        QmsFqcOrderDO entity = BeanUtils.toBean(createReqVO, QmsFqcOrderDO.class);
        entity.setId(null);
        entity.setFqcNo(fqcNo);
        applyStandardSnapshot(entity, standard);
        entity.setStatus(defaultIfBlank(entity.getStatus(), STATUS_PENDING));
        entity.setJudgment(defaultIfBlank(entity.getJudgment(), JUDGMENT_PENDING));
        qmsFqcOrderMapper.insert(entity);
        saveStandardSnapshotDetails(entity, standardItems, createReqVO.getAbnormals());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFqc(QmsFqcSaveReqVO updateReqVO) {
        QmsFqcOrderDO old = validateFqcExists(updateReqVO.getId());
        validateEditable(old);
        QmsFqcOrderDO updateObj = BeanUtils.toBean(updateReqVO, QmsFqcOrderDO.class);
        updateObj.setFqcNo(old.getFqcNo());
        updateObj.setStatus(defaultIfBlank(updateReqVO.getStatus(), old.getStatus()));
        updateObj.setJudgment(defaultIfBlank(updateReqVO.getJudgment(), old.getJudgment()));
        qmsFqcOrderMapper.updateById(updateObj);
        if (updateReqVO.getItems() != null && !updateReqVO.getItems().isEmpty()) {
            updateExecutionDetails(validateFqcExists(updateReqVO.getId()), updateReqVO.getItems(), updateReqVO.getAbnormals());
        }
        syncCutRoundInspectionFromFqc(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFqc(QmsFqcSaveReqVO submitReqVO) {
        QmsFqcOrderDO entity = validateFqcExists(submitReqVO.getId());
        validateEditable(entity);
        updateExecutionDetails(entity, submitReqVO.getItems(), submitReqVO.getAbnormals());
        calculateOrderJudgment(qmsFqcItemMapper.selectListByFqcId(entity.getId()));
        LocalDateTime now = LocalDateTime.now();

        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(entity.getId());
        updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setInspectorName(resolveLoginUserName());
        updateObj.setInspectionTime(now);
        updateObj.setRemark(StringUtils.hasText(submitReqVO.getRemark()) ? submitReqVO.getRemark() : entity.getRemark());
        updateObj.setStatus(STATUS_WAITING_QA);
        updateObj.setJudgment(JUDGMENT_PENDING);
        updateObj.setReleaseResult(null);
        updateObj.setSheetLocked(true);
        updateObj.setSubmissionTime(now);
        updateObj.setSubmitterName(resolveLoginUserName());
        qmsFqcOrderMapper.updateById(updateObj);
        syncCutRoundInspectionFromFqc(entity.getId());
        qmsAuditTodoNotifyService.sendAuditTodoIfNeeded(entity.getAuditNotifyTime(),
                "成品检验(FQC)",
                entity.getFqcNo(),
                buildFqcAuditBizName(entity),
                "请进入质量管理-成品检验(FQC)，使用整单审核按钮完成审核。",
                now,
                time -> qmsFqcOrderMapper.updateAuditNotifyTime(entity.getId(), time));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendFqc(Long id) {
        QmsFqcOrderDO entity = validateFqcExists(id);
        validateEditable(entity);
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SUSPENDED);
        qmsFqcOrderMapper.updateById(updateObj);
        syncCutRoundInspectionFromFqc(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFqc(Long id) {
        QmsFqcOrderDO entity = validateFqcExists(id);
        validateEditable(entity);
        qmsFqcSampleDefectMapper.deleteByFqcId(id);
        qmsFqcSampleMapper.deleteByFqcId(id);
        qmsFqcAbnormalMapper.deleteByFqcId(id);
        qmsFqcItemMapper.deleteByFqcId(id);
        qmsFqcOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFqcList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> validateEditable(validateFqcExists(id)));
        qmsFqcSampleDefectMapper.deleteByFqcIds(ids);
        qmsFqcSampleMapper.deleteByFqcIds(ids);
        qmsFqcAbnormalMapper.deleteByFqcIds(ids);
        qmsFqcItemMapper.deleteByFqcIds(ids);
        qmsFqcOrderMapper.deleteBatchIds(ids);
    }

    @Override
    public QmsFqcRespVO getFqcResp(Long id) {
        QmsFqcOrderDO entity = validateFqcExists(id);
        QmsFqcRespVO respVO = BeanUtils.toBean(entity, QmsFqcRespVO.class);
        respVO.setOriginalInspectionNo(respVO.getRejectRootInspectionNo() != null
                ? respVO.getRejectRootInspectionNo() : respVO.getRejectPrevInspectionNo());
        fillDetails(respVO);
        return respVO;
    }

    @Override
    public PageResult<QmsFqcOrderDO> getFqcPage(QmsFqcPageReqVO pageReqVO) {
        return qmsFqcOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<QmsFqcRespVO> getPendingFqcList() {
        List<QmsFqcOrderDO> list = qmsFqcOrderMapper.selectListByStatuses(List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_WAITING_QA, STATUS_SUSPENDED));
        return BeanUtils.toBean(list, QmsFqcRespVO.class);
    }

    @Override
    public List<QmsDefectCodeRespVO> getFqcDefectCodeOptions() {
        List<QmsDefectCodeDO> list = qmsDefectCodeMapper.selectList(new LambdaQueryWrapperX<QmsDefectCodeDO>()
                .eq(QmsDefectCodeDO::getType, DEFECT_TYPE_ITEM)
                .eq(QmsDefectCodeDO::getStatus, ENABLED)
                .orderByAsc(QmsDefectCodeDO::getParentId)
                .orderByAsc(QmsDefectCodeDO::getSort)
                .orderByAsc(QmsDefectCodeDO::getId));
        List<QmsDefectCodeRespVO> result = BeanUtils.toBean(list, QmsDefectCodeRespVO.class);
        for (QmsDefectCodeRespVO item : result) {
            item.setCauses(BeanUtils.toBean(
                    qmsDefectCauseMapper.selectListByDefectCodeId(item.getId()),
                    QmsDefectCodeRespVO.Cause.class));
        }
        return result;
    }

    @Override
    public QmsFqcStandardRespVO getFqcStandard(String materialCode) {
        QmsFqcSaveReqVO reqVO = new QmsFqcSaveReqVO();
        reqVO.setMaterialCode(materialCode);
        QmsQualityStandardDO standard = selectFqcStandard(reqVO);
        List<QmsQualityStandardItemDO> standardItems = selectStandardItems(standard.getId());
        QmsFqcStandardRespVO respVO = BeanUtils.toBean(standard, QmsFqcStandardRespVO.class);
        respVO.setStandardId(standard.getId());
        respVO.setItems(standardItems.stream().map(this::buildStandardItem).collect(Collectors.toList()));
        return respVO;
    }

    @Override
    public QmsFqcScanRespVO resolveScan(QmsFqcScanReqVO scanReqVO) {
        String scanCode = scanReqVO.getScanCode() == null ? "" : scanReqVO.getScanCode().trim();
        String scene = defaultIfBlank(scanReqVO.getScanScene(), "LEDGER_TOOLBAR");
        LocalDateTime now = LocalDateTime.now();
        List<QmsFqcOrderDO> candidates = qmsFqcOrderMapper.selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .and(wrapper -> wrapper
                        .eq(QmsFqcOrderDO::getFqcNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getWorkOrderNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getSourceReportNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getProductBatchNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getBatchNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getMaterialCode, scanCode))
                .orderByDesc(QmsFqcOrderDO::getId));
        QmsFqcScanRespVO respVO = new QmsFqcScanRespVO();
        respVO.setScanCode(scanCode);
        respVO.setScanScene(scene);
        respVO.setScanTime(now);
        respVO.setCandidateCount(candidates.size());
        respVO.setMatchResult(candidates.isEmpty() ? SCAN_RESULT_NOT_FOUND : candidates.size() == 1 ? SCAN_RESULT_MATCHED_SINGLE : SCAN_RESULT_MATCHED_MULTIPLE);
        respVO.setOpenTarget(candidates.size() <= 1 ? OPEN_TARGET_WORKBENCH : OPEN_TARGET_CANDIDATE_MODAL);
        respVO.setScanTargetType(resolveScanTargetType(scanCode, candidates));
        if (candidates.isEmpty()) {
            respVO.setMessage("未找到匹配的 FQC 成品检验单");
        } else if (candidates.size() == 1) {
            QmsFqcOrderDO matched = candidates.get(0);
            respVO.setMatchedFqcId(matched.getId());
            respVO.setMatchedFqcNo(matched.getFqcNo());
            respVO.setRecord(getFqcResp(matched.getId()));
            recordScanHit(matched, scanCode, scene, respVO.getScanTargetType(), now);
        } else {
            respVO.setCandidates(candidates.stream().map(this::buildScanCandidate).collect(Collectors.toList()));
        }
        QmsFqcScanRecordDO scanRecord = QmsFqcScanRecordDO.builder()
                .scanCode(scanCode)
                .scanTargetType(respVO.getScanTargetType())
                .scanScene(scene)
                .matchResult(respVO.getMatchResult())
                .matchedFqcId(respVO.getMatchedFqcId())
                .matchedFqcNo(respVO.getMatchedFqcNo())
                .candidateCount(respVO.getCandidateCount())
                .openTarget(respVO.getOpenTarget())
                .scanUserId(SecurityFrameworkUtils.getLoginUserId())
                .scanUserName(resolveLoginUserName())
                .scanTime(now)
                .clientType(scanReqVO.getClientType())
                .terminalCode(scanReqVO.getTerminalCode())
                .build();
        qmsFqcScanRecordMapper.insert(scanRecord);
        respVO.setScanRecordId(scanRecord.getId());
        return respVO;
    }

    @Override
    public List<QmsFqcSheetTemplateRespVO> getSheetTemplateList(String productModel) {
        return BeanUtils.toBean(qmsFqcSheetTemplateMapper.selectEnableList(productModel), QmsFqcSheetTemplateRespVO.class);
    }

    @Override
    public QmsFqcSheetTemplateRespVO getSheetTemplate(Long id) {
        QmsFqcSheetTemplateDO template = validateSheetTemplate(id);
        QmsFqcSheetTemplateRespVO respVO = BeanUtils.toBean(template, QmsFqcSheetTemplateRespVO.class);
        fillTemplateSections(respVO);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO selectSheetTemplate(Long id, Long templateId) {
        QmsFqcOrderDO order = validateFqcExists(id);
        validateEntryEditable(order);
        QmsFqcSheetTemplateDO template = validateSheetTemplate(templateId);
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(id);
        updateObj.setSheetTemplateId(template.getId());
        updateObj.setSheetTemplateCode(template.getTemplateCode());
        updateObj.setSheetTemplateName(template.getTemplateName());
        updateObj.setSheetTemplateVersion(template.getTemplateVersion());
        updateObj.setEntryLayout(ENTRY_LAYOUT_SHEET_GRID);
        updateObj.setEntryMode(ENTRY_MODE_MANUAL);
        updateObj.setStatus(STATUS_INSPECTING);
        updateObj.setLastSaveTime(LocalDateTime.now());
        updateObj.setSheetLocked(false);
        qmsFqcOrderMapper.updateById(updateObj);
        syncCutRoundInspectionFromFqc(id);
        return getFqcResp(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO saveSheetEntry(QmsFqcSaveReqVO saveReqVO) {
        return saveEntryDraft(saveReqVO, ENTRY_LAYOUT_SHEET_GRID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO saveProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        return saveEntryDraft(saveReqVO, ENTRY_LAYOUT_PROGRAM_FORM);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO recalculateProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcRespVO respVO = saveEntryDraft(saveReqVO, ENTRY_LAYOUT_PROGRAM_FORM);
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(saveReqVO.getId());
        updateObj.setLastCalculateTime(LocalDateTime.now());
        qmsFqcOrderMapper.updateById(updateObj);
        return getFqcResp(saveReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO auditProgramEntryItem(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcRespVO respVO = saveEntryDraft(saveReqVO, ENTRY_LAYOUT_PROGRAM_FORM);
        LocalDateTime now = LocalDateTime.now();
        qmsFqcItemMapper.selectListByFqcId(saveReqVO.getId()).forEach(item -> {
            if (StringUtils.hasText(item.getItemResult()) && !JUDGMENT_PENDING.equals(item.getItemResult())) {
                QmsFqcItemDO updateItem = new QmsFqcItemDO();
                updateItem.setId(item.getId());
                updateItem.setQaResult(item.getItemResult());
                updateItem.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
                updateItem.setQaInspectorName(resolveLoginUserName());
                updateItem.setQaTime(now);
                qmsFqcItemMapper.updateById(updateItem);
            }
        });
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO submitProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        saveEntryDraft(saveReqVO, ENTRY_LAYOUT_PROGRAM_FORM);
        QmsFqcOrderDO order = validateFqcExists(saveReqVO.getId());
        calculateOrderJudgment(qmsFqcItemMapper.selectListByFqcId(order.getId()));
        QmsFqcOrderDO updateObj = buildProgressUpdate(order.getId(), qmsFqcItemMapper.selectListByFqcId(order.getId()));
        updateObj.setStatus(STATUS_WAITING_QA);
        updateObj.setJudgment(JUDGMENT_PENDING);
        updateObj.setSheetLocked(true);
        updateObj.setReleaseResult(null);
        LocalDateTime submitTime = LocalDateTime.now();
        updateObj.setSubmissionTime(submitTime);
        updateObj.setSubmitterName(resolveLoginUserName());
        qmsFqcOrderMapper.updateById(updateObj);
        syncCutRoundInspectionFromFqc(order.getId());
        qmsAuditTodoNotifyService.sendAuditTodoIfNeeded(order.getAuditNotifyTime(),
                "成品检验(FQC)",
                order.getFqcNo(),
                buildFqcAuditBizName(order),
                "请进入质量管理-成品检验(FQC)，使用整单审核按钮完成审核。",
                submitTime,
                time -> qmsFqcOrderMapper.updateAuditNotifyTime(order.getId(), time));
        return getFqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO auditProgramEntry(QmsFqcAuditReqVO auditReqVO) {
        QmsFqcOrderDO order = validateFqcExists(auditReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus())) {
            throw exception(HCFQC_ITEMS_NOT_COMPLETED);
        }
        String auditResult = auditReqVO.getAuditResult();
        if (AUDIT_REJECT.equals(auditResult)) {
            QmsFqcSaveReqVO returnReqVO = new QmsFqcSaveReqVO();
            returnReqVO.setId(order.getId());
            returnReqVO.setReturnReason(auditReqVO.getRejectReason());
            return returnProgramEntry(returnReqVO);
        }
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(order.getId());
        String finalJudgment = AUDIT_FAIL.equals(auditResult) ? JUDGMENT_NG : calculateOrderJudgment(items);
        LocalDateTime now = LocalDateTime.now();
        QmsFqcOrderDO updateObj = buildProgressUpdate(order.getId(), items);
        updateObj.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setQaInspectorName(resolveLoginUserName());
        updateObj.setQaTime(now);
        updateObj.setReleaseTime(now);
        updateObj.setSheetLocked(true);
        if (JUDGMENT_OK.equals(finalJudgment) && AUDIT_PASS.equals(auditResult)) {
            updateObj.setStatus(STATUS_COMPLETED);
            updateObj.setJudgment(JUDGMENT_OK);
            updateObj.setReleaseResult(ALLOW_INBOUND);
        } else {
            updateObj.setStatus(STATUS_REJECTED);
            updateObj.setJudgment(JUDGMENT_NG);
            updateObj.setReleaseResult(FREEZE_BATCH);
            updateObj.setRelatedNcrNo(defaultIfBlank(order.getRelatedNcrNo(), generateNcrNo()));
            updateObj.setNcrStatus(defaultIfBlank(order.getNcrStatus(), "APPROVING"));
        }
        qmsFqcOrderMapper.updateById(updateObj);
        qmsFqcOrderMapper.clearAuditNotifyTime(order.getId());
        if (JUDGMENT_NG.equals(updateObj.getJudgment())) {
            ensureNgAbnormal(order.getId(), order.getFqcNo(), items, updateObj.getRelatedNcrNo());
            qmsAbnormalLockService.syncFromFqc(order.getId());
        }
        syncCutRoundInspectionFromFqc(order.getId());
        return getFqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcRespVO returnProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcOrderDO order = validateFqcExists(saveReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus())) {
            throw exception(HCFQC_ITEMS_NOT_COMPLETED);
        }
        String reason = defaultIfBlank(saveReqVO.getReturnReason(), saveReqVO.getRemark());
        if (!StringUtils.hasText(reason)) {
            reason = "FQC 审核退回修改";
        }
        LocalDateTime now = LocalDateTime.now();
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(order.getId());
        updateObj.setStatus(STATUS_INSPECTING);
        updateObj.setJudgment(JUDGMENT_PENDING);
        updateObj.setSheetLocked(false);
        updateObj.setReturnCount(order.getReturnCount() == null ? 1 : order.getReturnCount() + 1);
        updateObj.setLastReturnReason(reason);
        updateObj.setLastSaveTime(now);
        qmsFqcOrderMapper.updateById(updateObj);
        qmsFqcOrderMapper.clearAuditNotifyTime(order.getId());
        syncCutRoundInspectionFromFqc(order.getId());
        qmsFqcReturnRecordMapper.insert(QmsFqcReturnRecordDO.builder()
                .fqcId(order.getId())
                .fqcNo(order.getFqcNo())
                .returnStepCode("PROGRAM_ENTRY")
                .returnReason(reason)
                .returnUserId(SecurityFrameworkUtils.getLoginUserId())
                .returnUserName(resolveLoginUserName())
                .returnTime(now)
                .beforeStatus(order.getStatus())
                .afterStatus(STATUS_INSPECTING)
                .build());
        return getFqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcImportRespVO importOriginSheet(Long id, Long templateId, String mode, MultipartFile file) throws IOException {
        selectSheetTemplate(id, templateId);
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(id);
        updateObj.setEntryMode(ENTRY_MODE_HISTORICAL_IMPORT);
        updateObj.setEntryLayout(ENTRY_LAYOUT_SHEET_GRID);
        updateObj.setHistoricalBackfill(true);
        updateObj.setLastImportBatchNo(generateImportBatchNo());
        updateObj.setLastSaveTime(LocalDateTime.now());
        qmsFqcOrderMapper.updateById(updateObj);
        return buildImportResp(id, file, false, "历史原始记录表已登记到 FQC 独立导入批次，单元格数据请在固定表格录入页核对后保存。");
    }

    @Override
    public List<QmsFqcItemImportExcelVO> buildItemImportTemplate(Long id) {
        QmsFqcOrderDO order = validateFqcExists(id);
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(id);
        validateItemsExist(items);
        String templateVersionHash = qmsFqcItemWorkbookService.buildItemTemplateVersionHash(order, items);
        return qmsFqcItemWorkbookService.buildItemTemplateRows(order, items, templateVersionHash, Collections.emptyMap());
    }

    @Override
    public byte[] buildItemImportTemplateExcel(Long id) throws IOException {
        QmsFqcOrderDO order = validateFqcExists(id);
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(id);
        validateItemsExist(items);
        return qmsFqcItemWorkbookService.buildItemOverviewWorkbook(order, items, Collections.emptyMap());
    }

    @Override
    public List<QmsFqcItemImportExcelVO> buildItemExportRows(Long id) {
        QmsFqcOrderDO order = validateFqcExists(id);
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(id);
        validateItemsExist(items);
        List<Long> itemIds = items.stream().map(QmsFqcItemDO::getId).collect(Collectors.toList());
        Map<String, QmsFqcSampleDO> sampleMap = qmsFqcSampleMapper
                .selectListByFqcIdAndItemIdsAndRole(id, itemIds, ROLE_QA)
                .stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        String templateVersionHash = qmsFqcItemWorkbookService.buildItemTemplateVersionHash(order, items);
        return qmsFqcItemWorkbookService.buildItemTemplateRows(order, items, templateVersionHash, sampleMap);
    }

    @Override
    public byte[] buildItemExportExcel(Long id) throws IOException {
        QmsFqcOrderDO order = validateFqcExists(id);
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(id);
        validateItemsExist(items);
        List<Long> itemIds = items.stream().map(QmsFqcItemDO::getId).collect(Collectors.toList());
        Map<String, QmsFqcSampleDO> sampleMap = qmsFqcSampleMapper
                .selectListByFqcIdAndItemIdsAndRole(id, itemIds, ROLE_QA)
                .stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        return qmsFqcItemWorkbookService.buildItemOverviewWorkbook(order, items, sampleMap);
    }

    @Override
    public QmsFqcImportRespVO previewItemImport(Long id, MultipartFile file) throws IOException {
        QmsFqcItemWorkbookService.ItemImportPlan plan = buildItemImportPlan(id, file, false, true);
        return qmsFqcItemWorkbookService.buildItemImportResp(plan, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcImportRespVO confirmItemImport(Long id, Boolean allowOverwrite, MultipartFile file) throws IOException {
        QmsFqcItemWorkbookService.ItemImportPlan plan = buildItemImportPlan(id, file,
                Boolean.TRUE.equals(allowOverwrite), false);
        String importBatchNo = generateImportBatchNo();
        boolean applied = qmsFqcItemImportApplyService.applyItemImport(plan, importBatchNo);
        QmsFqcImportRespVO respVO = qmsFqcItemWorkbookService.buildItemImportResp(
                plan, applied ? getFqcResp(plan.order.getId()) : null);
        respVO.setImportBatchNo(importBatchNo);
        return respVO;
    }

    private QmsFqcItemWorkbookService.ItemImportPlan buildItemImportPlan(Long id, MultipartFile file,
                                                                         boolean allowOverwrite,
                                                                         boolean previewOnly) throws IOException {
        QmsFqcOrderDO order = validateFqcExists(id);
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(id);
        validateItemsExist(items);
        List<Long> itemIds = items.stream().map(QmsFqcItemDO::getId).collect(Collectors.toList());
        Map<String, QmsFqcSampleDO> existingSampleMap = qmsFqcSampleMapper
                .selectListByFqcIdAndItemIdsAndRole(id, itemIds, ROLE_QA)
                .stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        boolean importBlocked = Boolean.TRUE.equals(order.getSheetLocked())
                || STATUS_WAITING_QA.equals(order.getStatus())
                || STATUS_COMPLETED.equals(order.getStatus())
                || STATUS_REJECTED.equals(order.getStatus())
                || STATUS_CANCELED.equals(order.getStatus());
        return qmsFqcItemWorkbookService.buildItemImportPlan(
                order, items, existingSampleMap, file, allowOverwrite, previewOnly, importBlocked);
    }

    private QmsFqcRespVO saveEntryDraft(QmsFqcSaveReqVO saveReqVO, String entryLayout) {
        QmsFqcOrderDO order = validateFqcExists(saveReqVO.getId());
        validateEntryEditable(order);
        boolean hasSavedSampleValue = hasFilledSampleValue(saveReqVO.getItems());
        if (saveReqVO.getItems() != null && !saveReqVO.getItems().isEmpty()) {
            updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        }
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(order.getId());
        QmsFqcOrderDO updateObj = buildProgressUpdate(order.getId(), items);
        LocalDateTime now = LocalDateTime.now();
        updateObj.setStatus(STATUS_INSPECTING);
        updateObj.setEntryLayout(entryLayout);
        updateObj.setEntryMode(defaultIfBlank(saveReqVO.getEntryMode(), ENTRY_MODE_MANUAL));
        updateObj.setCurrentStepCode(defaultIfBlank(saveReqVO.getCurrentStepCode(), order.getCurrentStepCode()));
        updateObj.setSheetLocked(false);
        updateObj.setLastSaveTime(now);
        if (hasSavedSampleValue) {
            updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setInspectorName(resolveLoginUserName());
            updateObj.setInspectionTime(now);
        }
        qmsFqcOrderMapper.updateById(updateObj);
        syncCutRoundInspectionFromFqc(order.getId());
        return getFqcResp(order.getId());
    }

    private boolean hasFilledSampleValue(List<QmsFqcSaveReqVO.FqcItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(QmsFqcSaveReqVO.FqcItem::getSamples)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .anyMatch(sample ->
                        sample.getMeasuredValue() != null
                                || sample.getResultValue() != null
                                || sample.getDensityValue() != null
                                || sample.getCompressionRate() != null
                                || sample.getCompressionElasticityRate() != null
                                || hasMeaningfulSampleText(sample.getRawValuesJson())
                                || hasMeaningfulSampleText(sample.getQualitativeValue())
                                || JUDGMENT_OK.equals(sample.getSampleResult())
                                || JUDGMENT_NG.equals(sample.getSampleResult()));
    }

    private boolean hasMeaningfulSampleText(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return !"-".equals(trimmed) && !"{}".equals(trimmed) && !"[]".equals(trimmed);
    }

    private QmsFqcOrderDO buildProgressUpdate(Long fqcId, List<QmsFqcItemDO> items) {
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(fqcId);
        int required = items == null ? 0 : items.size();
        int completed = items == null ? 0 : (int) items.stream().filter(item -> JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult())).count();
        int abnormal = items == null ? 0 : (int) items.stream().filter(item -> JUDGMENT_NG.equals(item.getItemResult())).count();
        updateObj.setRequiredItemCount(required);
        updateObj.setCompletedItemCount(completed);
        updateObj.setAbnormalItemCount(abnormal);
        updateObj.setEntryProgress(required == 0 ? 0 : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(required), 0, RoundingMode.DOWN).intValue());
        return updateObj;
    }

    private QmsFqcSheetTemplateDO validateSheetTemplate(Long id) {
        QmsFqcSheetTemplateDO template = qmsFqcSheetTemplateMapper.selectById(id);
        if (template == null || !TEMPLATE_STATUS_ENABLE.equals(template.getStatus())) {
            throw exception(HCFQC_STANDARD_NOT_EXISTS);
        }
        return template;
    }

    private void fillTemplateSections(QmsFqcSheetTemplateRespVO respVO) {
        List<QmsFqcSheetSectionDO> sections = qmsFqcSheetSectionMapper.selectListByTemplateId(respVO.getId());
        List<Long> sectionIds = sections.stream().map(QmsFqcSheetSectionDO::getId).collect(Collectors.toList());
        Map<Long, List<QmsFqcSheetTemplateRespVO.Field>> fieldMap = sectionIds.isEmpty() ? Collections.emptyMap()
                : BeanUtils.toBean(qmsFqcSheetFieldMapper.selectListBySectionIds(sectionIds), QmsFqcSheetTemplateRespVO.Field.class)
                .stream()
                .collect(Collectors.groupingBy(QmsFqcSheetTemplateRespVO.Field::getSectionId));
        List<QmsFqcSheetTemplateRespVO.Section> sectionRespList = BeanUtils.toBean(sections, QmsFqcSheetTemplateRespVO.Section.class);
        sectionRespList.forEach(section -> section.setFields(fieldMap.getOrDefault(section.getId(), Collections.emptyList())));
        respVO.setSections(sectionRespList);
    }

    private String resolveScanTargetType(String scanCode, List<QmsFqcOrderDO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return SCAN_TARGET_UNKNOWN;
        }
        QmsFqcOrderDO first = candidates.get(0);
        if (scanCode.equals(first.getFqcNo())) {
            return SCAN_TARGET_FQC_NO;
        }
        if (scanCode.equals(first.getWorkOrderNo())) {
            return SCAN_TARGET_WORK_ORDER_NO;
        }
        if (SOURCE_MODULE_CUT_ROUND.equals(first.getSourceModule()) && scanCode.equals(first.getSourceReportNo())) {
            return SCAN_TARGET_CUT_ROUND_BATCH_NO;
        }
        if (scanCode.equals(first.getProductBatchNo()) || scanCode.equals(first.getBatchNo())) {
            return SCAN_TARGET_PRODUCT_BATCH_NO;
        }
        if (scanCode.equals(first.getMaterialCode())) {
            return SCAN_TARGET_MATERIAL_CODE;
        }
        return SCAN_TARGET_UNKNOWN;
    }

    private void recordScanHit(QmsFqcOrderDO order, String scanCode, String scene, String targetType, LocalDateTime scanTime) {
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(order.getId());
        updateObj.setLastScanCode(scanCode);
        updateObj.setLastScanScene(scene);
        updateObj.setLastScanTargetType(targetType);
        updateObj.setLastScanTime(scanTime);
        updateObj.setLastScanUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setLastScanUserName(resolveLoginUserName());
        qmsFqcOrderMapper.updateById(updateObj);
    }

    private QmsFqcScanRespVO.Candidate buildScanCandidate(QmsFqcOrderDO order) {
        QmsFqcScanRespVO.Candidate candidate = new QmsFqcScanRespVO.Candidate();
        candidate.setId(order.getId());
        candidate.setFqcNo(order.getFqcNo());
        candidate.setWorkOrderNo(order.getWorkOrderNo());
        candidate.setProductModel(order.getProductModel());
        candidate.setProductBatchNo(defaultIfBlank(order.getProductBatchNo(), order.getBatchNo()));
        candidate.setMachineCode(order.getMachineCode());
        candidate.setStatus(order.getStatus());
        candidate.setJudgment(order.getJudgment());
        candidate.setCurrentStepCode(order.getCurrentStepCode());
        candidate.setLastSaveTime(order.getLastSaveTime());
        candidate.setCreateTime(order.getCreateTime());
        return candidate;
    }

    private String sampleKey(QmsFqcSampleDO sample) {
        return sampleKey(sample.getFqcItemId(), sample.getSampleSeq());
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    private QmsFqcItemImportExcelVO buildItemImportRow(QmsFqcItemDO item) {
        QmsFqcItemImportExcelVO row = new QmsFqcItemImportExcelVO();
        row.setFqcId(item.getFqcId());
        row.setFqcNo(item.getFqcNo());
        row.setFqcItemId(item.getId());
        row.setInspectionItem(item.getInspectionItem());
        row.setItemType(item.getItemType());
        row.setValueTemplate(item.getValueTemplate());
        row.setTargetValue(item.getTargetValue());
        row.setAvgLimitText(limitText(item.getAvgMinLimit(), item.getAvgMaxLimit()));
        row.setStdLimitText(limitText(item.getStdMinLimit(), item.getStdMaxLimit()));
        row.setSampleResult(item.getItemResult());
        row.setCalculatedAvg(item.getCalculatedAvg());
        row.setCalculatedStd(item.getCalculatedStd());
        return row;
    }

    private QmsFqcItemImportExcelVO buildItemExportRow(QmsFqcItemDO item, QmsFqcSampleDO sample) {
        QmsFqcItemImportExcelVO row = buildItemImportRow(item);
        row.setSampleSeq(sample.getSampleSeq());
        row.setPositionCode(sample.getSamplePosition());
        row.setMeasuredValue(sample.getMeasuredValue());
        row.setQualitativeValue(sample.getQualitativeValue());
        row.setRemark(sample.getRemark());
        row.setSampleResult(sample.getSampleResult());
        row.setValueSource(sample.getValueSource());
        row.setImportBatchNo(sample.getImportBatchNo());
        return row;
    }

    private QmsFqcImportRespVO buildImportResp(Long id, MultipartFile file, Boolean previewOnly, String message) {
        QmsFqcImportRespVO respVO = new QmsFqcImportRespVO();
        respVO.setImportBatchNo(generateImportBatchNo());
        respVO.setFileName(file == null ? null : file.getOriginalFilename());
        respVO.setStatus("SUCCESS");
        respVO.setPreviewOnly(previewOnly);
        respVO.setTotalCount(0);
        respVO.setSuccessCount(0);
        respVO.setFailureCount(0);
        respVO.setWarningCount(0);
        respVO.setValidateSummary(message);
        respVO.setMessages(List.of(message));
        respVO.setRecord(getFqcResp(id));
        return respVO;
    }

    private String limitText(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "";
        }
        if (min == null) {
            return "<= " + max;
        }
        if (max == null) {
            return ">= " + min;
        }
        return min + " - " + max;
    }

    private String generateImportBatchNo() {
        return "FQC-IMP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.valueOf(System.currentTimeMillis() % 1_000_000L);
    }

    private QmsQualityStandardDO selectFqcStandard(QmsFqcSaveReqVO reqVO) {
        if (reqVO.getStandardId() != null) {
            QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(reqVO.getStandardId());
            validateStandardUsable(standard);
            return standard;
        }
        QmsQualityStandardDO standard = qmsQualityStandardMapper.selectList(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                        .eq(QmsQualityStandardDO::getApplyType, APPLY_TYPE_FQC)
                        .eq(QmsQualityStandardDO::getStatus, ENABLED)
                        .eq(QmsQualityStandardDO::getAuditStatus, AUDITED)
                        .orderByDesc(QmsQualityStandardDO::getId))
                .stream()
                .filter(item -> matchesMaterialScope(item, reqVO))
                .max(Comparator.comparingInt((QmsQualityStandardDO item) -> calculateStandardMatchScore(item, reqVO))
                        .thenComparing(QmsQualityStandardDO::getId))
                .orElse(null);
        if (standard == null) {
            throw exception(HCFQC_STANDARD_NOT_EXISTS);
        }
        return standard;
    }

    private void validateStandardUsable(QmsQualityStandardDO standard) {
        if (standard == null || !APPLY_TYPE_FQC.equals(standard.getApplyType())
                || !ENABLED.equals(standard.getStatus()) || !AUDITED.equals(standard.getAuditStatus())) {
            throw exception(HCFQC_STANDARD_NOT_EXISTS);
        }
    }

    private List<QmsQualityStandardItemDO> selectStandardItems(Long standardId) {
        List<QmsQualityStandardItemDO> standardItems = qmsQualityStandardItemMapper.selectListByStandardId(standardId);
        if (standardItems == null || standardItems.isEmpty()) {
            throw exception(HCFQC_STANDARD_ITEMS_EMPTY);
        }
        return standardItems;
    }

    private boolean matchesMaterialScope(QmsQualityStandardDO standard, QmsFqcSaveReqVO reqVO) {
        boolean hasMaterial = standard.getMaterialId() != null || StringUtils.hasText(standard.getMaterialCode());
        if (!hasMaterial) {
            return true;
        }
        if (standard.getMaterialId() != null && reqVO.getMaterialId() != null) {
            return standard.getMaterialId().equals(reqVO.getMaterialId());
        }
        return StringUtils.hasText(standard.getMaterialCode())
                && StringUtils.hasText(reqVO.getMaterialCode())
                && standard.getMaterialCode().equals(reqVO.getMaterialCode());
    }

    private int calculateStandardMatchScore(QmsQualityStandardDO standard, QmsFqcSaveReqVO reqVO) {
        if (standard.getMaterialId() != null && standard.getMaterialId().equals(reqVO.getMaterialId())) {
            return 4;
        }
        if (StringUtils.hasText(standard.getMaterialCode()) && standard.getMaterialCode().equals(reqVO.getMaterialCode())) {
            return 3;
        }
        return 0;
    }

    private void applyStandardSnapshot(QmsFqcOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId());
        order.setStandardNo(standard.getStandardNo());
        order.setStandardVersion(standard.getVersion());
        if (order.getMaterialId() == null) {
            order.setMaterialId(standard.getMaterialId());
        }
        if (!StringUtils.hasText(order.getMaterialCode())) {
            order.setMaterialCode(standard.getMaterialCode());
        }
        if (!StringUtils.hasText(order.getMaterialName())) {
            order.setMaterialName(standard.getMaterialName());
        }
        if (!StringUtils.hasText(order.getSpecification())) {
            order.setSpecification(standard.getSpecification());
        }
    }

    private void saveStandardSnapshotDetails(QmsFqcOrderDO order, List<QmsQualityStandardItemDO> standardItems,
                                             List<QmsFqcSaveReqVO.FqcAbnormal> abnormals) {
        for (int i = 0; i < standardItems.size(); i++) {
            QmsQualityStandardItemDO standardItem = standardItems.get(i);
            QmsFqcItemDO itemDO = new QmsFqcItemDO();
            itemDO.setFqcId(order.getId());
            itemDO.setFqcNo(order.getFqcNo());
            itemDO.setStandardItemId(standardItem.getId());
            itemDO.setCategory(resolveCategory(standardItem));
            itemDO.setInspectionItem(standardItem.getInspectionItem());
            itemDO.setItemType(standardItem.getItemType());
            itemDO.setTargetValue(standardItem.getTargetValue());
            itemDO.setStandardDesc(standardItem.getStandardDesc());
            itemDO.setUnit(standardItem.getUnit());
            itemDO.setRuleDescription(standardItem.getRuleDescription());
            itemDO.setInspectionMethod(standardItem.getInspectionMethod());
            itemDO.setTestFrequencyJudgement(standardItem.getTestFrequencyJudgement());
            itemDO.setValueTemplate(standardItem.getValueTemplate());
            itemDO.setValueTemplateName(resolveValueTemplateName(standardItem.getValueTemplate(),
                    standardItem.getEntryRuleTemplateName()));
            itemDO.setJudgmentMetric(standardItem.getJudgmentMetric());
            itemDO.setTemplateParams(standardItem.getTemplateParams());
            itemDO.setAvgMinLimit(standardItem.getAvgMinLimit());
            itemDO.setAvgMaxLimit(standardItem.getAvgMaxLimit());
            itemDO.setStdMinLimit(standardItem.getStdMinLimit());
            itemDO.setStdMaxLimit(standardItem.getStdMaxLimit());
            itemDO.setTestTool(standardItem.getTestTool());
            itemDO.setActualValueRequired(Boolean.TRUE.equals(standardItem.getActualValueRequired()));
            itemDO.setSampleSize(order.getSampleQty() != null && order.getSampleQty() > 0 ? order.getSampleQty() : standardItem.getSampleSize());
            itemDO.setMinValueLimit(standardItem.getMinValue());
            itemDO.setMaxValueLimit(standardItem.getMaxValue());
            itemDO.setItemResult(JUDGMENT_PENDING);
            itemDO.setOperatorResult(JUDGMENT_PENDING);
            itemDO.setQaResult(JUDGMENT_PENDING);
            itemDO.setSheetSectionCode(standardItem.getSheetSectionCode());
            itemDO.setSheetSectionName(defaultIfBlank(standardItem.getProcessName(), standardItem.getSheetSectionCode()));
            itemDO.setSheetMetricCode(standardItem.getSheetMetricCode());
            itemDO.setSheetMetricName(standardItem.getInspectionItem());
            itemDO.setSheetFieldCode(standardItem.getSheetFieldCode());
            itemDO.setRequiredSampleCount(itemDO.getSampleSize());
            itemDO.setCompletedSampleCount(0);
            itemDO.setAbnormalSampleCount(0);
            itemDO.setInputStatus(INPUT_STATUS_EMPTY);
            itemDO.setIsSpc(Boolean.TRUE.equals(standardItem.getIsSpc()));
            itemDO.setSort(standardItem.getSort() == null ? (i + 1) * 10 : standardItem.getSort());
            qmsFqcItemMapper.insert(itemDO);
        }
        saveAbnormals(order, abnormals);
    }

    private void updateExecutionDetails(QmsFqcOrderDO order, List<QmsFqcSaveReqVO.FqcItem> items,
                                        List<QmsFqcSaveReqVO.FqcAbnormal> abnormals) {
        validateItems(items);
        List<QmsFqcItemDO> existingItems = qmsFqcItemMapper.selectListByFqcId(order.getId());
        validateItemsExist(existingItems);
        for (QmsFqcSaveReqVO.FqcItem item : items) {
            if (!matchesExistingItem(item, existingItems)) {
                throw exception(HCFQC_ITEM_SOURCE_INVALID);
            }
        }
        qmsFqcSampleDefectMapper.deleteByFqcId(order.getId());
        qmsFqcSampleMapper.deleteByFqcId(order.getId());
        for (QmsFqcItemDO existingItem : existingItems) {
            QmsFqcSaveReqVO.FqcItem incomingItem = findIncomingItem(existingItem, items);
            if (incomingItem == null || incomingItem.getSamples() == null || incomingItem.getSamples().isEmpty()) {
                continue;
            }
            applyItemStats(existingItem, incomingItem.getSamples());
            qmsFqcItemMapper.updateById(existingItem);
            saveSamples(order, existingItem, incomingItem.getSamples());
        }
        if (abnormals != null) {
            qmsFqcAbnormalMapper.deleteByFqcId(order.getId());
            saveAbnormals(order, abnormals);
        }
    }

    private QmsFqcSaveReqVO.FqcItem findIncomingItem(QmsFqcItemDO existingItem, List<QmsFqcSaveReqVO.FqcItem> items) {
        QmsFqcSaveReqVO.FqcItem matchedById = items.stream()
                .filter(item -> item.getId() != null && item.getId().equals(existingItem.getId()))
                .findFirst().orElse(null);
        if (matchedById != null) {
            return matchedById;
        }
        return items.stream().filter(item -> item.getId() == null
                        && item.getStandardItemId() != null
                        && item.getStandardItemId().equals(existingItem.getStandardItemId()))
                .findFirst().orElse(null);
    }

    private boolean matchesExistingItem(QmsFqcSaveReqVO.FqcItem item, List<QmsFqcItemDO> existingItems) {
        if (item.getId() != null) {
            return existingItems.stream().anyMatch(existingItem -> item.getId().equals(existingItem.getId()));
        }
        return existingItems.stream().anyMatch(existingItem ->
                item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId()));
    }

    private void applyItemStats(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        String result = calculateItemResult(itemDO, samples);
        List<BigDecimal> measuredValues = samples.stream()
                .map(QmsFqcSaveReqVO.FqcSample::getMeasuredValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!measuredValues.isEmpty()) {
            BigDecimal max = Collections.max(measuredValues);
            BigDecimal min = Collections.min(measuredValues);
            BigDecimal total = measuredValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            itemDO.setMaxValue(max);
            itemDO.setMinValue(min);
            itemDO.setAverageValue(total.divide(BigDecimal.valueOf(measuredValues.size()), 6, RoundingMode.HALF_UP));
            itemDO.setOperatorMax(max);
            itemDO.setOperatorMin(min);
            itemDO.setOperatorAvg(itemDO.getAverageValue());
            itemDO.setCalculatedMax(max);
            itemDO.setCalculatedMin(min);
            itemDO.setCalculatedAvg(itemDO.getAverageValue());
        }
        itemDO.setItemResult(result);
        itemDO.setOperatorResult(result);
        itemDO.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        itemDO.setOperatorName(resolveLoginUserName());
        itemDO.setOperatorTime(LocalDateTime.now());
        itemDO.setRequiredSampleCount(itemDO.getSampleSize());
        itemDO.setCompletedSampleCount((int) samples.stream().filter(sample ->
                sample.getMeasuredValue() != null
                        || StringUtils.hasText(sample.getQualitativeValue())
                        || StringUtils.hasText(sample.getSampleResult())).count());
        itemDO.setAbnormalSampleCount((int) samples.stream().filter(sample ->
                JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue())).count());
        itemDO.setInputStatus(JUDGMENT_NG.equals(result) ? INPUT_STATUS_ABNORMAL
                : JUDGMENT_OK.equals(result) ? INPUT_STATUS_COMPLETE : "FILLING");
    }

    private String calculateItemResult(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        if (samples == null || samples.size() < itemDO.getSampleSize()) {
            return JUDGMENT_PENDING;
        }
        boolean hasPending = false;
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            if (JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue())) {
                return JUDGMENT_NG;
            }
            BigDecimal value = sample.getMeasuredValue();
            if (value != null) {
                if (itemDO.getMinValueLimit() != null && value.compareTo(itemDO.getMinValueLimit()) < 0) {
                    return JUDGMENT_NG;
                }
                if (itemDO.getMaxValueLimit() != null && value.compareTo(itemDO.getMaxValueLimit()) > 0) {
                    return JUDGMENT_NG;
                }
                continue;
            }
            if (!JUDGMENT_OK.equals(sample.getSampleResult()) && !JUDGMENT_OK.equals(sample.getQualitativeValue())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private String calculateOrderJudgment(List<QmsFqcItemDO> items) {
        validateItemsExist(items);
        boolean hasPending = false;
        for (QmsFqcItemDO item : items) {
            if (JUDGMENT_NG.equals(item.getItemResult())) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(item.getItemResult())) {
                hasPending = true;
            }
        }
        if (hasPending) {
            throw exception(HCFQC_ITEMS_NOT_COMPLETED);
        }
        return JUDGMENT_OK;
    }

    private void saveSamples(QmsFqcOrderDO order, QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        List<QmsFqcSampleDO> sampleList = new ArrayList<>();
        List<List<QmsDefectCodeDO>> sampleDefectList = new ArrayList<>();
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            boolean ngSample = isNgSample(sample);
            List<QmsDefectCodeDO> defects = ngSample ? resolveSampleDefects(sample) : Collections.emptyList();
            if (ngSample && defects.isEmpty()) {
                throw exception(HCFQC_SAMPLE_DEFECT_REQUIRED, buildSampleLabel(itemDO, sample));
            }
            QmsFqcSampleDO sampleDO = BeanUtils.toBean(sample, QmsFqcSampleDO.class);
            sampleDO.setId(null);
            sampleDO.setFqcId(order.getId());
            sampleDO.setFqcItemId(itemDO.getId());
            sampleDO.setFqcNo(order.getFqcNo());
            sampleDO.setSubmissionDetailId(itemDO.getSubmissionDetailId());
            sampleDO.setCutRoundInspectionDetailId(itemDO.getCutRoundInspectionDetailId());
            sampleDO.setSliceSeqNo(itemDO.getSliceSeqNo());
            sampleDO.setProductionBatchNo(itemDO.getProductionBatchNo());
            sampleDO.setParentProductionBatchNo(itemDO.getParentProductionBatchNo());
            sampleDO.setStepCode(itemDO.getStepCode());
            sampleDO.setMetricCode(itemDO.getMetricCode());
            sampleDO.setMetricGroupCode(itemDO.getMetricGroupCode());
            sampleDO.setInputComponent(itemDO.getInputComponent());
            sampleDO.setSheetSectionCode(itemDO.getSheetSectionCode());
            sampleDO.setSheetMetricCode(itemDO.getSheetMetricCode());
            sampleDO.setSampleRole("OPERATOR");
            sampleDO.setValueSource(defaultIfBlank(sample.getValueSource(), VALUE_SOURCE_MANUAL));
            sampleDO.setInputTime(sample.getInputTime() == null ? LocalDateTime.now() : sample.getInputTime());
            sampleDO.setSampleResult(defaultIfBlank(sample.getSampleResult(), JUDGMENT_PENDING));
            if (defects.isEmpty()) {
                sampleDO.setDefectCode(null);
                sampleDO.setDefectName(null);
            } else {
                QmsDefectCodeDO firstDefect = defects.get(0);
                sampleDO.setDefectCode(firstDefect.getCode());
                sampleDO.setDefectName(firstDefect.getName());
            }
            sampleList.add(sampleDO);
            sampleDefectList.add(defects);
        }
        qmsFqcSampleMapper.insertBatch(sampleList);
        saveSampleDefects(order, itemDO, sampleList, sampleDefectList);
    }

    private void saveSampleDefects(QmsFqcOrderDO order, QmsFqcItemDO itemDO, List<QmsFqcSampleDO> sampleList,
                                   List<List<QmsDefectCodeDO>> sampleDefectList) {
        List<QmsFqcSampleDefectDO> defectList = new ArrayList<>();
        for (int sampleIndex = 0; sampleIndex < sampleList.size(); sampleIndex++) {
            QmsFqcSampleDO sampleDO = sampleList.get(sampleIndex);
            List<QmsDefectCodeDO> defects = sampleDefectList.get(sampleIndex);
            for (int defectIndex = 0; defectIndex < defects.size(); defectIndex++) {
                QmsDefectCodeDO defect = defects.get(defectIndex);
                QmsFqcSampleDefectDO defectDO = new QmsFqcSampleDefectDO();
                defectDO.setFqcId(order.getId());
                defectDO.setFqcNo(order.getFqcNo());
                defectDO.setFqcItemId(itemDO.getId());
                defectDO.setSampleId(sampleDO.getId());
                defectDO.setSampleSeq(sampleDO.getSampleSeq());
                defectDO.setSamplePosition(sampleDO.getSamplePosition());
                defectDO.setDefectCodeId(defect.getId());
                defectDO.setDefectCode(defect.getCode());
                defectDO.setDefectName(defect.getName());
                defectDO.setDefectLevel(defect.getLevel());
                defectDO.setSort((defectIndex + 1) * 10);
                defectDO.setTenantId(order.getTenantId() == null ? 0L : order.getTenantId());
                defectList.add(defectDO);
            }
        }
        if (!defectList.isEmpty()) {
            qmsFqcSampleDefectMapper.insertBatch(defectList);
        }
    }

    private List<QmsDefectCodeDO> resolveSampleDefects(QmsFqcSaveReqVO.FqcSample sample) {
        LinkedHashMap<Long, QmsDefectCodeDO> defects = new LinkedHashMap<>();
        if (sample.getDefects() != null) {
            for (QmsFqcSaveReqVO.FqcSampleDefect defect : sample.getDefects()) {
                addResolvedDefect(defects, defect.getDefectCodeId(), defect.getDefectCode());
            }
        }
        if (defects.isEmpty() && StringUtils.hasText(sample.getDefectCode())) {
            addResolvedDefect(defects, null, sample.getDefectCode());
        }
        return new ArrayList<>(defects.values());
    }

    private void addResolvedDefect(LinkedHashMap<Long, QmsDefectCodeDO> defects, Long defectCodeId, String defectCode) {
        QmsDefectCodeDO defect = null;
        if (defectCodeId != null) {
            defect = qmsDefectCodeMapper.selectById(defectCodeId);
        }
        if (defect == null && StringUtils.hasText(defectCode)) {
            defect = qmsDefectCodeMapper.selectOne(new LambdaQueryWrapperX<QmsDefectCodeDO>()
                    .eq(QmsDefectCodeDO::getCode, defectCode.trim()));
        }
        if (defect == null || !DEFECT_TYPE_ITEM.equals(defect.getType()) || !ENABLED.equals(defect.getStatus())) {
            throw exception(HCFQC_SAMPLE_DEFECT_INVALID, StringUtils.hasText(defectCode) ? defectCode.trim() : String.valueOf(defectCodeId));
        }
        defects.putIfAbsent(defect.getId(), defect);
    }

    private boolean isNgSample(QmsFqcSaveReqVO.FqcSample sample) {
        return JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue());
    }

    private String buildSampleLabel(QmsFqcItemDO itemDO, QmsFqcSaveReqVO.FqcSample sample) {
        return itemDO.getInspectionItem() + "/样本" + sample.getSampleSeq();
    }

    private void saveAbnormals(QmsFqcOrderDO order, List<QmsFqcSaveReqVO.FqcAbnormal> abnormals) {
        if (abnormals == null || abnormals.isEmpty()) {
            return;
        }
        List<QmsFqcAbnormalDO> abnormalList = abnormals.stream().map(abnormal -> {
            QmsFqcAbnormalDO abnormalDO = BeanUtils.toBean(abnormal, QmsFqcAbnormalDO.class);
            abnormalDO.setId(null);
            abnormalDO.setFqcId(order.getId());
            abnormalDO.setFqcNo(order.getFqcNo());
            abnormalDO.setProcessStatus(defaultIfBlank(abnormal.getProcessStatus(), PROCESS_PENDING));
            return abnormalDO;
        }).collect(Collectors.toList());
        qmsFqcAbnormalMapper.insertBatch(abnormalList);
    }

    private void ensureNgAbnormal(Long fqcId, String fqcNo, List<QmsFqcItemDO> items, String relatedNcrNo) {
        if (!qmsFqcAbnormalMapper.selectListByFqcId(fqcId).isEmpty()) {
            return;
        }
        Map<Long, String> defectSummaryByItem = qmsFqcSampleDefectMapper.selectListByFqcId(fqcId).stream()
                .collect(Collectors.groupingBy(QmsFqcSampleDefectDO::getFqcItemId, Collectors.mapping(
                        QmsFqcSampleDefectDO::getDefectName,
                        Collectors.collectingAndThen(Collectors.toCollection(java.util.LinkedHashSet::new),
                                names -> names.stream().filter(StringUtils::hasText).collect(Collectors.joining("、"))))));
        List<QmsFqcAbnormalDO> abnormalList = items.stream()
                .filter(item -> JUDGMENT_NG.equals(item.getItemResult()))
                .map(item -> QmsFqcAbnormalDO.builder()
                        .fqcId(fqcId)
                        .fqcNo(fqcNo)
                        .fqcItemId(item.getId())
                        .abnormalDesc("成品检验项判定不合格：" + item.getInspectionItem()
                                + (StringUtils.hasText(defectSummaryByItem.get(item.getId()))
                                ? "；缺陷：" + defectSummaryByItem.get(item.getId()) : ""))
                        .processStatus(PROCESS_PENDING)
                        .actionRequired("FREEZE_BATCH")
                        .relatedNcrNo(relatedNcrNo)
                        .dispositionStatus("APPROVING")
                        .build())
                .collect(Collectors.toList());
        if (!abnormalList.isEmpty()) {
            qmsFqcAbnormalMapper.insertBatch(abnormalList);
        }
    }

    private void syncCutRoundInspectionFromFqc(Long fqcId) {
        QmsFqcOrderDO order = fqcId == null ? null : qmsFqcOrderMapper.selectById(fqcId);
        if (order == null || !SOURCE_MODULE_CUT_ROUND.equals(order.getSourceModule())) {
            return;
        }
        List<HcCutRoundInspectionDetailDO> details = hcCutRoundInspectionDetailMapper.selectListByFqcOrderId(order.getId());
        if (details.isEmpty() && order.getSourceReportId() != null) {
            details = hcCutRoundInspectionDetailMapper.selectListByCutRoundReportId(order.getSourceReportId()).stream()
                    .filter(detail -> detail.getFqcOrderId() == null || Objects.equals(detail.getFqcOrderId(), order.getId()))
                    .collect(Collectors.toList());
        }
        if (details.isEmpty()) {
            return;
        }
        String inspectionResult = resolveCutRoundInspectionResult(order);
        LocalDateTime inspectionTime = order.getQaTime() == null ? order.getInspectionTime() : order.getQaTime();
        String inspectorName = defaultIfBlank(order.getQaInspectorName(), order.getInspectorName());
        String remark = buildCutRoundInspectionRemark(order, inspectionResult);
        for (HcCutRoundInspectionDetailDO detail : details) {
            hcCutRoundInspectionDetailMapper.update(null, new LambdaUpdateWrapper<HcCutRoundInspectionDetailDO>()
                    .eq(HcCutRoundInspectionDetailDO::getId, detail.getId())
                    .set(HcCutRoundInspectionDetailDO::getFqcOrderId, order.getId())
                    .set(HcCutRoundInspectionDetailDO::getFqcNo, order.getFqcNo())
                    .set(HcCutRoundInspectionDetailDO::getFqcStatus, order.getStatus())
                    .set(HcCutRoundInspectionDetailDO::getFqcJudgment, order.getJudgment())
                    .set(HcCutRoundInspectionDetailDO::getInspectionResult, inspectionResult)
                    .set(HcCutRoundInspectionDetailDO::getInspectorName, inspectorName)
                    .set(HcCutRoundInspectionDetailDO::getInspectionTime, inspectionTime)
                    .set(HcCutRoundInspectionDetailDO::getRemark, remark));
            syncCutRoundReportInspection(detail.getCutRoundReportId(), order, inspectionResult, inspectorName, inspectionTime, remark);
            refreshCutRoundInspectionTaskStatus(detail.getTaskId());
        }
    }

    private void syncCutRoundReportInspection(Long cutRoundReportId, QmsFqcOrderDO order, String inspectionResult,
                                              String inspectorName, LocalDateTime inspectionTime, String remark) {
        if (cutRoundReportId == null) {
            return;
        }
        String inspectionStatus = inspectionResult == null
                ? CUT_ROUND_INSPECTION_STATUS_INSPECTING : CUT_ROUND_INSPECTION_STATUS_COMPLETED;
        hcCutRoundReportMapper.update(null, new LambdaUpdateWrapper<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getId, cutRoundReportId)
                .set(HcCutRoundReportDO::getInspectionStatus, inspectionStatus)
                .set(HcCutRoundReportDO::getInspectionResult, inspectionResult)
                .set(HcCutRoundReportDO::getInspectorName, inspectorName)
                .set(HcCutRoundReportDO::getInspectionTime, inspectionTime)
                .set(HcCutRoundReportDO::getInspectionRemark, remark));
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(cutRoundReportId);
        hcPackagingPieceEventLogService.recordCutRoundFqcResult(report, inspectionResult, inspectorName,
                inspectionTime == null ? LocalDateTime.now() : inspectionTime, order.getFqcNo(), remark);
    }

    private void refreshCutRoundInspectionTaskStatus(Long taskId) {
        List<HcCutRoundInspectionDetailDO> details = hcCutRoundInspectionDetailMapper.selectListByTaskId(taskId);
        if (details.isEmpty()) {
            return;
        }
        long completedCount = details.stream().filter(this::isCutRoundInspectionDetailFinal).count();
        long ngCount = details.stream().filter(this::isCutRoundInspectionDetailNg).count();
        boolean hasWaitingQa = details.stream().anyMatch(detail -> STATUS_WAITING_QA.equals(detail.getFqcStatus()));
        String taskStatus;
        if (completedCount == details.size()) {
            taskStatus = ngCount == 0 ? CUT_ROUND_TASK_STATUS_COMPLETED
                    : ngCount == details.size() ? CUT_ROUND_TASK_STATUS_REJECTED : CUT_ROUND_TASK_STATUS_PARTIAL_NG;
        } else {
            taskStatus = hasWaitingQa ? CUT_ROUND_TASK_STATUS_WAITING_QA : CUT_ROUND_TASK_STATUS_INSPECTING;
        }
        hcCutRoundInspectionTaskMapper.update(null, new LambdaUpdateWrapper<HcCutRoundInspectionTaskDO>()
                .eq(HcCutRoundInspectionTaskDO::getId, taskId)
                .set(HcCutRoundInspectionTaskDO::getTaskStatus, taskStatus));
    }

    private boolean isCutRoundInspectionDetailFinal(HcCutRoundInspectionDetailDO detail) {
        return STATUS_COMPLETED.equals(detail.getFqcStatus()) || STATUS_REJECTED.equals(detail.getFqcStatus())
                || JUDGMENT_OK.equals(detail.getInspectionResult()) || JUDGMENT_NG.equals(detail.getInspectionResult());
    }

    private boolean isCutRoundInspectionDetailNg(HcCutRoundInspectionDetailDO detail) {
        return JUDGMENT_NG.equals(detail.getInspectionResult()) || JUDGMENT_NG.equals(detail.getFqcJudgment())
                || STATUS_REJECTED.equals(detail.getFqcStatus());
    }

    private String resolveCutRoundInspectionResult(QmsFqcOrderDO order) {
        if (STATUS_COMPLETED.equals(order.getStatus()) && JUDGMENT_OK.equals(order.getJudgment())) {
            return JUDGMENT_OK;
        }
        if (STATUS_REJECTED.equals(order.getStatus()) || JUDGMENT_NG.equals(order.getJudgment())) {
            return JUDGMENT_NG;
        }
        return null;
    }

    private String buildCutRoundInspectionRemark(QmsFqcOrderDO order, String inspectionResult) {
        if (JUDGMENT_NG.equals(inspectionResult)) {
            return StringUtils.hasText(order.getRelatedNcrNo())
                    ? "FQC判定不合格，NCR：" + order.getRelatedNcrNo() : "FQC判定不合格";
        }
        if (JUDGMENT_OK.equals(inspectionResult)) {
            return "FQC判定合格";
        }
        if (STATUS_WAITING_QA.equals(order.getStatus())) {
            return "FQC已提交，等待质量审核";
        }
        if (STATUS_SUSPENDED.equals(order.getStatus())) {
            return "FQC已挂起";
        }
        if (STATUS_INSPECTING.equals(order.getStatus())) {
            return StringUtils.hasText(order.getLastReturnReason())
                    ? "FQC退回修改：" + order.getLastReturnReason() : "FQC检验中";
        }
        return "FQC待检";
    }

    private void fillDetails(QmsFqcRespVO respVO) {
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(respVO.getId());
        List<QmsFqcSampleDO> samples = qmsFqcSampleMapper.selectListByFqcId(respVO.getId());
        List<QmsFqcRespVO.FqcSample> sampleRespList = BeanUtils.toBean(samples, QmsFqcRespVO.FqcSample.class);
        fillSampleDefects(respVO.getId(), sampleRespList);
        Map<Long, List<QmsFqcRespVO.FqcSample>> sampleMap = sampleRespList.stream()
                .collect(Collectors.groupingBy(QmsFqcRespVO.FqcSample::getFqcItemId));
        List<QmsFqcRespVO.FqcItem> itemRespList = BeanUtils.toBean(items, QmsFqcRespVO.FqcItem.class);
        itemRespList.forEach(item -> item.setSamples(sampleMap.getOrDefault(item.getId(), Collections.emptyList())));
        respVO.setItems(itemRespList);
        respVO.setAbnormals(BeanUtils.toBean(qmsFqcAbnormalMapper.selectListByFqcId(respVO.getId()), QmsFqcRespVO.FqcAbnormal.class));
    }

    private void fillSampleDefects(Long fqcId, List<QmsFqcRespVO.FqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcRespVO.FqcSampleDefect>> defectBySampleId = new LinkedHashMap<>();
        Map<String, List<QmsFqcRespVO.FqcSampleDefect>> defectByItemSeq = new LinkedHashMap<>();
        for (QmsFqcSampleDefectDO defectDO : qmsFqcSampleDefectMapper.selectListByFqcId(fqcId)) {
            QmsFqcRespVO.FqcSampleDefect defect = BeanUtils.toBean(defectDO, QmsFqcRespVO.FqcSampleDefect.class);
            if (defectDO.getSampleId() != null) {
                defectBySampleId.computeIfAbsent(defectDO.getSampleId(), key -> new ArrayList<>()).add(defect);
            }
            defectByItemSeq.computeIfAbsent(defectSampleKey(defectDO.getFqcItemId(), defectDO.getSampleSeq()), key -> new ArrayList<>()).add(defect);
        }
        for (QmsFqcRespVO.FqcSample sample : samples) {
            List<QmsFqcRespVO.FqcSampleDefect> defects = sample.getId() == null
                    ? null : defectBySampleId.get(sample.getId());
            if (defects == null || defects.isEmpty()) {
                defects = defectByItemSeq.getOrDefault(defectSampleKey(sample.getFqcItemId(), sample.getSampleSeq()), Collections.emptyList());
            }
            sample.setDefects(defects);
            if (!defects.isEmpty()) {
                sample.setDefectCode(defects.get(0).getDefectCode());
                sample.setDefectName(defects.get(0).getDefectName());
            }
        }
    }

    private String defectSampleKey(Long fqcItemId, Integer sampleSeq) {
        return String.valueOf(fqcItemId) + "#" + String.valueOf(sampleSeq);
    }

    private QmsFqcStandardRespVO.StandardItem buildStandardItem(QmsQualityStandardItemDO item) {
        QmsFqcStandardRespVO.StandardItem resp = BeanUtils.toBean(item, QmsFqcStandardRespVO.StandardItem.class);
        resp.setStandardItemId(item.getId());
        resp.setCategory(resolveCategory(item));
        resp.setMinValueLimit(item.getMinValue());
        resp.setMaxValueLimit(item.getMaxValue());
        return resp;
    }

    private String resolveCategory(QmsQualityStandardItemDO item) {
        String text = (item.getInspectionItem() == null ? "" : item.getInspectionItem()) + (item.getStandardDesc() == null ? "" : item.getStandardDesc());
        if (text.contains("尺寸") || text.contains("长度") || text.contains("厚度") || text.contains("宽度")) {
            return "DIMENSION";
        }
        if (text.contains("性能") || text.contains("硬度") || text.contains("拉伸") || text.contains("压缩")) {
            return "PERFORMANCE";
        }
        if (text.contains("包装") || text.contains("标签")) {
            return "PACKAGE";
        }
        return "APPEARANCE";
    }

    private QmsFqcOrderDO validateFqcExists(Long id) {
        QmsFqcOrderDO entity = qmsFqcOrderMapper.selectById(id);
        if (entity == null) {
            throw exception(HCFQC_NOT_EXISTS);
        }
        return entity;
    }

    private void validateEditable(QmsFqcOrderDO entity) {
        if (STATUS_COMPLETED.equals(entity.getStatus()) || STATUS_REJECTED.equals(entity.getStatus()) || STATUS_CANCELED.equals(entity.getStatus())) {
            throw exception(HCFQC_FINISHED_LOCKED);
        }
    }

    private void validateEntryEditable(QmsFqcOrderDO entity) {
        validateEditable(entity);
        if (Boolean.TRUE.equals(entity.getSheetLocked()) || STATUS_WAITING_QA.equals(entity.getStatus())) {
            throw exception(HCFQC_FINISHED_LOCKED);
        }
    }

    private void validateFqcNoUnique(Long id, String fqcNo) {
        if (qmsFqcOrderMapper.selectByFqcNo(fqcNo, id) != null) {
            throw exception(HCFQC_NO_EXISTS);
        }
    }

    private void validateItems(List<QmsFqcSaveReqVO.FqcItem> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCFQC_ITEMS_EMPTY);
        }
    }

    private void validateItemsExist(Collection<QmsFqcItemDO> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCFQC_ITEMS_EMPTY);
        }
    }

    private String generateFqcNo() {
        return qmsNoGeneratorService.generateNo(APPLY_TYPE_FQC);
    }

    private String generateNcrNo() {
        return "NCR-FQC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.valueOf(System.currentTimeMillis() % 1_000_000L);
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private String buildFqcAuditBizName(QmsFqcOrderDO order) {
        List<String> parts = new ArrayList<>();
        appendBizNamePart(parts, order.getMaterialName());
        appendBizNamePart(parts, order.getMaterialCode());
        appendBizNamePart(parts, order.getProductModel());
        appendBizNamePart(parts, order.getBatchNo());
        return parts.isEmpty() ? defaultIfBlank(order.getFqcNo(), "") : String.join(" / ", parts);
    }

    private void appendBizNamePart(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private String resolveValueTemplateName(String valueTemplate, String customTemplateName) {
        if (StringUtils.hasText(customTemplateName)) {
            return customTemplateName;
        }
        if ("DENSITY_CALC".equals(valueTemplate)) {
            return "密度计算模板";
        }
        if ("COMPRESSION_CALC".equals(valueTemplate)) {
            return "压缩性能计算模板";
        }
        if ("SINGLE_VALUE".equals(valueTemplate)) {
            return "单值实测模板";
        }
        return null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) && !"-".equals(value) ? value : defaultValue;
    }

}
