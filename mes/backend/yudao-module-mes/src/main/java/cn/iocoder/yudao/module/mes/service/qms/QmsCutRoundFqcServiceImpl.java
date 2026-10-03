package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcItemPhotoDefectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcItemPhotoDefectRespVO;
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
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemPhotoDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemPhotoDefectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcReturnRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDefectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcScanRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.service.hc.packagingevent.HcPackagingPieceEventLogService;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.service.hc.qtimeconfig.HcQtimeEvaluationService;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticePickItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemPhotoMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemPhotoDefectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcReturnRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleDefectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcScanRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSubmissionDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class QmsCutRoundFqcServiceImpl implements QmsCutRoundFqcService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final String SOURCE_MODULE_CUT_ROUND_FQC = "CUT_ROUND_FQC";
    private static final String CUT_ROUND_FQC_SELF_CHECK_LIGHT_TABLE_FORM_CODE =
            "CUT_FQC_SELF_CHECK_LIGHT_TABLE";
    private static final String CUT_ROUND_FQC_SELF_CHECK_VACUUM_FORM_CODE =
            "CUT_FQC_SELF_CHECK_CLASS100_VACUUM";
    private static final String CUT_ROUND_FQC_SELF_CHECK_CLEANING_FORM_CODE =
            "CUT_FQC_SELF_CHECK_CLASS100_CLEANING";
    private static final Map<String, String> CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_NAMES =
            buildCutRoundFqcRequiredSelfCheckFormNames();
    private static final String SOURCE_MENU_CODE_CUT_ROUND_SEGMENT_COMPLETE = "CUT_ROUND_SEGMENT_COMPLETE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_WAITING_QA = "WAITING_QA";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String RELEASE_ALLOW_INBOUND = "ALLOW_INBOUND";
    private static final String RELEASE_PIECE_CONTROLLED = "PIECE_CONTROLLED";
    private static final String AUDIT_PASS = "PASS";
    private static final String AUDIT_REJECT = "REJECT";
    private static final String SCAN_TARGET_PRODUCTION_BATCH_NO = "PRODUCTION_BATCH_NO";
    private static final String SCAN_TARGET_PARENT_PRODUCTION_BATCH_NO = "PARENT_PRODUCTION_BATCH_NO";
    private static final String SCAN_TARGET_FQC_ORDER = "FQC_ORDER";
    private static final String SCAN_RESULT_MATCHED_SINGLE = "MATCHED_SINGLE";
    private static final String SCAN_RESULT_MATCHED_MULTIPLE = "MATCHED_MULTIPLE";
    private static final String SCAN_RESULT_NOT_FOUND = "NOT_FOUND";
    private static final String OPEN_TARGET_WORKBENCH = "WORKBENCH";
    private static final String CUT_ROUND_INSPECTION_STATUS_INSPECTING = "INSPECTING";
    private static final String CUT_ROUND_INSPECTION_STATUS_COMPLETED = "COMPLETED";
    private static final String CUT_ROUND_TASK_STATUS_INSPECTING = "INSPECTING";
    private static final String CUT_ROUND_TASK_STATUS_WAITING_QA = "WAITING_QA";
    private static final String CUT_ROUND_TASK_STATUS_COMPLETED = "COMPLETED";
    private static final String CUT_ROUND_TASK_STATUS_PARTIAL_NG = "PARTIAL_NG";
    private static final String ENTRY_LAYOUT_PROGRAM_FORM = "PROGRAM_FORM";
    private static final String ENTRY_MODE_MANUAL = "MANUAL";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_FILLING = "FILLING";
    private static final Integer STANDARD_STATUS_ENABLED = 1;
    private static final Integer STANDARD_AUDIT_STATUS_AUDITED = 20;
    private static final String SAMPLE_ROLE_OPERATOR = "OPERATOR";
    private static final String VALUE_SOURCE_MANUAL = "MANUAL";
    private static final int SUBMISSION_DETAIL_PHOTO_MAX_COUNT = 20;
    private static final int SUBMISSION_DETAIL_PHOTO_MAX_URL_LENGTH = 2048;
    private static final int ITEM_PHOTO_MAX_COUNT = 6;
    private static final Set<String> ITEM_PHOTO_SCENES = Set.of("RESULT", "DEFECT", "RECHECK");
    private static final Set<String> SUBMISSION_DETAIL_PHOTO_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp");

    private static Map<String, String> buildCutRoundFqcRequiredSelfCheckFormNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put(CUT_ROUND_FQC_SELF_CHECK_LIGHT_TABLE_FORM_CODE, "检验光桌日常点检表");
        names.put(CUT_ROUND_FQC_SELF_CHECK_VACUUM_FORM_CODE, "百级吸尘器日常点检表");
        names.put(CUT_ROUND_FQC_SELF_CHECK_CLEANING_FORM_CODE, "百级房卫生清洁记录表");
        return names;
    }

    @Resource
    private QmsFqcService qmsFqcService;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Resource
    private QmsFqcItemMapper qmsFqcItemMapper;
    @Resource
    private QmsFqcItemPhotoMapper qmsFqcItemPhotoMapper;
    @Resource
    private QmsFqcItemPhotoDefectMapper qmsFqcItemPhotoDefectMapper;
    @Resource
    private QmsDefectCodeMapper qmsDefectCodeMapper;
    @Resource
    private QmsFqcSampleMapper qmsFqcSampleMapper;
    @Resource
    private QmsFqcSampleDefectMapper qmsFqcSampleDefectMapper;
    @Resource
    private QmsFqcScanRecordMapper qmsFqcScanRecordMapper;
    @Resource
    private QmsFqcSubmissionDetailMapper qmsFqcSubmissionDetailMapper;
    @Resource
    private QmsFqcReturnRecordMapper qmsFqcReturnRecordMapper;
    @Resource
    private HcCutRoundInspectionTaskMapper hcCutRoundInspectionTaskMapper;
    @Resource
    private HcCutRoundInspectionDetailMapper hcCutRoundInspectionDetailMapper;
    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private HcPackagingPieceEventLogService hcPackagingPieceEventLogService;
    @Resource
    private HcProcessReportMapper hcProcessReportMapper;
    @Resource
    private HcProcessFormRecordMapper hcProcessFormRecordMapper;
    @Resource
    private HcQtimeEvaluationService hcQtimeEvaluationService;
    @Resource
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Resource
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Resource
    private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFqcOrderDO createFromCutRoundInspectionTask(HcPlanOrderDO planOrder,
                                                           HcPlanOrderOperationDO operation,
                                                           HcCutRoundInspectionTaskDO task,
                                                           List<HcCutRoundInspectionDetailDO> details,
                                                           Map<Long, HcCutRoundReportDO> reportMap) {
        if (task == null || details == null || details.isEmpty()) {
            throw invalidParamException("裁切报检任务和送检明细不能为空");
        }
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectLatestBySource(SOURCE_MODULE_CUT_ROUND_FQC, task.getId());
        if (order == null) {
            Long fqcId = qmsFqcService.createFqc(buildCreateReq(planOrder, operation, task, details, reportMap));
            order = qmsFqcOrderMapper.selectById(fqcId);
        }
        upsertSubmissionDetails(order, planOrder, operation, task, details, reportMap == null ? Collections.emptyMap() : reportMap);
        ensurePieceInspectionItems(order.getId());
        syncActualValueRequiredSnapshot(order);
        refreshOrderItemProgress(order.getId());
        refreshOrderSubmissionStats(order.getId());
        syncCutRoundInspectionFromSubmissionDetails(order.getId());
        return qmsFqcOrderMapper.selectById(order.getId());
    }

    @Override
    public PageResult<QmsCutRoundFqcRespVO> getPage(QmsCutRoundFqcPageReqVO pageReqVO) {
        List<Long> fqcIds = null;
        if (StrUtil.isNotBlank(pageReqVO.getProductionBatchNo())) {
            fqcIds = qmsFqcSubmissionDetailMapper.selectFqcIdsByProductionBatchNo(pageReqVO.getProductionBatchNo().trim());
            if (fqcIds.isEmpty()) {
                return PageResult.empty();
            }
        }
        LambdaQueryWrapperX<QmsFqcOrderDO> wrapper = new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_CUT_ROUND_FQC)
                .inIfPresent(QmsFqcOrderDO::getId, fqcIds)
                .likeIfPresent(QmsFqcOrderDO::getFqcNo, pageReqVO.getFqcNo())
                .likeIfPresent(QmsFqcOrderDO::getReportNo, pageReqVO.getTaskNo())
                .likeIfPresent(QmsFqcOrderDO::getWorkOrderNo, pageReqVO.getPlanNo())
                .likeIfPresent(QmsFqcOrderDO::getMaterialCode, pageReqVO.getMaterialCode())
                .likeIfPresent(QmsFqcOrderDO::getProductModel, pageReqVO.getProductModel())
                .eqIfPresent(QmsFqcOrderDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(QmsFqcOrderDO::getJudgment, pageReqVO.getJudgment())
                .eqIfPresent(QmsFqcOrderDO::getRecheckFlag, pageReqVO.getRecheckFlag())
                .betweenIfPresent(QmsFqcOrderDO::getSubmissionTime, pageReqVO.getSubmissionTime())
                .orderByDesc(QmsFqcOrderDO::getId);
        PageResult<QmsFqcOrderDO> page = qmsFqcOrderMapper.selectPage(pageReqVO, wrapper);
        List<QmsCutRoundFqcRespVO> records = BeanUtils.toBean(page.getList(), QmsCutRoundFqcRespVO.class);
        records.forEach(this::fillRecheckSource);
        return new PageResult<>(records, page.getTotal());
    }

    @Override
    public List<QmsDefectCodeRespVO> getDefectCodeOptions() {
        return qmsFqcService.getFqcDefectCodeOptions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO startProgramEntry(Long id) {
        QmsFqcOrderDO order = validateCutRoundFqc(id);
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        LocalDateTime now = LocalDateTime.now();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = resolveLoginUserName();
        if (order.getInspectionTime() == null) {
            QmsFqcOrderDO updateOrder = new QmsFqcOrderDO();
            updateOrder.setId(order.getId());
            updateOrder.setStatus(STATUS_INSPECTING);
            updateOrder.setInspectorId(userId);
            updateOrder.setInspectorName(userName);
            updateOrder.setInspectionTime(now);
            qmsFqcOrderMapper.updateById(updateOrder);
        }
        return getLight(order.getId());
    }

    @Override
    public QmsCutRoundFqcRespVO get(Long id) {
        QmsFqcOrderDO order = validateCutRoundFqc(id);
        ensurePieceInspectionItems(order.getId());
        syncActualValueRequiredSnapshot(order);
        QmsCutRoundFqcRespVO respVO = BeanUtils.toBean(order, QmsCutRoundFqcRespVO.class);
        fillRecheckSource(respVO);
        QmsFqcRespVO fqcResp = qmsFqcService.getFqcResp(order.getId());
        List<QmsCutRoundFqcRespVO.SubmissionDetail> detailRespList = BeanUtils.toBean(
                qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId()),
                QmsCutRoundFqcRespVO.SubmissionDetail.class);
        fillSubmissionDetailItems(detailRespList, fqcResp.getItems());
        fillBatchQtime(respVO, detailRespList);
        respVO.setSubmissionDetails(detailRespList);
        respVO.setItems(fqcResp.getItems());
        respVO.setAbnormals(fqcResp.getAbnormals());
        return respVO;
    }

    @Override
    public QmsCutRoundFqcRespVO getLight(Long id) {
        QmsFqcOrderDO order = validateCutRoundFqc(id);
        ensurePieceInspectionItems(order.getId());
        syncActualValueRequiredSnapshot(order);
        QmsCutRoundFqcRespVO respVO = BeanUtils.toBean(order, QmsCutRoundFqcRespVO.class);
        fillRecheckSource(respVO);
        List<QmsCutRoundFqcRespVO.SubmissionDetail> detailRespList = BeanUtils.toBean(
                qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId()),
                QmsCutRoundFqcRespVO.SubmissionDetail.class);
        fillSubmissionDetailStats(detailRespList, qmsFqcItemMapper.selectListByFqcId(order.getId()));
        fillBatchQtime(respVO, detailRespList);
        respVO.setSubmissionDetails(detailRespList);
        respVO.setItems(Collections.emptyList());
        respVO.setAbnormals(Collections.emptyList());
        return respVO;
    }

    @Override
    public List<QmsFqcRespVO.FqcItem> getSubmissionDetailItems(Long id, Long submissionDetailId) {
        QmsFqcOrderDO order = validateCutRoundFqc(id);
        syncActualValueRequiredSnapshot(order);
        QmsFqcSubmissionDetailDO detail = qmsFqcSubmissionDetailMapper.selectById(submissionDetailId);
        if (detail == null || !Objects.equals(detail.getFqcId(), id)) {
            throw invalidParamException("送检片号不存在");
        }
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcIdAndSubmissionDetailId(id, submissionDetailId);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> itemIds = items.stream()
                .map(QmsFqcItemDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<QmsFqcRespVO.FqcSample> sampleRespList = BeanUtils.toBean(
                qmsFqcSampleMapper.selectListByFqcIdAndItemIds(id, itemIds),
                QmsFqcRespVO.FqcSample.class);
        fillCutRoundSampleDefects(id, itemIds, sampleRespList);
        Map<Long, List<QmsFqcRespVO.FqcSample>> sampleMap = sampleRespList.stream()
                .collect(Collectors.groupingBy(QmsFqcRespVO.FqcSample::getFqcItemId));
        List<QmsFqcRespVO.FqcItem> itemRespList = BeanUtils.toBean(items, QmsFqcRespVO.FqcItem.class);
        itemRespList.forEach(item -> item.setSamples(sampleMap.getOrDefault(item.getId(), Collections.emptyList())));
        return itemRespList;
    }

    @Override
    public List<QmsCutRoundFqcItemPhotoRespVO> getItemPhotos(Long id, Long submissionDetailId, Long fqcItemId) {
        QmsFqcOrderDO order = validateCutRoundFqc(id);
        validateItemPhotoScope(order, submissionDetailId, fqcItemId);
        return buildItemPhotoRespList(qmsFqcItemPhotoMapper.selectListByItem(id, submissionDetailId, fqcItemId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcItemPhotoRespVO saveItemPhoto(QmsCutRoundFqcItemPhotoSaveReqVO reqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(reqVO.getFqcId());
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        ItemPhotoScope scope = validateItemPhotoScope(order, reqVO.getSubmissionDetailId(), reqVO.getFqcItemId());
        validateItemPhotoSample(reqVO.getSampleId(), scope);
        String photoUrl = StrUtil.trim(reqVO.getPhotoUrl());
        validateFqcPhotoUrl(photoUrl, "项目照片");
        String photoScene = StrUtil.blankToDefault(reqVO.getPhotoScene(), "RESULT").trim().toUpperCase(Locale.ROOT);
        if (!ITEM_PHOTO_SCENES.contains(photoScene)) {
            throw invalidParamException("项目照片场景只能是 RESULT、DEFECT 或 RECHECK");
        }
        List<QmsFqcItemPhotoDefectDO> photoDefects = normalizeItemPhotoDefects(reqVO);
        List<QmsFqcItemPhotoDO> existingPhotos = qmsFqcItemPhotoMapper.selectListByItem(
                order.getId(), scope.detail.getId(), scope.item.getId());
        QmsFqcItemPhotoDO photo = existingPhotos.stream()
                .filter(row -> photoUrl.equals(row.getPhotoUrl()))
                .findFirst()
                .orElse(null);
        QmsFqcItemPhotoDefectDO firstDefect = photoDefects.isEmpty() ? null : photoDefects.get(0);
        if (photo == null) {
            if (existingPhotos.size() >= ITEM_PHOTO_MAX_COUNT) {
                throw invalidParamException("检验项目【{}】最多上传{}张照片",
                        scope.item.getInspectionItem(), ITEM_PHOTO_MAX_COUNT);
            }
            photo = QmsFqcItemPhotoDO.builder()
                    .fqcId(order.getId())
                    .fqcNo(order.getFqcNo())
                    .submissionDetailId(scope.detail.getId())
                    .fqcItemId(scope.item.getId())
                    .sampleId(reqVO.getSampleId())
                    .productionBatchNo(scope.detail.getProductionBatchNo())
                    .inspectionItem(scope.item.getInspectionItem())
                    .photoUrl(photoUrl)
                    .photoScene(photoScene)
                    .defectCodeId(firstDefect == null ? null : firstDefect.getDefectCodeId())
                    .defectCode(firstDefect == null ? null : firstDefect.getDefectCode())
                    .remark(StrUtil.trim(reqVO.getRemark()))
                    .sort(reqVO.getSort() == null ? (existingPhotos.size() + 1) * 10 : reqVO.getSort())
                    .capturedById(SecurityFrameworkUtils.getLoginUserId())
                    .capturedByName(resolveLoginUserName())
                    .capturedTime(LocalDateTime.now())
                    .tenantId(scope.detail.getTenantId() == null ? order.getTenantId() : scope.detail.getTenantId())
                    .build();
            qmsFqcItemPhotoMapper.insert(photo);
        } else {
            photo.setSampleId(reqVO.getSampleId());
            photo.setPhotoScene(photoScene);
            photo.setDefectCodeId(firstDefect == null ? null : firstDefect.getDefectCodeId());
            photo.setDefectCode(firstDefect == null ? null : firstDefect.getDefectCode());
            photo.setRemark(StrUtil.trim(reqVO.getRemark()));
            if (reqVO.getSort() != null) {
                photo.setSort(reqVO.getSort());
            }
            qmsFqcItemPhotoMapper.updateById(photo);
        }
        replaceItemPhotoDefects(photo, scope, photoDefects);
        mergePhotoDefectsIntoPersistedSamples(order, scope.item, photoDefects);
        return buildItemPhotoResp(photo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItemPhoto(Long id, Long photoId) {
        QmsFqcOrderDO order = validateCutRoundFqc(id);
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        QmsFqcItemPhotoDO photo = qmsFqcItemPhotoMapper.selectById(photoId);
        if (photo == null || Boolean.TRUE.equals(photo.getDeleted()) || !Objects.equals(photo.getFqcId(), id)) {
            throw invalidParamException("项目照片不存在或不属于当前裁切成品检验单");
        }
        validateItemPhotoScope(order, photo.getSubmissionDetailId(), photo.getFqcItemId());
        qmsFqcItemPhotoDefectMapper.deletePhysicallyByPhotoId(photoId);
        qmsFqcItemPhotoMapper.deleteById(photoId);
    }

    private List<QmsFqcItemPhotoDefectDO> normalizeItemPhotoDefects(
            QmsCutRoundFqcItemPhotoSaveReqVO reqVO) {
        Map<Long, QmsFqcItemPhotoDefectDO> defectMap = new LinkedHashMap<>();
        List<QmsCutRoundFqcItemPhotoDefectReqVO> reqDefects = reqVO.getDefects();
        if ((reqDefects == null || reqDefects.isEmpty()) && reqVO.getDefectCodeId() != null) {
            QmsCutRoundFqcItemPhotoDefectReqVO legacyDefect = new QmsCutRoundFqcItemPhotoDefectReqVO();
            legacyDefect.setDefectCodeId(reqVO.getDefectCodeId());
            reqDefects = Collections.singletonList(legacyDefect);
        }
        if (reqDefects == null) {
            return Collections.emptyList();
        }
        for (int index = 0; index < reqDefects.size(); index++) {
            QmsCutRoundFqcItemPhotoDefectReqVO reqDefect = reqDefects.get(index);
            if (reqDefect == null || reqDefect.getDefectCodeId() == null) {
                throw invalidParamException("照片缺陷码不能为空");
            }
            if (defectMap.containsKey(reqDefect.getDefectCodeId())) {
                continue;
            }
            QmsDefectCodeDO defectCode = qmsDefectCodeMapper.selectById(reqDefect.getDefectCodeId());
            if (defectCode == null) {
                throw invalidParamException("照片关联的缺陷码不存在：" + reqDefect.getDefectCodeId());
            }
            if (Integer.valueOf(0).equals(defectCode.getStatus())
                    || "CATEGORY".equalsIgnoreCase(defectCode.getType())) {
                throw invalidParamException("照片只能关联已启用的明细缺陷码：" + defectCode.getCode());
            }
            defectMap.put(defectCode.getId(), QmsFqcItemPhotoDefectDO.builder()
                    .defectCodeId(defectCode.getId())
                    .defectCode(defectCode.getCode())
                    .defectName(defectCode.getName())
                    .defectLevel(defectCode.getLevel())
                    .sort(reqDefect.getSort() == null ? (index + 1) * 10 : reqDefect.getSort())
                    .build());
        }
        return new ArrayList<>(defectMap.values());
    }

    private void replaceItemPhotoDefects(QmsFqcItemPhotoDO photo, ItemPhotoScope scope,
                                         List<QmsFqcItemPhotoDefectDO> photoDefects) {
        qmsFqcItemPhotoDefectMapper.deletePhysicallyByPhotoId(photo.getId());
        if (photoDefects.isEmpty()) {
            return;
        }
        Long tenantId = scope.detail.getTenantId() == null ? photo.getTenantId() : scope.detail.getTenantId();
        for (QmsFqcItemPhotoDefectDO photoDefect : photoDefects) {
            photoDefect.setId(null);
            photoDefect.setItemPhotoId(photo.getId());
            photoDefect.setFqcId(photo.getFqcId());
            photoDefect.setSubmissionDetailId(photo.getSubmissionDetailId());
            photoDefect.setFqcItemId(photo.getFqcItemId());
            photoDefect.setTenantId(tenantId == null ? 0L : tenantId);
        }
        qmsFqcItemPhotoDefectMapper.insertBatch(photoDefects);
    }

    private List<QmsCutRoundFqcItemPhotoRespVO> buildItemPhotoRespList(List<QmsFqcItemPhotoDO> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> photoIds = photos.stream().map(QmsFqcItemPhotoDO::getId).collect(Collectors.toList());
        Map<Long, List<QmsFqcItemPhotoDefectDO>> defectMap = qmsFqcItemPhotoDefectMapper
                .selectListByPhotoIds(photoIds).stream()
                .collect(Collectors.groupingBy(QmsFqcItemPhotoDefectDO::getItemPhotoId,
                        LinkedHashMap::new, Collectors.toList()));
        List<QmsCutRoundFqcItemPhotoRespVO> result = new ArrayList<>();
        for (QmsFqcItemPhotoDO photo : photos) {
            QmsCutRoundFqcItemPhotoRespVO respVO = BeanUtils.toBean(photo, QmsCutRoundFqcItemPhotoRespVO.class);
            List<QmsCutRoundFqcItemPhotoDefectRespVO> defects = BeanUtils.toBean(
                    defectMap.getOrDefault(photo.getId(), Collections.emptyList()),
                    QmsCutRoundFqcItemPhotoDefectRespVO.class);
            if (defects.isEmpty() && photo.getDefectCodeId() != null) {
                QmsCutRoundFqcItemPhotoDefectRespVO legacyDefect = new QmsCutRoundFqcItemPhotoDefectRespVO();
                legacyDefect.setDefectCodeId(photo.getDefectCodeId());
                legacyDefect.setDefectCode(photo.getDefectCode());
                legacyDefect.setSort(10);
                defects = Collections.singletonList(legacyDefect);
            }
            respVO.setDefects(defects);
            result.add(respVO);
        }
        return result;
    }

    private QmsCutRoundFqcItemPhotoRespVO buildItemPhotoResp(QmsFqcItemPhotoDO photo) {
        return buildItemPhotoRespList(Collections.singletonList(photo)).get(0);
    }

    private void mergePhotoDefectsIntoIncomingSamples(QmsFqcOrderDO order, QmsFqcItemDO item,
                                                       List<QmsFqcSaveReqVO.FqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        List<QmsFqcItemPhotoDefectDO> photoDefects = distinctItemPhotoDefects(
                qmsFqcItemPhotoDefectMapper.selectListByItem(order.getId(), item.getId()));
        if (photoDefects.isEmpty()) {
            return;
        }
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            if (!isNgSample(sample)) {
                continue;
            }
            Map<String, QmsFqcSaveReqVO.FqcSampleDefect> mergedDefects = new LinkedHashMap<>();
            for (QmsFqcSaveReqVO.FqcSampleDefect defect : normalizeSampleDefects(sample)) {
                addSampleDefect(mergedDefects, defect);
            }
            for (QmsFqcItemPhotoDefectDO photoDefect : photoDefects) {
                addSampleDefect(mergedDefects, toSampleDefect(photoDefect));
            }
            List<QmsFqcSaveReqVO.FqcSampleDefect> defects = new ArrayList<>(mergedDefects.values());
            sample.setDefects(defects);
            if (!defects.isEmpty()) {
                sample.setDefectCode(defects.get(0).getDefectCode());
                sample.setDefectName(defects.get(0).getDefectName());
            }
        }
    }

    private void mergePhotoDefectsIntoPersistedSamples(QmsFqcOrderDO order, QmsFqcItemDO item,
                                                        List<QmsFqcItemPhotoDefectDO> photoDefects) {
        List<QmsFqcItemPhotoDefectDO> distinctPhotoDefects = distinctItemPhotoDefects(photoDefects);
        if (distinctPhotoDefects.isEmpty()) {
            return;
        }
        List<QmsFqcSampleDO> samples = qmsFqcSampleMapper.selectListByFqcIdAndItemIdsAndRole(
                order.getId(), Collections.singletonList(item.getId()), SAMPLE_ROLE_OPERATOR).stream()
                .filter(this::isNgPersistedSample)
                .collect(Collectors.toList());
        if (samples.isEmpty()) {
            return;
        }
        List<QmsFqcSampleDefectDO> existingDefects = qmsFqcSampleDefectMapper.selectListByFqcIdAndItemIds(
                order.getId(), Collections.singletonList(item.getId()));
        Map<Long, Set<String>> existingKeys = new LinkedHashMap<>();
        Map<Long, Integer> existingCounts = new LinkedHashMap<>();
        for (QmsFqcSampleDefectDO existingDefect : existingDefects) {
            existingKeys.computeIfAbsent(existingDefect.getSampleId(), key -> new LinkedHashSet<>())
                    .add(defectKey(existingDefect.getDefectCodeId(), existingDefect.getDefectCode(),
                            existingDefect.getDefectName()));
            existingCounts.merge(existingDefect.getSampleId(), 1, Integer::sum);
        }
        List<QmsFqcSampleDefectDO> rowsToInsert = new ArrayList<>();
        for (QmsFqcSampleDO sample : samples) {
            Set<String> sampleKeys = existingKeys.computeIfAbsent(sample.getId(), key -> new LinkedHashSet<>());
            int sortIndex = existingCounts.getOrDefault(sample.getId(), 0);
            for (QmsFqcItemPhotoDefectDO photoDefect : distinctPhotoDefects) {
                String key = defectKey(photoDefect.getDefectCodeId(), photoDefect.getDefectCode(),
                        photoDefect.getDefectName());
                if (!sampleKeys.add(key)) {
                    continue;
                }
                sortIndex++;
                QmsFqcSampleDefectDO defectDO = new QmsFqcSampleDefectDO();
                defectDO.setFqcId(order.getId());
                defectDO.setFqcNo(order.getFqcNo());
                defectDO.setFqcItemId(item.getId());
                defectDO.setSampleId(sample.getId());
                defectDO.setSampleSeq(sample.getSampleSeq());
                defectDO.setSamplePosition(sample.getSamplePosition());
                defectDO.setDefectCodeId(photoDefect.getDefectCodeId());
                defectDO.setDefectCode(photoDefect.getDefectCode());
                defectDO.setDefectName(photoDefect.getDefectName());
                defectDO.setDefectLevel(photoDefect.getDefectLevel());
                defectDO.setSort(sortIndex * 10);
                defectDO.setTenantId(order.getTenantId() == null ? 0L : order.getTenantId());
                rowsToInsert.add(defectDO);
            }
            if (StrUtil.isBlank(sample.getDefectCode()) && !distinctPhotoDefects.isEmpty()) {
                QmsFqcItemPhotoDefectDO firstDefect = distinctPhotoDefects.get(0);
                QmsFqcSampleDO updateSample = new QmsFqcSampleDO();
                updateSample.setId(sample.getId());
                updateSample.setDefectCode(firstDefect.getDefectCode());
                updateSample.setDefectName(firstDefect.getDefectName());
                qmsFqcSampleMapper.updateById(updateSample);
            }
        }
        if (!rowsToInsert.isEmpty()) {
            qmsFqcSampleDefectMapper.insertBatch(rowsToInsert);
        }
    }

    private boolean isNgPersistedSample(QmsFqcSampleDO sample) {
        return JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue());
    }

    private List<QmsFqcItemPhotoDefectDO> distinctItemPhotoDefects(
            List<QmsFqcItemPhotoDefectDO> photoDefects) {
        Map<String, QmsFqcItemPhotoDefectDO> defectMap = new LinkedHashMap<>();
        if (photoDefects != null) {
            for (QmsFqcItemPhotoDefectDO photoDefect : photoDefects) {
                if (photoDefect == null) {
                    continue;
                }
                defectMap.putIfAbsent(defectKey(photoDefect.getDefectCodeId(), photoDefect.getDefectCode(),
                        photoDefect.getDefectName()), photoDefect);
            }
        }
        return new ArrayList<>(defectMap.values());
    }

    private QmsFqcSaveReqVO.FqcSampleDefect toSampleDefect(QmsFqcItemPhotoDefectDO photoDefect) {
        QmsFqcSaveReqVO.FqcSampleDefect defect = new QmsFqcSaveReqVO.FqcSampleDefect();
        defect.setDefectCodeId(photoDefect.getDefectCodeId());
        defect.setDefectCode(photoDefect.getDefectCode());
        defect.setDefectName(photoDefect.getDefectName());
        defect.setDefectLevel(photoDefect.getDefectLevel());
        defect.setSort(photoDefect.getSort());
        return defect;
    }

    private String defectKey(Long defectCodeId, String defectCode, String defectName) {
        return defectCodeId == null ? firstNotBlank(defectCode, defectName, "") : String.valueOf(defectCodeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO saveProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(saveReqVO.getId());
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        syncActualValueRequiredSnapshot(order);
        List<QmsFqcItemDO> savedItems = saveCutRoundProgramItems(order, saveReqVO.getItems());
        List<Long> affectedDetailIds = resolveAffectedSubmissionDetailIds(savedItems);
        refreshSubmissionDetailsFromItems(saveReqVO.getId(), affectedDetailIds);
        refreshOrderItemProgress(saveReqVO.getId());
        refreshOrderSubmissionStats(saveReqVO.getId());
        refreshOrderDraftStatus(order, saveReqVO);
        syncCutRoundInspectionFromSubmissionDetails(saveReqVO.getId(), affectedDetailIds);
        return buildProgramEntryLightResp(saveReqVO.getId(), savedItems, affectedDetailIds);
    }

    private List<QmsFqcItemDO> saveCutRoundProgramItems(QmsFqcOrderDO order,
                                                        List<QmsFqcSaveReqVO.FqcItem> incomingItems) {
        if (incomingItems == null || incomingItems.isEmpty()) {
            throw invalidParamException("本次保存的检验项目不能为空");
        }
        Map<Long, QmsFqcSaveReqVO.FqcItem> incomingItemMap = new LinkedHashMap<>();
        List<QmsFqcItemDO> existingItems = new ArrayList<>();
        for (QmsFqcSaveReqVO.FqcItem incomingItem : incomingItems) {
            if (incomingItem == null || incomingItem.getId() == null) {
                throw invalidParamException("裁切成品检验项目必须携带项目ID，禁止按标准项兜底匹配");
            }
            if (incomingItemMap.putIfAbsent(incomingItem.getId(), incomingItem) != null) {
                throw invalidParamException("本次保存包含重复的检验项目ID：" + incomingItem.getId());
            }
            QmsFqcItemDO existingItem = qmsFqcItemMapper.selectById(incomingItem.getId());
            validateCutRoundIncomingItem(order, existingItem, incomingItem);
            mergePhotoDefectsIntoIncomingSamples(order, existingItem, incomingItem.getSamples());
            validateCutRoundIncomingSamples(existingItem, incomingItem.getSamples());
            existingItems.add(existingItem);
        }

        List<Long> itemIds = existingItems.stream().map(QmsFqcItemDO::getId).collect(Collectors.toList());
        qmsFqcSampleDefectMapper.deleteByFqcItemIds(itemIds);
        qmsFqcSampleMapper.deleteByFqcItemIds(itemIds);

        for (QmsFqcItemDO existingItem : existingItems) {
            QmsFqcSaveReqVO.FqcItem incomingItem = incomingItemMap.get(existingItem.getId());
            applyCutRoundActualValue(existingItem, incomingItem);
            applyCutRoundItemStats(existingItem, incomingItem.getSamples());
            qmsFqcItemMapper.updateById(existingItem);
            saveCutRoundSamples(order, existingItem, incomingItem.getSamples());
        }
        return existingItems;
    }

    private void validateCutRoundIncomingItem(QmsFqcOrderDO order, QmsFqcItemDO existingItem,
                                              QmsFqcSaveReqVO.FqcItem incomingItem) {
        if (existingItem == null || Boolean.TRUE.equals(existingItem.getDeleted())
                || !Objects.equals(existingItem.getFqcId(), order.getId())) {
            throw invalidParamException("检验项目不存在或不属于当前裁切成品检验单");
        }
        if (existingItem.getSubmissionDetailId() == null) {
            throw invalidParamException("裁切成品检验只允许保存片级检验项目");
        }
        if (incomingItem.getSubmissionDetailId() != null
                && !Objects.equals(incomingItem.getSubmissionDetailId(), existingItem.getSubmissionDetailId())) {
            throw invalidParamException("检验项目与送检明细不匹配");
        }
        if (incomingItem.getStandardItemId() != null
                && !Objects.equals(incomingItem.getStandardItemId(), existingItem.getStandardItemId())) {
            throw invalidParamException("检验项目与质量标准项不匹配");
        }
    }

    private void validateCutRoundIncomingSamples(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            throw invalidParamException("检验项目【" + itemDO.getInspectionItem() + "】样本不能为空");
        }
        int sampleSize = Math.max(1, itemDO.getSampleSize() == null ? 1 : itemDO.getSampleSize());
        if (samples.size() > sampleSize) {
            throw invalidParamException("检验项目【" + itemDO.getInspectionItem() + "】样本数量("
                    + samples.size() + ")不能超过取样数(" + sampleSize + ")");
        }
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            if (sample == null || sample.getSampleSeq() == null) {
                throw invalidParamException("检验项目【" + itemDO.getInspectionItem() + "】存在无样本序号的数据");
            }
        }
    }

    private void applyCutRoundActualValue(QmsFqcItemDO itemDO, QmsFqcSaveReqVO.FqcItem incomingItem) {
        itemDO.setActualValue(Boolean.TRUE.equals(itemDO.getActualValueRequired())
                ? trimToNull(incomingItem.getActualValue()) : null);
    }

    private void applyCutRoundItemStats(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        String result = calculateCutRoundItemResult(itemDO, samples);
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
        itemDO.setCompletedSampleCount((int) samples.stream().filter(this::hasCutRoundSampleValue).count());
        itemDO.setAbnormalSampleCount((int) samples.stream().filter(this::isNgSample).count());
        itemDO.setInputStatus(JUDGMENT_NG.equals(result) ? INPUT_STATUS_ABNORMAL
                : JUDGMENT_OK.equals(result) ? INPUT_STATUS_COMPLETE : INPUT_STATUS_FILLING);
    }

    private String calculateCutRoundItemResult(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        int sampleSize = Math.max(1, itemDO.getSampleSize() == null ? 1 : itemDO.getSampleSize());
        if (samples == null || samples.size() < sampleSize) {
            return JUDGMENT_PENDING;
        }
        boolean hasPending = false;
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            if (isNgSample(sample)) {
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

    private boolean hasCutRoundSampleValue(QmsFqcSaveReqVO.FqcSample sample) {
        return sample.getMeasuredValue() != null
                || sample.getResultValue() != null
                || StrUtil.isNotBlank(sample.getRawValuesJson())
                || StrUtil.isNotBlank(sample.getQualitativeValue())
                || JUDGMENT_OK.equals(sample.getSampleResult())
                || JUDGMENT_NG.equals(sample.getSampleResult());
    }

    private void saveCutRoundSamples(QmsFqcOrderDO order, QmsFqcItemDO itemDO,
                                     List<QmsFqcSaveReqVO.FqcSample> samples) {
        List<QmsFqcSampleDO> sampleList = new ArrayList<>();
        List<List<QmsFqcSaveReqVO.FqcSampleDefect>> sampleDefectList = new ArrayList<>();
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            boolean ngSample = isNgSample(sample);
            List<QmsFqcSaveReqVO.FqcSampleDefect> defects = normalizeSampleDefects(sample);
            if (ngSample && defects.isEmpty()) {
                throw invalidParamException("FQC NG样本【" + itemDO.getInspectionItem()
                        + "/样本" + sample.getSampleSeq() + "】必须选择缺陷码");
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
            sampleDO.setSampleRole(SAMPLE_ROLE_OPERATOR);
            sampleDO.setValueSource(StrUtil.blankToDefault(sample.getValueSource(), VALUE_SOURCE_MANUAL));
            sampleDO.setInputTime(sample.getInputTime() == null ? LocalDateTime.now() : sample.getInputTime());
            sampleDO.setSampleResult(normalizeSampleResult(sample));
            if (StrUtil.isBlank(sampleDO.getQualitativeValue())
                    && (JUDGMENT_OK.equals(sampleDO.getSampleResult()) || JUDGMENT_NG.equals(sampleDO.getSampleResult()))) {
                sampleDO.setQualitativeValue(sampleDO.getSampleResult());
            }
            if (defects.isEmpty()) {
                sampleDO.setDefectCode(null);
                sampleDO.setDefectName(null);
            } else {
                QmsFqcSaveReqVO.FqcSampleDefect firstDefect = defects.get(0);
                sampleDO.setDefectCode(firstNotBlank(firstDefect.getDefectCode(), sample.getDefectCode()));
                sampleDO.setDefectName(firstNotBlank(firstDefect.getDefectName(), sample.getDefectName()));
            }
            sampleList.add(sampleDO);
            sampleDefectList.add(defects);
        }
        qmsFqcSampleMapper.insertBatch(sampleList);
        saveCutRoundSampleDefects(order, itemDO, sampleList, sampleDefectList);
    }

    private String normalizeSampleResult(QmsFqcSaveReqVO.FqcSample sample) {
        String value = firstNotBlank(sample.getSampleResult(), sample.getQualitativeValue(), JUDGMENT_PENDING);
        value = value.trim().toUpperCase();
        if (!JUDGMENT_OK.equals(value) && !JUDGMENT_NG.equals(value) && !JUDGMENT_PENDING.equals(value)) {
            throw invalidParamException("样本判定只能是 OK、NG 或 PENDING");
        }
        return value;
    }

    private boolean isNgSample(QmsFqcSaveReqVO.FqcSample sample) {
        return JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue());
    }

    private List<QmsFqcSaveReqVO.FqcSampleDefect> normalizeSampleDefects(QmsFqcSaveReqVO.FqcSample sample) {
        Map<String, QmsFqcSaveReqVO.FqcSampleDefect> defectMap = new LinkedHashMap<>();
        if (sample.getDefects() != null) {
            for (QmsFqcSaveReqVO.FqcSampleDefect defect : sample.getDefects()) {
                addSampleDefect(defectMap, defect);
            }
        }
        if (defectMap.isEmpty() && StrUtil.isNotBlank(sample.getDefectCode())) {
            QmsFqcSaveReqVO.FqcSampleDefect defect = new QmsFqcSaveReqVO.FqcSampleDefect();
            defect.setDefectCode(sample.getDefectCode());
            defect.setDefectName(sample.getDefectName());
            addSampleDefect(defectMap, defect);
        }
        return new ArrayList<>(defectMap.values());
    }

    private void addSampleDefect(Map<String, QmsFqcSaveReqVO.FqcSampleDefect> defectMap,
                                 QmsFqcSaveReqVO.FqcSampleDefect defect) {
        if (defect == null || (defect.getDefectCodeId() == null
                && StrUtil.isBlank(defect.getDefectCode())
                && StrUtil.isBlank(defect.getDefectName()))) {
            return;
        }
        String key = defect.getDefectCodeId() == null
                ? firstNotBlank(defect.getDefectCode(), defect.getDefectName(), "")
                : String.valueOf(defect.getDefectCodeId());
        defectMap.putIfAbsent(key, defect);
    }

    private void saveCutRoundSampleDefects(QmsFqcOrderDO order, QmsFqcItemDO itemDO,
                                           List<QmsFqcSampleDO> sampleList,
                                           List<List<QmsFqcSaveReqVO.FqcSampleDefect>> sampleDefectList) {
        List<QmsFqcSampleDefectDO> defectList = new ArrayList<>();
        for (int sampleIndex = 0; sampleIndex < sampleList.size(); sampleIndex++) {
            QmsFqcSampleDO sampleDO = sampleList.get(sampleIndex);
            List<QmsFqcSaveReqVO.FqcSampleDefect> defects = sampleDefectList.get(sampleIndex);
            for (int defectIndex = 0; defectIndex < defects.size(); defectIndex++) {
                QmsFqcSaveReqVO.FqcSampleDefect defect = defects.get(defectIndex);
                QmsFqcSampleDefectDO defectDO = new QmsFqcSampleDefectDO();
                defectDO.setFqcId(order.getId());
                defectDO.setFqcNo(order.getFqcNo());
                defectDO.setFqcItemId(itemDO.getId());
                defectDO.setSampleId(sampleDO.getId());
                defectDO.setSampleSeq(sampleDO.getSampleSeq());
                defectDO.setSamplePosition(sampleDO.getSamplePosition());
                defectDO.setDefectCodeId(defect.getDefectCodeId());
                defectDO.setDefectCode(defect.getDefectCode());
                defectDO.setDefectName(defect.getDefectName());
                defectDO.setDefectLevel(defect.getDefectLevel());
                defectDO.setSort(defect.getSort() == null ? (defectIndex + 1) * 10 : defect.getSort());
                defectDO.setTenantId(order.getTenantId() == null ? 0L : order.getTenantId());
                defectList.add(defectDO);
            }
        }
        if (!defectList.isEmpty()) {
            qmsFqcSampleDefectMapper.insertBatch(defectList);
        }
    }

    private void refreshOrderDraftStatus(QmsFqcOrderDO order, QmsFqcSaveReqVO saveReqVO) {
        LocalDateTime now = LocalDateTime.now();
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(order.getId());
        updateObj.setStatus(STATUS_INSPECTING);
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setEntryMode(StrUtil.blankToDefault(saveReqVO.getEntryMode(), ENTRY_MODE_MANUAL));
        updateObj.setCurrentStepCode(StrUtil.blankToDefault(saveReqVO.getCurrentStepCode(), order.getCurrentStepCode()));
        updateObj.setSheetLocked(false);
        updateObj.setLastSaveTime(now);
        if (hasCutRoundSavedSampleValue(saveReqVO.getItems())) {
            updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setInspectorName(resolveLoginUserName());
            updateObj.setInspectionTime(now);
        }
        qmsFqcOrderMapper.updateById(updateObj);
    }

    private boolean hasCutRoundSavedSampleValue(List<QmsFqcSaveReqVO.FqcItem> items) {
        return items != null && items.stream()
                .filter(Objects::nonNull)
                .map(QmsFqcSaveReqVO.FqcItem::getSamples)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .anyMatch(this::hasCutRoundSampleValue);
    }

    private List<Long> resolveAffectedSubmissionDetailIds(List<QmsFqcItemDO> savedItems) {
        return (savedItems == null ? Collections.<QmsFqcItemDO>emptyList() : savedItems).stream()
                .map(QmsFqcItemDO::getSubmissionDetailId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private QmsCutRoundFqcRespVO buildProgramEntryLightResp(Long fqcId, List<QmsFqcItemDO> savedItems,
                                                            Collection<Long> affectedDetailIds) {
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(fqcId);
        QmsCutRoundFqcRespVO respVO = BeanUtils.toBean(order, QmsCutRoundFqcRespVO.class);
        fillRecheckSource(respVO);
        List<QmsCutRoundFqcRespVO.SubmissionDetail> detailRespList = BeanUtils.toBean(
                qmsFqcSubmissionDetailMapper.selectListByIds(affectedDetailIds),
                QmsCutRoundFqcRespVO.SubmissionDetail.class);
        fillSubmissionDetailStats(detailRespList,
                qmsFqcItemMapper.selectListByFqcIdAndSubmissionDetailIds(fqcId, affectedDetailIds));
        respVO.setSubmissionDetails(detailRespList);
        respVO.setItems(BeanUtils.toBean(savedItems, QmsFqcRespVO.FqcItem.class));
        respVO.setAbnormals(Collections.emptyList());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO submitProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(saveReqVO.getId());
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        syncActualValueRequiredSnapshot(order);
        saveCutRoundProgramItems(order, saveReqVO.getItems());
        refreshSubmissionDetailsFromItems(saveReqVO.getId());
        refreshOrderItemProgress(saveReqVO.getId());
        refreshOrderSubmissionStats(saveReqVO.getId());
        List<QmsFqcSubmissionDetailDO> details = qmsFqcSubmissionDetailMapper.selectListByFqcId(saveReqVO.getId());
        validateInspectionItemsCompleted(saveReqVO.getId(), details);
        validateRequiredActualValues(saveReqVO.getId());
        validateSubmissionDetailsCompleted(details);
        // 送检时间和送检人是裁切工序发起报检时的原始追溯信息，提交检测结果不得覆盖。
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, saveReqVO.getId())
                .set(QmsFqcOrderDO::getStatus, STATUS_WAITING_QA)
                .set(QmsFqcOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsFqcOrderDO::getReleaseResult, null)
                .set(QmsFqcOrderDO::getSheetLocked, true));
        syncCutRoundInspectionFromSubmissionDetails(saveReqVO.getId());
        return get(saveReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO saveSubmissionDetails(QmsCutRoundFqcSubmissionDetailSaveReqVO reqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(reqVO.getFqcId());
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        LocalDateTime now = LocalDateTime.now();
        for (QmsCutRoundFqcSubmissionDetailSaveReqVO.Detail detailReq : reqVO.getDetails()) {
            QmsFqcSubmissionDetailDO detail = qmsFqcSubmissionDetailMapper.selectById(detailReq.getId());
            if (detail == null || Boolean.TRUE.equals(detail.getDeleted())
                    || !Objects.equals(detail.getFqcId(), order.getId())) {
                throw invalidParamException("送检明细不存在或不属于当前FQC单");
            }
            String rowJudgment = normalizeRowJudgment(detailReq.getRowJudgment());
            qmsFqcSubmissionDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcSubmissionDetailDO>()
                    .eq(QmsFqcSubmissionDetailDO::getId, detail.getId())
                    .set(QmsFqcSubmissionDetailDO::getRowJudgment, rowJudgment)
                    .set(QmsFqcSubmissionDetailDO::getDefectCode, detailReq.getDefectCode())
                    .set(QmsFqcSubmissionDetailDO::getDefectName, detailReq.getDefectName())
                    .set(QmsFqcSubmissionDetailDO::getNgReason, detailReq.getNgReason())
                    .set(QmsFqcSubmissionDetailDO::getRemark, detailReq.getRemark())
                    .set(QmsFqcSubmissionDetailDO::getInspectorId, SecurityFrameworkUtils.getLoginUserId())
                    .set(QmsFqcSubmissionDetailDO::getInspectorName, resolveLoginUserName())
                    .set(QmsFqcSubmissionDetailDO::getInspectionTime, JUDGMENT_PENDING.equals(rowJudgment) ? null : now));
        }
        refreshOrderSubmissionStats(order.getId());
        syncCutRoundInspectionFromSubmissionDetails(order.getId());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO saveSubmissionDetailPhotos(QmsCutRoundFqcSubmissionDetailPhotoSaveReqVO reqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(reqVO.getFqcId());
        validateCutRoundFqcEditable(order);
        assertCutRoundFqcSelfCheckReady();
        QmsFqcSubmissionDetailDO detail = qmsFqcSubmissionDetailMapper.selectById(reqVO.getSubmissionDetailId());
        if (detail == null || Boolean.TRUE.equals(detail.getDeleted())
                || !Objects.equals(detail.getFqcId(), order.getId())) {
            throw invalidParamException("送检明细不存在或不属于当前FQC单");
        }
        QmsFqcSubmissionDetailDO updateObj = new QmsFqcSubmissionDetailDO();
        updateObj.setId(detail.getId());
        updateObj.setPhotoUrls(normalizeSubmissionDetailPhotoUrls(reqVO.getPhotoUrls()));
        qmsFqcSubmissionDetailMapper.updateById(updateObj);
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO audit(QmsFqcAuditReqVO auditReqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(auditReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus())) {
            throw invalidParamException("裁切成品检验单提交检测结果后才允许审核");
        }
        String auditResult = validateAuditResult(auditReqVO);
        if (AUDIT_REJECT.equals(auditResult)) {
            return returnForRework(order, requireRejectReason(auditReqVO.getRejectReason()));
        }
        refreshSubmissionDetailsFromItems(order.getId());
        refreshOrderSubmissionStats(order.getId());
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(order.getId());
        List<QmsFqcSubmissionDetailDO> details = qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId());
        validateInspectionItemsCompleted(items, details);
        validateSubmissionDetailsCompleted(details);
        boolean hasNgItem = items.stream().anyMatch(this::isNgItem);
        boolean hasNgDetail = details.stream().anyMatch(detail -> JUDGMENT_NG.equals(detail.getRowJudgment()));
        if (hasNgItem && !hasNgDetail) {
            throw invalidParamException("检验项目存在NG，但未汇总到片级判定，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        String finalJudgment = hasNgDetail ? JUDGMENT_NG : JUDGMENT_OK;
        String releaseResult = hasNgDetail ? RELEASE_PIECE_CONTROLLED : RELEASE_ALLOW_INBOUND;
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, order.getId())
                .set(QmsFqcOrderDO::getStatus, STATUS_COMPLETED)
                .set(QmsFqcOrderDO::getJudgment, finalJudgment)
                .set(QmsFqcOrderDO::getQaInspectorId, SecurityFrameworkUtils.getLoginUserId())
                .set(QmsFqcOrderDO::getQaInspectorName, resolveLoginUserName())
                .set(QmsFqcOrderDO::getQaTime, now)
                .set(QmsFqcOrderDO::getReleaseResult, releaseResult)
                .set(QmsFqcOrderDO::getReleaseTime, now)
                .set(QmsFqcOrderDO::getSheetLocked, true)
                .set(QmsFqcOrderDO::getRelatedNcrNo, null)
                .set(QmsFqcOrderDO::getNcrStatus, null));
        qmsFqcOrderMapper.clearAuditNotifyTime(order.getId());
        refreshOrderSubmissionStats(order.getId());
        syncCutRoundInspectionFromSubmissionDetails(order.getId());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcRespVO revokeAudit(QmsFqcAuditRevokeReqVO revokeReqVO) {
        QmsFqcOrderDO order = validateCutRoundFqc(revokeReqVO.getId());
        if (!STATUS_COMPLETED.equals(order.getStatus())) {
            throw invalidParamException("仅已完成的裁切成品检验单允许撤销审核");
        }
        if (StrUtil.isNotBlank(order.getRelatedNcrNo())) {
            throw invalidParamException("当前FQC单已关联NCR，不能直接撤销审核");
        }
        String reason = StrUtil.trim(revokeReqVO.getRevokeReason());
        if (StrUtil.isBlank(reason)) {
            throw invalidParamException("撤销审核原因不能为空");
        }
        List<QmsFqcSubmissionDetailDO> details = qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId());
        validateAuditRevokeDownstream(details);

        LocalDateTime now = LocalDateTime.now();
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, order.getId())
                .set(QmsFqcOrderDO::getStatus, STATUS_WAITING_QA)
                .set(QmsFqcOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsFqcOrderDO::getQaInspectorId, null)
                .set(QmsFqcOrderDO::getQaInspectorName, null)
                .set(QmsFqcOrderDO::getQaTime, null)
                .set(QmsFqcOrderDO::getReleaseResult, null)
                .set(QmsFqcOrderDO::getReleaseTime, null)
                .set(QmsFqcOrderDO::getSheetLocked, true)
                .set(QmsFqcOrderDO::getLastReturnReason, reason)
                .set(QmsFqcOrderDO::getReturnCount, (order.getReturnCount() == null ? 0 : order.getReturnCount()) + 1));
        qmsFqcOrderMapper.clearAuditNotifyTime(order.getId());
        syncCutRoundInspectionFromSubmissionDetails(order.getId());
        qmsFqcReturnRecordMapper.insert(QmsFqcReturnRecordDO.builder()
                .fqcId(order.getId())
                .fqcNo(order.getFqcNo())
                .returnStepCode("AUDIT_REVOKE")
                .returnReason(reason)
                .returnUserId(SecurityFrameworkUtils.getLoginUserId())
                .returnUserName(resolveLoginUserName())
                .returnTime(now)
                .beforeStatus(STATUS_COMPLETED)
                .afterStatus(STATUS_WAITING_QA)
                .build());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyNcrDispositionResult(QmsNcRecordDO ncr,
                                          QmsNcDispositionExecutionDO execution,
                                          List<QmsNcDispositionScopeDO> scopes) {
        if (ncr == null || execution == null || scopes == null || scopes.isEmpty()
                || ncr.getSourceId() == null
                || !SOURCE_MODULE_CUT_ROUND_FQC.equals(normalizeUpper(ncr.getSourceBizType()))) {
            return;
        }
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(ncr.getSourceId());
        if (order == null || !SOURCE_MODULE_CUT_ROUND_FQC.equals(order.getSourceModule())) {
            return;
        }
        String dispositionType = normalizeUpper(firstNotBlank(execution.getDispositionType(), ncr.getFinalDisposition()));
        if (StrUtil.isBlank(dispositionType)) {
            return;
        }
        List<QmsFqcSubmissionDetailDO> details = qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId());
        if (details.isEmpty()) {
            return;
        }
        Map<String, QmsFqcSubmissionDetailDO> detailByPieceNo = details.stream()
                .filter(detail -> StrUtil.isNotBlank(detail.getProductionBatchNo()))
                .collect(Collectors.toMap(detail -> normalizePieceNo(detail.getProductionBatchNo()),
                        detail -> detail, (left, right) -> left, LinkedHashMap::new));
        Map<Long, HcCutRoundReportDO> reportById = loadCutRoundReports(details);
        LocalDateTime now = LocalDateTime.now();
        Set<Long> affectedTaskIds = new LinkedHashSet<>();
        for (QmsNcDispositionScopeDO scope : scopes) {
            if (scope == null || !"PIECE".equals(normalizeUpper(scope.getScopeLevel()))) {
                continue;
            }
            QmsFqcSubmissionDetailDO detail = detailByPieceNo.get(normalizePieceNo(
                    firstNotBlank(scope.getPieceNo(), scope.getSourceObjectNo())));
            if (detail == null) {
                continue;
            }
            String effectiveDisposition = resolveEffectiveDisposition(
                    firstNotBlank(scope.getDispositionType(), dispositionType), scope.getScopeRole());
            String dispositionRemark = buildNcrDispositionRemark(ncr, execution, scope, effectiveDisposition);
            String rowJudgment = isNcrDispositionReleased(effectiveDisposition) ? JUDGMENT_OK : JUDGMENT_NG;
            boolean reopenReport = isNcrDispositionReopen(effectiveDisposition);
            applySubmissionDetailDisposition(detail, rowJudgment, dispositionRemark);
            applyInspectionDetailDisposition(order, detail, rowJudgment, dispositionRemark, now);
            applyCutRoundReportDisposition(detail, reportById.get(detail.getCutRoundReportId()),
                    rowJudgment, reopenReport, dispositionRemark, now);
            if (detail.getCutRoundInspectionTaskId() != null) {
                affectedTaskIds.add(detail.getCutRoundInspectionTaskId());
            }
        }
        refreshOrderSubmissionStats(order.getId());
        affectedTaskIds.forEach(this::refreshCutRoundInspectionTaskStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCutRoundFqcScanRespVO resolveScan(QmsFqcScanReqVO scanReqVO) {
        String scanCode = StrUtil.trimToEmpty(scanReqVO.getScanCode());
        ScanResolution scanResolution = resolveScanMatches(scanReqVO, scanCode);
        QmsCutRoundFqcScanRespVO respVO = new QmsCutRoundFqcScanRespVO();
        respVO.setScanCode(scanCode);
        respVO.setScanTargetType(scanResolution.targetType);
        respVO.setOpenTarget(OPEN_TARGET_WORKBENCH);
        respVO.setCandidateCount(scanResolution.matches.size());
        if (scanResolution.matches.isEmpty()) {
            respVO.setMatchResult(SCAN_RESULT_NOT_FOUND);
            respVO.setMessage("未找到裁切成品检验送检明细");
            insertScanRecord(scanReqVO, null, null, null, scanResolution.targetType,
                    SCAN_RESULT_NOT_FOUND, 0, null, "未找到裁切成品检验送检明细");
            return respVO;
        }
        if (scanResolution.matches.size() > 1) {
            respVO.setMatchResult(SCAN_RESULT_MATCHED_MULTIPLE);
            respVO.setMessage("匹配到 " + scanResolution.matches.size() + " 条裁切送检片号，请选择后进入检验");
            respVO.setCandidates(scanResolution.matches.stream()
                    .map(this::buildScanCandidate)
                    .collect(Collectors.toList()));
            insertScanRecord(scanReqVO, null, null, scanResolution.matches.get(0).order.getTenantId(),
                    scanResolution.targetType,
                    SCAN_RESULT_MATCHED_MULTIPLE, scanResolution.matches.size(),
                    buildScanCandidateIds(scanResolution.matches), "需要人工选择送检片号");
            return respVO;
        }
        ScanMatch scanMatch = scanResolution.matches.get(0);
        respVO.setMatchResult(SCAN_RESULT_MATCHED_SINGLE);
        respVO.setMessage(scanMatch.message);
        respVO.setRecord(get(scanMatch.order.getId()));
        if (scanMatch.detail != null) {
            respVO.setDetail(BeanUtils.toBean(scanMatch.detail, QmsCutRoundFqcRespVO.SubmissionDetail.class));
        }
        insertScanRecord(scanReqVO, scanMatch.order, scanMatch.detail, null, scanMatch.targetType,
                SCAN_RESULT_MATCHED_SINGLE, 1, buildScanCandidateIds(Collections.singletonList(scanMatch)), null);
        return respVO;
    }

    @Override
    public QmsFqcSubmissionDetailDO getPackagingSubmissionDetail(String productionBatchNo) {
        return qmsFqcSubmissionDetailMapper.selectLatestByProductionBatchNo(StrUtil.trimToEmpty(productionBatchNo));
    }

    private void assertCutRoundFqcSelfCheckReady() {
        LocalDate today = LocalDate.now();
        List<String> missingNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_NAMES.entrySet()) {
            boolean confirmed = hcProcessFormRecordMapper.existsConfirmedByTemplateCodeAndRecordDate(
                    entry.getKey(), today);
            if (!confirmed) {
                missingNames.add(entry.getValue());
            }
        }
        if (!missingNames.isEmpty()) {
            throw invalidParamException("今天尚未确认" + String.join("、", missingNames) + "，不能进行裁切成品检验录入");
        }
    }

    private QmsFqcSaveReqVO buildCreateReq(HcPlanOrderDO planOrder, HcPlanOrderOperationDO operation,
                                           HcCutRoundInspectionTaskDO task,
                                           List<HcCutRoundInspectionDetailDO> details,
                                           Map<Long, HcCutRoundReportDO> reportMap) {
        HcCutRoundInspectionDetailDO firstDetail = details.get(0);
        HcCutRoundReportDO firstReport = reportMap.get(firstDetail.getCutRoundReportId());
        QmsFqcSaveReqVO reqVO = new QmsFqcSaveReqVO();
        reqVO.setReportNo(task.getTaskNo());
        reqVO.setWorkOrderNo(firstNotBlank(planOrder.getPlanNo(), task.getPlanNo(), task.getTaskNo()));
        reqVO.setSourceReportId(task.getId());
        reqVO.setSourceReportNo(task.getTaskNo());
        reqVO.setSourceModule(SOURCE_MODULE_CUT_ROUND_FQC);
        reqVO.setSourceOperationCode(operation.getOpCode());
        reqVO.setSourceOperationName(operation.getOpName());
        reqVO.setPlanOrderId(planOrder.getId());
        reqVO.setOperationCode(operation.getOpCode());
        reqVO.setOperationName(operation.getOpName());
        reqVO.setMachineId(operation.getEquipmentId());
        reqVO.setMachineCode(operation.getEquipmentCode());
        reqVO.setMachineName(operation.getEquipmentName());
        reqVO.setMaterialId(planOrder.getMaterialId());
        reqVO.setMaterialCode(firstNotBlank(firstDetail.getMaterialCode(), firstReport == null ? null : firstReport.getMaterialCode(), planOrder.getMaterialCode()));
        reqVO.setMaterialName(firstNotBlank(firstDetail.getMaterialName(), firstReport == null ? null : firstReport.getMaterialName(), planOrder.getMaterialName()));
        reqVO.setSpecification(firstNotBlank(firstDetail.getSizeRule(), planOrder.getSizeSpec(), planOrder.getSizeName()));
        reqVO.setProductModel(firstNotBlank(firstDetail.getModelCode(), firstReport == null ? null : firstReport.getModelCode(), planOrder.getModelCode(), planOrder.getModelName()));
        reqVO.setProductBatchNo(firstNotBlank(firstDetail.getParentProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()));
        reqVO.setBatchNo(firstNotBlank(firstDetail.getParentProductionBatchNo(), firstDetail.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()));
        reqVO.setProduceQty(BigDecimal.valueOf(details.size()));
        reqVO.setUnitCode(firstNotBlank(planOrder.getTargetUnitCode(), operation.getUnitCode(), "PCS"));
        reqVO.setUnitName(firstNotBlank(planOrder.getTargetUnitName(), operation.getUnitName(), operation.getUom(), "片"));
        reqVO.setSampleQty(1);
        reqVO.setInspectionCategory("FINAL");
        reqVO.setSubmissionType(resolveFqcSubmissionType(planOrder));
        reqVO.setSubmissionTime(firstValidTime(task.getReportTime(), task.getCreateTime(), LocalDateTime.now()));
        reqVO.setSubmitterName(firstNotBlank(task.getReporterName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        reqVO.setStatus(STATUS_PENDING);
        reqVO.setJudgment(JUDGMENT_PENDING);
        reqVO.setRemark("来源裁切报检任务：" + task.getTaskNo() + "；送检片数：" + details.size());
        return reqVO;
    }

    private void upsertSubmissionDetails(QmsFqcOrderDO order, HcPlanOrderDO planOrder, HcPlanOrderOperationDO operation,
                                         HcCutRoundInspectionTaskDO task, List<HcCutRoundInspectionDetailDO> details,
                                         Map<Long, HcCutRoundReportDO> reportMap) {
        for (HcCutRoundInspectionDetailDO detail : details) {
            if (detail.getProductionBatchNo() == null
                    || qmsFqcSubmissionDetailMapper.selectByFqcIdAndProductionBatchNo(order.getId(), detail.getProductionBatchNo()) != null) {
                continue;
            }
            HcCutRoundReportDO report = reportMap.get(detail.getCutRoundReportId());
            FqcAutoNgSuggestion autoNgSuggestion = resolveAutoNgSuggestion(detail, report);
            QmsFqcSubmissionDetailDO detailDO = QmsFqcSubmissionDetailDO.builder()
                    .tenantId(detail.getTenantId() == null ? order.getTenantId() : detail.getTenantId())
                    .fqcId(order.getId())
                    .fqcNo(order.getFqcNo())
                    .cutRoundInspectionTaskId(task.getId())
                    .cutRoundInspectionTaskNo(task.getTaskNo())
                    .cutRoundInspectionDetailId(detail.getId())
                    .cutRoundReportId(detail.getCutRoundReportId())
                    .seqNo(detail.getSeqNo())
                    .planId(planOrder.getId())
                    .planNo(planOrder.getPlanNo())
                    .planOperationId(operation.getId())
                    .operationCode(operation.getOpCode())
                    .operationName(operation.getOpName())
                    .materialCode(firstNotBlank(detail.getMaterialCode(), report == null ? null : report.getMaterialCode()))
                    .materialName(firstNotBlank(detail.getMaterialName(), report == null ? null : report.getMaterialName()))
                    .modelCode(firstNotBlank(detail.getModelCode(), report == null ? null : report.getModelCode()))
                    .sizeRule(detail.getSizeRule())
                    .productionBatchNo(detail.getProductionBatchNo())
                    .parentProductionBatchNo(detail.getParentProductionBatchNo())
                    .qualityRiskFlag(detail.getQualityRiskFlag())
                    .qualityRiskSnapshotJson(detail.getQualityRiskSnapshotJson())
                    // 裁切已确认 NG 时，先把该缺陷带入片级送检明细。FQC 逐项复检为 OK 后会回归 OK，
                    // 因此这不是最终放行结论，也不会把粘胶2来源 NG 直接伪造成 FQC NG。
                    .rowJudgment(autoNgSuggestion.cutRoundNg() ? JUDGMENT_NG : JUDGMENT_PENDING)
                    .defectCode(autoNgSuggestion.defectCode())
                    .defectName(autoNgSuggestion.defectName())
                    .ngReason(autoNgSuggestion.ngReason())
                    .build();
            qmsFqcSubmissionDetailMapper.insert(detailDO);
        }
    }

    private FqcAutoNgSuggestion resolveAutoNgSuggestion(HcCutRoundInspectionDetailDO inspectionDetail,
                                                         HcCutRoundReportDO report) {
        Map<String, Object> snapshot = parseRiskSnapshot(firstNotBlank(
                inspectionDetail == null ? null : inspectionDetail.getQualityRiskSnapshotJson(),
                report == null ? null : report.getQualityRiskSnapshotJson()));
        boolean cutRoundNg = Boolean.TRUE.equals(snapshot.get("cutRoundNg"))
                || isNgText(snapshot.get("cutRoundSelfCheck"))
                || (report != null && isNgText(report.getSelfCheck()));
        if (!cutRoundNg) {
            return FqcAutoNgSuggestion.none();
        }
        List<Map<String, String>> defectItems = parseSnapshotDefectItems(snapshot.get("cutRoundDefectItems"));
        String fallbackCode = firstNotBlank(stringValue(snapshot.get("cutRoundDefectCode")),
                report == null ? null : report.getDefectCode());
        String fallbackReason = firstNotBlank(stringValue(snapshot.get("cutRoundReason")),
                report == null ? null : report.getRemark(), "裁切自检NG");
        if (defectItems.isEmpty()) {
            defectItems = List.of(buildDefectItem(fallbackCode, fallbackReason, fallbackReason));
        }
        Map<String, String> firstDefect = defectItems.get(0);
        String summary = defectItems.stream()
                .map(item -> firstNotBlank(item.get("defectName"), item.get("defectCode"), item.get("remark")))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("；"));
        return new FqcAutoNgSuggestion(true,
                firstNotBlank(firstDefect.get("defectCode"), fallbackCode),
                firstNotBlank(firstDefect.get("defectName"), fallbackReason, fallbackCode),
                "裁切外观检验自动带入：" + firstNotBlank(summary, fallbackReason));
    }

    private Map<String, Object> parseRiskSnapshot(String snapshotJson) {
        if (StrUtil.isBlank(snapshotJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> snapshot = JsonUtils.parseObjectQuietly(snapshotJson, MAP_TYPE);
        return snapshot == null ? Collections.emptyMap() : snapshot;
    }

    private List<Map<String, String>> parseSnapshotDefectItems(Object value) {
        if (!(value instanceof List<?> rows)) {
            return Collections.emptyList();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> item)) {
                continue;
            }
            String defectCode = firstNotBlank(stringValue(item.get("defectCode")), stringValue(item.get("code")));
            String defectName = firstNotBlank(stringValue(item.get("defectName")), stringValue(item.get("itemName")),
                    stringValue(item.get("name")), defectCode);
            String remark = firstNotBlank(stringValue(item.get("remark")), stringValue(item.get("reason")));
            if (StrUtil.isNotBlank(firstNotBlank(defectCode, defectName, remark))) {
                result.add(buildDefectItem(defectCode, defectName, remark));
            }
        }
        return result;
    }

    private Map<String, String> buildDefectItem(String defectCode, String defectName, String remark) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("defectCode", defectCode);
        item.put("defectName", defectName);
        item.put("remark", remark);
        return item;
    }

    private boolean isNgText(Object value) {
        String text = StrUtil.trimToEmpty(stringValue(value)).toUpperCase();
        return List.of("NG", "N", "FALSE", "ABNORMAL", "FAIL", "FAILED", "不合格", "异常").contains(text)
                || text.startsWith("NG") || text.contains("_NG");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void ensurePieceInspectionItems(Long fqcId) {
        List<QmsFqcSubmissionDetailDO> details = qmsFqcSubmissionDetailMapper.selectListByFqcId(fqcId);
        if (details.isEmpty()) {
            return;
        }
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(fqcId);
        List<QmsFqcItemDO> templateItems = items.stream()
                .filter(item -> item.getSubmissionDetailId() == null)
                .collect(Collectors.toList());
        if (templateItems.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcItemDO>> itemMap = items.stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcItemDO::getSubmissionDetailId));
        for (QmsFqcSubmissionDetailDO detail : details) {
            if (!itemMap.getOrDefault(detail.getId(), Collections.emptyList()).isEmpty()) {
                continue;
            }
            for (QmsFqcItemDO templateItem : templateItems) {
                qmsFqcItemMapper.insert(buildPieceItem(templateItem, detail));
            }
        }
        List<Long> templateItemIds = templateItems.stream()
                .map(QmsFqcItemDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        qmsFqcSampleDefectMapper.deleteByFqcItemIds(templateItemIds);
        qmsFqcSampleMapper.deleteByFqcItemIds(templateItemIds);
        templateItemIds.forEach(qmsFqcItemMapper::deleteById);
    }

    private QmsFqcItemDO buildPieceItem(QmsFqcItemDO templateItem, QmsFqcSubmissionDetailDO detail) {
        QmsFqcItemDO item = BeanUtils.toBean(templateItem, QmsFqcItemDO.class);
        item.setId(null);
        item.setSubmissionDetailId(detail.getId());
        item.setCutRoundInspectionDetailId(detail.getCutRoundInspectionDetailId());
        item.setSliceSeqNo(detail.getSeqNo());
        item.setProductionBatchNo(detail.getProductionBatchNo());
        item.setParentProductionBatchNo(detail.getParentProductionBatchNo());
        item.setItemResult(JUDGMENT_PENDING);
        item.setActualValue(null);
        item.setOperatorMax(null);
        item.setOperatorMin(null);
        item.setOperatorAvg(null);
        item.setOperatorResult(JUDGMENT_PENDING);
        item.setOperatorId(null);
        item.setOperatorName(null);
        item.setOperatorTime(null);
        item.setQaMax(null);
        item.setQaMin(null);
        item.setQaAvg(null);
        item.setQaResult(JUDGMENT_PENDING);
        item.setQaInspectorId(null);
        item.setQaInspectorName(null);
        item.setQaTime(null);
        item.setCalculatedAvg(null);
        item.setCalculatedStd(null);
        item.setCalculatedMin(null);
        item.setCalculatedMax(null);
        item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0);
        item.setInputStatus("EMPTY");
        return item;
    }

    private void syncActualValueRequiredSnapshot(QmsFqcOrderDO order) {
        if (!shouldSyncActualValueRequired(order)) {
            return;
        }
        QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(order.getStandardId());
        if (standard == null || !STANDARD_STATUS_ENABLED.equals(standard.getStatus())
                || !STANDARD_AUDIT_STATUS_AUDITED.equals(standard.getAuditStatus())) {
            return;
        }
        List<QmsQualityStandardItemDO> standardItems =
                qmsQualityStandardItemMapper.selectListByStandardId(order.getStandardId());
        if (standardItems.isEmpty()) {
            return;
        }
        Map<Long, QmsQualityStandardItemDO> standardItemById = new LinkedHashMap<>();
        Map<String, QmsQualityStandardItemDO> standardItemBySortKey = new LinkedHashMap<>();
        Map<String, QmsQualityStandardItemDO> standardItemByNameKey = new LinkedHashMap<>();
        Set<String> duplicateNameKeys = new LinkedHashSet<>();
        for (QmsQualityStandardItemDO standardItem : standardItems) {
            standardItemById.put(standardItem.getId(), standardItem);
            standardItemBySortKey.put(actualValueRequiredSortKey(standardItem.getSort(),
                    standardItem.getInspectionItem(), standardItem.getItemType()), standardItem);
            String nameKey = actualValueRequiredNameKey(standardItem.getInspectionItem(), standardItem.getItemType());
            if (standardItemByNameKey.containsKey(nameKey)) {
                duplicateNameKeys.add(nameKey);
            } else {
                standardItemByNameKey.put(nameKey, standardItem);
            }
        }
        duplicateNameKeys.forEach(standardItemByNameKey::remove);

        for (QmsFqcItemDO item : qmsFqcItemMapper.selectListByFqcId(order.getId())) {
            QmsQualityStandardItemDO standardItem = resolveActualValueRequiredStandardItem(
                    item, standardItemById, standardItemBySortKey, standardItemByNameKey);
            if (standardItem == null) {
                continue;
            }
            boolean required = Boolean.TRUE.equals(standardItem.getActualValueRequired());
            if (Boolean.TRUE.equals(item.getActualValueRequired()) == required) {
                continue;
            }
            QmsFqcItemDO updateObj = new QmsFqcItemDO();
            updateObj.setId(item.getId());
            updateObj.setActualValueRequired(required);
            if (!required) {
                updateObj.setActualValue(null);
            }
            qmsFqcItemMapper.updateById(updateObj);
            item.setActualValueRequired(required);
            if (!required) {
                item.setActualValue(null);
            }
        }
    }

    private boolean shouldSyncActualValueRequired(QmsFqcOrderDO order) {
        return order != null && order.getStandardId() != null
                && (STATUS_PENDING.equals(order.getStatus()) || STATUS_INSPECTING.equals(order.getStatus()));
    }

    private QmsQualityStandardItemDO resolveActualValueRequiredStandardItem(
            QmsFqcItemDO item,
            Map<Long, QmsQualityStandardItemDO> standardItemById,
            Map<String, QmsQualityStandardItemDO> standardItemBySortKey,
            Map<String, QmsQualityStandardItemDO> standardItemByNameKey) {
        if (item.getStandardItemId() != null && standardItemById.containsKey(item.getStandardItemId())) {
            return standardItemById.get(item.getStandardItemId());
        }
        QmsQualityStandardItemDO standardItem = standardItemBySortKey.get(actualValueRequiredSortKey(
                item.getSort(), item.getInspectionItem(), item.getItemType()));
        if (standardItem != null) {
            return standardItem;
        }
        return standardItemByNameKey.get(actualValueRequiredNameKey(item.getInspectionItem(), item.getItemType()));
    }

    private String actualValueRequiredSortKey(Integer sort, String inspectionItem, String itemType) {
        return (sort == null ? "" : sort) + "|" + actualValueRequiredNameKey(inspectionItem, itemType);
    }

    private String actualValueRequiredNameKey(String inspectionItem, String itemType) {
        return StrUtil.trimToEmpty(inspectionItem) + "|" + StrUtil.trimToEmpty(itemType);
    }

    private void fillSubmissionDetailItems(List<QmsCutRoundFqcRespVO.SubmissionDetail> details,
                                           List<QmsFqcRespVO.FqcItem> items) {
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcRespVO.FqcItem>> itemMap = (items == null ? Collections.<QmsFqcRespVO.FqcItem>emptyList() : items)
                .stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcRespVO.FqcItem::getSubmissionDetailId, LinkedHashMap::new, Collectors.toList()));
        for (QmsCutRoundFqcRespVO.SubmissionDetail detail : details) {
            List<QmsFqcRespVO.FqcItem> pieceItems = itemMap.getOrDefault(detail.getId(), Collections.emptyList());
            int requiredCount = pieceItems.size();
            int completedCount = (int) pieceItems.stream().filter(this::isCompletedItem).count();
            int abnormalCount = (int) pieceItems.stream().filter(this::isNgRespItem).count();
            detail.setItems(pieceItems);
            detail.setRequiredItemCount(requiredCount);
            detail.setCompletedItemCount(completedCount);
            detail.setAbnormalItemCount(abnormalCount);
            detail.setEntryProgress(requiredCount == 0 ? 0 : completedCount * 100 / requiredCount);
        }
    }

    private void fillBatchQtime(QmsCutRoundFqcRespVO respVO,
                                List<QmsCutRoundFqcRespVO.SubmissionDetail> details) {
        if (respVO == null) {
            return;
        }
        List<Long> reportIds = details == null ? Collections.emptyList() : details.stream()
                .map(QmsCutRoundFqcRespVO.SubmissionDetail::getCutRoundReportId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, HcCutRoundReportDO> reportMap = reportIds.isEmpty() ? Collections.emptyMap()
                : hcCutRoundReportMapper.selectListByIds(reportIds).stream()
                .collect(Collectors.toMap(HcCutRoundReportDO::getId, item -> item));
        LocalDateTime cutRoundEndTime = resolveCutRoundBatchCompleteTime(details);
        if (cutRoundEndTime == null) {
            cutRoundEndTime = reportMap.values().stream()
                .map(HcCutRoundReportDO::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        }
        QmsCutRoundFqcRespVO.SubmissionDetail firstDetail = details == null ? null : details.stream()
                .findFirst()
                .orElse(null);
        respVO.setQtime(hcQtimeEvaluationService.evaluateCutRoundToFqc(
                cutRoundEndTime, respVO.getInspectionTime(),
                respVO.getProductModel(),
                firstDetail == null ? null : firstDetail.getModelCode(),
                respVO.getMaterialCode(),
                firstDetail == null ? null : firstDetail.getMaterialCode(),
                respVO.getProductBatchNo(),
                firstDetail == null ? null : firstDetail.getProductionBatchNo()));
    }

    private LocalDateTime resolveCutRoundBatchCompleteTime(
            List<QmsCutRoundFqcRespVO.SubmissionDetail> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        Map<String, Long> operationIdByBatch = new LinkedHashMap<>();
        for (QmsCutRoundFqcRespVO.SubmissionDetail detail : details) {
            String batchNo = normalizeQtimeSegmentBatchNo(firstNotBlank(detail.getParentProductionBatchNo(),
                    detail.getProductionBatchNo()));
            if (detail.getPlanOperationId() != null && StrUtil.isNotBlank(batchNo)) {
                operationIdByBatch.putIfAbsent(batchNo, detail.getPlanOperationId());
            }
        }
        LocalDateTime result = null;
        for (Map.Entry<String, Long> entry : operationIdByBatch.entrySet()) {
            HcProcessReportDO batchComplete = hcProcessReportMapper.selectOne(
                    new LambdaQueryWrapperX<HcProcessReportDO>()
                            .eq(HcProcessReportDO::getPlanOperationId, entry.getValue())
                            .eq(HcProcessReportDO::getSourceMenuCode,
                                    SOURCE_MENU_CODE_CUT_ROUND_SEGMENT_COMPLETE)
                            .eq(HcProcessReportDO::getBatchNo, entry.getKey())
                            .eq(HcProcessReportDO::getDeleted, false)
                            .orderByDesc(HcProcessReportDO::getEndTime)
                            .orderByDesc(HcProcessReportDO::getId)
                            .last("LIMIT 1"));
            if (batchComplete != null && batchComplete.getEndTime() != null
                    && (result == null || batchComplete.getEndTime().isAfter(result))) {
                result = batchComplete.getEndTime();
            }
        }
        return result;
    }

    private String normalizeQtimeSegmentBatchNo(String batchNo) {
        String value = StrUtil.trimToEmpty(batchNo).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(value)) {
            return "";
        }
        value = value.replaceFirst("-J\\d+$", "");
        value = value.replaceFirst("-S\\d+$", "");
        return value.replaceFirst("^(.+[PQRS])\\d{3}[A-Z]?$", "$1");
    }

    private void fillCutRoundSampleDefects(Long fqcId, Collection<Long> itemIds,
                                           List<QmsFqcRespVO.FqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcRespVO.FqcSampleDefect>> defectBySampleId = new LinkedHashMap<>();
        Map<String, List<QmsFqcRespVO.FqcSampleDefect>> defectByItemSeq = new LinkedHashMap<>();
        for (QmsFqcSampleDefectDO defectDO : qmsFqcSampleDefectMapper.selectListByFqcIdAndItemIds(fqcId, itemIds)) {
            QmsFqcRespVO.FqcSampleDefect defect = BeanUtils.toBean(defectDO, QmsFqcRespVO.FqcSampleDefect.class);
            if (defectDO.getSampleId() != null) {
                defectBySampleId.computeIfAbsent(defectDO.getSampleId(), key -> new ArrayList<>()).add(defect);
            }
            defectByItemSeq.computeIfAbsent(cutRoundDefectSampleKey(defectDO.getFqcItemId(), defectDO.getSampleSeq()),
                    key -> new ArrayList<>()).add(defect);
        }
        for (QmsFqcRespVO.FqcSample sample : samples) {
            List<QmsFqcRespVO.FqcSampleDefect> defects = sample.getId() == null
                    ? null : defectBySampleId.get(sample.getId());
            if (defects == null || defects.isEmpty()) {
                defects = defectByItemSeq.getOrDefault(
                        cutRoundDefectSampleKey(sample.getFqcItemId(), sample.getSampleSeq()),
                        Collections.emptyList());
            }
            sample.setDefects(defects);
            if (!defects.isEmpty()) {
                sample.setDefectCode(defects.get(0).getDefectCode());
                sample.setDefectName(defects.get(0).getDefectName());
            }
        }
    }

    private String cutRoundDefectSampleKey(Long fqcItemId, Integer sampleSeq) {
        return String.valueOf(fqcItemId) + "#" + String.valueOf(sampleSeq);
    }

    private void fillSubmissionDetailStats(List<QmsCutRoundFqcRespVO.SubmissionDetail> details,
                                           List<QmsFqcItemDO> items) {
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcItemDO>> itemMap = (items == null ? Collections.<QmsFqcItemDO>emptyList() : items)
                .stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcItemDO::getSubmissionDetailId, LinkedHashMap::new, Collectors.toList()));
        for (QmsCutRoundFqcRespVO.SubmissionDetail detail : details) {
            List<QmsFqcItemDO> pieceItems = itemMap.getOrDefault(detail.getId(), Collections.emptyList());
            int requiredCount = pieceItems.size();
            int completedCount = (int) pieceItems.stream().filter(this::isCompletedItem).count();
            int abnormalCount = (int) pieceItems.stream().filter(this::isNgItem).count();
            detail.setRequiredItemCount(requiredCount);
            detail.setCompletedItemCount(completedCount);
            detail.setAbnormalItemCount(abnormalCount);
            detail.setEntryProgress(requiredCount == 0 ? 0 : completedCount * 100 / requiredCount);
        }
    }

    private void refreshSubmissionDetailsFromItems(Long fqcId) {
        refreshSubmissionDetailsFromItems(fqcId, null);
    }

    private void refreshSubmissionDetailsFromItems(Long fqcId, Collection<Long> submissionDetailIds) {
        List<QmsFqcSubmissionDetailDO> details = submissionDetailIds == null
                ? qmsFqcSubmissionDetailMapper.selectListByFqcId(fqcId)
                : qmsFqcSubmissionDetailMapper.selectListByIds(submissionDetailIds);
        if (details.isEmpty()) {
            return;
        }
        List<QmsFqcItemDO> items = submissionDetailIds == null
                ? qmsFqcItemMapper.selectListByFqcId(fqcId)
                : qmsFqcItemMapper.selectListByFqcIdAndSubmissionDetailIds(fqcId, submissionDetailIds);
        List<Long> itemIds = items.stream().map(QmsFqcItemDO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        List<QmsFqcSampleDO> samples = qmsFqcSampleMapper.selectListByFqcIdAndItemIds(fqcId, itemIds);
        Map<Long, List<QmsFqcItemDO>> itemMap = items.stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcItemDO::getSubmissionDetailId));
        Map<Long, List<QmsFqcSampleDO>> sampleMap = samples.stream()
                .filter(sample -> sample.getFqcItemId() != null)
                .collect(Collectors.groupingBy(QmsFqcSampleDO::getFqcItemId));
        LocalDateTime now = LocalDateTime.now();
        for (QmsFqcSubmissionDetailDO detail : details) {
            List<QmsFqcItemDO> pieceItems = itemMap.getOrDefault(detail.getId(), Collections.emptyList());
            String itemJudgment = resolvePieceJudgment(pieceItems);
            boolean keepManualNg = JUDGMENT_NG.equals(detail.getRowJudgment()) && JUDGMENT_PENDING.equals(itemJudgment);
            String rowJudgment = keepManualNg ? JUDGMENT_NG : itemJudgment;
            String itemNgReason = JUDGMENT_NG.equals(itemJudgment) ? buildPieceNgReason(pieceItems, sampleMap) : null;
            String ngReason = JUDGMENT_NG.equals(rowJudgment)
                    ? firstNotBlank(itemNgReason, detail.getNgReason(), detail.getRemark(), "裁切成品检验片级判定NG")
                    : null;
            QmsFqcSampleDO firstNgSample = JUDGMENT_NG.equals(itemJudgment)
                    ? findFirstNgSample(pieceItems, sampleMap) : null;
            Long inspectorId = detail.getInspectorId();
            String inspectorName = detail.getInspectorName();
            LocalDateTime inspectionTime = detail.getInspectionTime();
            if (!JUDGMENT_PENDING.equals(rowJudgment)) {
                inspectorId = inspectorId == null ? SecurityFrameworkUtils.getLoginUserId() : inspectorId;
                inspectorName = firstNotBlank(inspectorName, resolveLoginUserName());
                inspectionTime = inspectionTime == null ? now : inspectionTime;
            }
            qmsFqcSubmissionDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcSubmissionDetailDO>()
                    .eq(QmsFqcSubmissionDetailDO::getId, detail.getId())
                    .set(QmsFqcSubmissionDetailDO::getRowJudgment, rowJudgment)
                    .set(QmsFqcSubmissionDetailDO::getDefectCode, JUDGMENT_NG.equals(rowJudgment)
                            ? firstNotBlank(firstNgSample == null ? null : firstNgSample.getDefectCode(), detail.getDefectCode())
                            : null)
                    .set(QmsFqcSubmissionDetailDO::getDefectName, JUDGMENT_NG.equals(rowJudgment)
                            ? firstNotBlank(firstNgSample == null ? null : firstNgSample.getDefectName(), detail.getDefectName())
                            : null)
                    .set(QmsFqcSubmissionDetailDO::getNgReason, ngReason)
                    .set(QmsFqcSubmissionDetailDO::getInspectorId, inspectorId)
                    .set(QmsFqcSubmissionDetailDO::getInspectorName, inspectorName)
                    .set(QmsFqcSubmissionDetailDO::getInspectionTime, inspectionTime));
        }
    }

    private String resolvePieceJudgment(List<QmsFqcItemDO> pieceItems) {
        if (pieceItems == null || pieceItems.isEmpty()) {
            return JUDGMENT_PENDING;
        }
        boolean hasPending = false;
        for (QmsFqcItemDO item : pieceItems) {
            if (isNgItem(item)) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(item.getItemResult())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private String buildPieceNgReason(List<QmsFqcItemDO> pieceItems, Map<Long, List<QmsFqcSampleDO>> sampleMap) {
        List<String> reasons = new ArrayList<>();
        for (QmsFqcItemDO item : pieceItems) {
            if (!isNgItem(item)) {
                continue;
            }
            List<String> sampleReasons = sampleMap.getOrDefault(item.getId(), Collections.emptyList())
                    .stream()
                    .filter(sample -> JUDGMENT_NG.equals(sample.getSampleResult())
                            || JUDGMENT_NG.equals(sample.getQualitativeValue())
                            || StrUtil.isNotBlank(sample.getDefectName()))
                    .map(sample -> firstNotBlank(sample.getRemark(), sample.getDefectName(), sample.getDefectCode()))
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            String itemReason = item.getInspectionItem()
                    + (sampleReasons.isEmpty() ? "" : "：" + String.join("；", sampleReasons));
            reasons.add(itemReason);
        }
        return reasons.isEmpty() ? "检测项目存在NG" : String.join("；", reasons);
    }

    private QmsFqcSampleDO findFirstNgSample(List<QmsFqcItemDO> pieceItems, Map<Long, List<QmsFqcSampleDO>> sampleMap) {
        for (QmsFqcItemDO item : pieceItems) {
            for (QmsFqcSampleDO sample : sampleMap.getOrDefault(item.getId(), Collections.emptyList())) {
                if (JUDGMENT_NG.equals(sample.getSampleResult())
                        || JUDGMENT_NG.equals(sample.getQualitativeValue())
                        || StrUtil.isNotBlank(sample.getDefectName())) {
                    return sample;
                }
            }
        }
        return null;
    }

    private boolean isCompletedItem(QmsFqcRespVO.FqcItem item) {
        return JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult());
    }

    private boolean isCompletedItem(QmsFqcItemDO item) {
        return JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult());
    }

    private boolean isNgRespItem(QmsFqcRespVO.FqcItem item) {
        return JUDGMENT_NG.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getQaResult());
    }

    private Map<Long, HcCutRoundReportDO> loadCutRoundReports(List<QmsFqcSubmissionDetailDO> details) {
        List<Long> reportIds = details.stream()
                .map(QmsFqcSubmissionDetailDO::getCutRoundReportId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (reportIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return hcCutRoundReportMapper.selectListByIds(reportIds).stream()
                .collect(Collectors.toMap(HcCutRoundReportDO::getId, report -> report,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private void applySubmissionDetailDisposition(QmsFqcSubmissionDetailDO detail,
                                                   String rowJudgment,
                                                   String dispositionRemark) {
        String mergedRemark = mergeDispositionRemark(detail.getRemark(), dispositionRemark);
        String mergedNgReason = JUDGMENT_OK.equals(rowJudgment)
                ? null : mergeDispositionRemark(detail.getNgReason(), dispositionRemark);
        qmsFqcSubmissionDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getId, detail.getId())
                .set(QmsFqcSubmissionDetailDO::getRowJudgment, rowJudgment)
                .set(QmsFqcSubmissionDetailDO::getRemark, mergedRemark)
                .set(QmsFqcSubmissionDetailDO::getNgReason, mergedNgReason)
                .set(JUDGMENT_OK.equals(rowJudgment), QmsFqcSubmissionDetailDO::getDefectCode, null)
                .set(JUDGMENT_OK.equals(rowJudgment), QmsFqcSubmissionDetailDO::getDefectName, null));
        detail.setRowJudgment(rowJudgment);
        detail.setRemark(mergedRemark);
        detail.setNgReason(mergedNgReason);
        if (JUDGMENT_OK.equals(rowJudgment)) {
            detail.setDefectCode(null);
            detail.setDefectName(null);
        }
    }

    private void applyInspectionDetailDisposition(QmsFqcOrderDO order,
                                                   QmsFqcSubmissionDetailDO detail,
                                                   String inspectionResult,
                                                   String dispositionRemark,
                                                   LocalDateTime dispositionTime) {
        if (detail.getCutRoundInspectionDetailId() == null) {
            return;
        }
        hcCutRoundInspectionDetailMapper.update(null, new LambdaUpdateWrapper<HcCutRoundInspectionDetailDO>()
                .eq(HcCutRoundInspectionDetailDO::getId, detail.getCutRoundInspectionDetailId())
                .set(HcCutRoundInspectionDetailDO::getFqcOrderId, order.getId())
                .set(HcCutRoundInspectionDetailDO::getFqcNo, order.getFqcNo())
                .set(HcCutRoundInspectionDetailDO::getFqcStatus, order.getStatus())
                .set(HcCutRoundInspectionDetailDO::getFqcJudgment, order.getJudgment())
                .set(HcCutRoundInspectionDetailDO::getInspectionResult, inspectionResult)
                .set(HcCutRoundInspectionDetailDO::getInspectorName, resolveDispositionInspector(order, detail))
                .set(HcCutRoundInspectionDetailDO::getInspectionTime, dispositionTime)
                .set(HcCutRoundInspectionDetailDO::getRemark, dispositionRemark));
    }

    private void applyCutRoundReportDisposition(QmsFqcSubmissionDetailDO detail,
                                                HcCutRoundReportDO report,
                                                String inspectionResult,
                                                boolean reopenReport,
                                                String dispositionRemark,
                                                LocalDateTime dispositionTime) {
        Long reportId = detail.getCutRoundReportId();
        if (reportId == null) {
            return;
        }
        LambdaUpdateWrapper<HcCutRoundReportDO> wrapper = new LambdaUpdateWrapper<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getId, reportId)
                .set(HcCutRoundReportDO::getInspectionRemark, limitRemark(dispositionRemark))
                .set(HcCutRoundReportDO::getRemark, mergeDispositionRemark(report == null ? null : report.getRemark(),
                        dispositionRemark));
        if (reopenReport) {
            wrapper.set(HcCutRoundReportDO::getReportStatus, "DRAFT")
                    .set(HcCutRoundReportDO::getInspectionTaskId, null)
                    .set(HcCutRoundReportDO::getInspectionTaskNo, null)
                    .set(HcCutRoundReportDO::getInspectionStatus, null)
                    .set(HcCutRoundReportDO::getInspectionResult, null)
                    .set(HcCutRoundReportDO::getInspectorName, null)
                    .set(HcCutRoundReportDO::getInspectionTime, null)
                    .set(HcCutRoundReportDO::getConfirmerName, null)
                    .set(HcCutRoundReportDO::getConfirmerTime, null);
        } else {
            wrapper.set(HcCutRoundReportDO::getInspectionStatus, CUT_ROUND_INSPECTION_STATUS_COMPLETED)
                    .set(HcCutRoundReportDO::getInspectionResult, inspectionResult)
                    .set(HcCutRoundReportDO::getInspectorName, resolveDispositionInspector(null, detail))
                    .set(HcCutRoundReportDO::getInspectionTime, dispositionTime);
        }
        hcCutRoundReportMapper.update(null, wrapper);
    }

    private String resolveDispositionInspector(QmsFqcOrderDO order, QmsFqcSubmissionDetailDO detail) {
        return firstNotBlank(detail == null ? null : detail.getInspectorName(),
                order == null ? null : order.getQaInspectorName(),
                order == null ? null : order.getInspectorName(),
                resolveLoginUserName());
    }

    private String resolveEffectiveDisposition(String dispositionType, String scopeRole) {
        if ("PICK".equals(dispositionType) && "PICK_OUTSIDE_SCRAP".equals(normalizeUpper(scopeRole))) {
            return "SCRAP";
        }
        return dispositionType;
    }

    private boolean isNcrDispositionReleased(String dispositionType) {
        return "PICK".equals(dispositionType) || "CONCESSION".equals(dispositionType);
    }

    private boolean isNcrDispositionReopen(String dispositionType) {
        return "REWORK".equals(dispositionType) || "RECUT".equals(dispositionType);
    }

    private String buildNcrDispositionRemark(QmsNcRecordDO ncr,
                                             QmsNcDispositionExecutionDO execution,
                                             QmsNcDispositionScopeDO scope,
                                             String effectiveDisposition) {
        StringBuilder remark = new StringBuilder();
        remark.append("根据").append(firstNotBlank(ncr.getNcNo(), execution.getNcNo(), "NCR")).append("处置单");
        String finalOpinion = firstNotBlank(ncr.getFinalOpinion(), execution.getRemark());
        if (StrUtil.isNotBlank(finalOpinion)) {
            remark.append("，审核意见：").append(finalOpinion);
        }
        remark.append("，").append(dispositionActionName(effectiveDisposition));
        Map<String, Object> extra = parseRiskSnapshot(execution.getExtraJson());
        if ("RECUT".equals(effectiveDisposition)) {
            String recutText = buildRecutRemark(extra);
            if (StrUtil.isNotBlank(recutText)) {
                remark.append("；").append(recutText);
            }
        }
        if ("CONCESSION".equals(effectiveDisposition)) {
            String concessionReason = stringValue(extra.get("concessionReason"));
            if (StrUtil.isNotBlank(concessionReason)) {
                remark.append("；特采理由：").append(concessionReason);
            }
        }
        if (StrUtil.isNotBlank(scope.getRemark())) {
            remark.append("；片号说明：").append(scope.getRemark());
        }
        return limitRemark(remark.toString());
    }

    private String buildRecutRemark(Map<String, Object> extra) {
        List<String> parts = new ArrayList<>();
        if (StrUtil.isNotBlank(stringValue(extra.get("recutTargetSize")))) {
            parts.add("目标尺寸：" + stringValue(extra.get("recutTargetSize")));
        }
        if (StrUtil.isNotBlank(stringValue(extra.get("recutTolerance")))) {
            parts.add("公差：" + stringValue(extra.get("recutTolerance")));
        }
        if (StrUtil.isNotBlank(stringValue(extra.get("recutQty")))) {
            parts.add("数量：" + stringValue(extra.get("recutQty")));
        }
        return parts.isEmpty() ? "" : "改切要求：" + String.join("，", parts);
    }

    private String dispositionActionName(String dispositionType) {
        return switch (dispositionType) {
            case "PICK" -> "挑选放行";
            case "REWORK" -> "返工，片号报工状态重置为未扫码";
            case "RECUT" -> "改切，片号报工状态重置为未扫码";
            case "SCRAP" -> "报废";
            case "CONCESSION" -> "特采放行";
            default -> StrUtil.blankToDefault(dispositionType, "处置");
        };
    }

    private String mergeDispositionRemark(String previous, String addition) {
        if (StrUtil.isBlank(previous)) {
            return limitRemark(addition);
        }
        if (StrUtil.isBlank(addition) || previous.contains(addition)) {
            return limitRemark(previous);
        }
        return limitRemark(previous + "；" + addition);
    }

    private String limitRemark(String remark) {
        String text = StrUtil.trimToEmpty(remark);
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    private String normalizePieceNo(String value) {
        return StrUtil.trimToEmpty(value).replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private void refreshOrderSubmissionStats(Long fqcId) {
        Long totalCount = qmsFqcSubmissionDetailMapper.selectCount(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false));
        Long okCount = qmsFqcSubmissionDetailMapper.selectCount(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .eq(QmsFqcSubmissionDetailDO::getRowJudgment, JUDGMENT_OK));
        Long ngCount = qmsFqcSubmissionDetailMapper.selectCount(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .eq(QmsFqcSubmissionDetailDO::getRowJudgment, JUDGMENT_NG));
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, fqcId)
                .set(QmsFqcOrderDO::getSubmissionDetailCount, totalCount.intValue())
                .set(QmsFqcOrderDO::getOkQty, okCount.intValue())
                .set(QmsFqcOrderDO::getNgQty, ngCount.intValue()));
    }

    private void refreshOrderItemProgress(Long fqcId) {
        Long requiredCount = qmsFqcItemMapper.selectCount(new LambdaQueryWrapperX<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, fqcId));
        Long completedCount = qmsFqcItemMapper.selectCount(new LambdaQueryWrapperX<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, fqcId)
                .in(QmsFqcItemDO::getItemResult, JUDGMENT_OK, JUDGMENT_NG));
        Long abnormalCount = qmsFqcItemMapper.selectCount(new LambdaQueryWrapperX<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, fqcId)
                .eq(QmsFqcItemDO::getItemResult, JUDGMENT_NG));
        int entryProgress = requiredCount == 0 ? 0 : completedCount.intValue() * 100 / requiredCount.intValue();
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, fqcId)
                .set(QmsFqcOrderDO::getRequiredItemCount, requiredCount.intValue())
                .set(QmsFqcOrderDO::getCompletedItemCount, completedCount.intValue())
                .set(QmsFqcOrderDO::getAbnormalItemCount, abnormalCount.intValue())
                .set(QmsFqcOrderDO::getEntryProgress, entryProgress));
    }

    private void syncCutRoundInspectionFromSubmissionDetails(Long fqcId) {
        syncCutRoundInspectionFromSubmissionDetails(fqcId, null);
    }

    private void syncCutRoundInspectionFromSubmissionDetails(Long fqcId, Collection<Long> submissionDetailIds) {
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(fqcId);
        if (order == null || !SOURCE_MODULE_CUT_ROUND_FQC.equals(order.getSourceModule())) {
            return;
        }
        List<QmsFqcSubmissionDetailDO> details = submissionDetailIds == null
                ? qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId())
                : qmsFqcSubmissionDetailMapper.selectListByIds(submissionDetailIds);
        for (QmsFqcSubmissionDetailDO detail : details) {
            String inspectionResult = resolveInspectionResult(order, detail);
            LocalDateTime inspectionTime = detail.getInspectionTime() == null ? order.getQaTime() : detail.getInspectionTime();
            String inspectorName = firstNotBlank(detail.getInspectorName(), order.getQaInspectorName(), order.getInspectorName());
            String remark = buildInspectionRemark(order, detail, inspectionResult);
            if (detail.getCutRoundInspectionDetailId() != null) {
                hcCutRoundInspectionDetailMapper.update(null, new LambdaUpdateWrapper<HcCutRoundInspectionDetailDO>()
                        .eq(HcCutRoundInspectionDetailDO::getId, detail.getCutRoundInspectionDetailId())
                        .set(HcCutRoundInspectionDetailDO::getFqcOrderId, order.getId())
                        .set(HcCutRoundInspectionDetailDO::getFqcNo, order.getFqcNo())
                        .set(HcCutRoundInspectionDetailDO::getFqcStatus, order.getStatus())
                        .set(HcCutRoundInspectionDetailDO::getFqcJudgment, order.getJudgment())
                        .set(HcCutRoundInspectionDetailDO::getInspectionResult, inspectionResult)
                        .set(HcCutRoundInspectionDetailDO::getInspectorName, inspectorName)
                        .set(HcCutRoundInspectionDetailDO::getInspectionTime, inspectionTime)
                        .set(HcCutRoundInspectionDetailDO::getRemark, remark));
            }
            syncCutRoundReportInspection(detail.getCutRoundReportId(), inspectionResult, inspectorName, inspectionTime, remark);
        }
        details.stream()
                .map(QmsFqcSubmissionDetailDO::getCutRoundInspectionTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::refreshCutRoundInspectionTaskStatus);
    }

    private void syncCutRoundReportInspection(Long cutRoundReportId, String inspectionResult,
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
                inspectionTime == null ? LocalDateTime.now() : inspectionTime, null, remark);
    }

    private void refreshCutRoundInspectionTaskStatus(Long taskId) {
        List<HcCutRoundInspectionDetailDO> details = hcCutRoundInspectionDetailMapper.selectListByTaskId(taskId);
        if (details.isEmpty()) {
            return;
        }
        long completedCount = details.stream().filter(this::isCutRoundInspectionDetailFinal).count();
        long ngCount = details.stream().filter(detail -> JUDGMENT_NG.equals(detail.getInspectionResult())).count();
        boolean hasWaitingQa = details.stream().anyMatch(detail -> STATUS_WAITING_QA.equals(detail.getFqcStatus()));
        String taskStatus;
        if (completedCount == details.size()) {
            taskStatus = ngCount == 0 ? CUT_ROUND_TASK_STATUS_COMPLETED : CUT_ROUND_TASK_STATUS_PARTIAL_NG;
        } else {
            taskStatus = hasWaitingQa ? CUT_ROUND_TASK_STATUS_WAITING_QA : CUT_ROUND_TASK_STATUS_INSPECTING;
        }
        hcCutRoundInspectionTaskMapper.update(null, new LambdaUpdateWrapper<HcCutRoundInspectionTaskDO>()
                .eq(HcCutRoundInspectionTaskDO::getId, taskId)
                .set(HcCutRoundInspectionTaskDO::getTaskStatus, taskStatus));
    }

    private boolean isCutRoundInspectionDetailFinal(HcCutRoundInspectionDetailDO detail) {
        return JUDGMENT_OK.equals(detail.getInspectionResult()) || JUDGMENT_NG.equals(detail.getInspectionResult());
    }

    private String resolveInspectionResult(QmsFqcOrderDO order, QmsFqcSubmissionDetailDO detail) {
        if (!STATUS_COMPLETED.equals(order.getStatus())) {
            return null;
        }
        return JUDGMENT_OK.equals(detail.getRowJudgment()) || JUDGMENT_NG.equals(detail.getRowJudgment())
                ? detail.getRowJudgment() : null;
    }

    private String buildInspectionRemark(QmsFqcOrderDO order, QmsFqcSubmissionDetailDO detail, String inspectionResult) {
        if (JUDGMENT_NG.equals(inspectionResult)) {
            return StrUtil.blankToDefault(detail.getNgReason(), "裁切成品检验片级判定NG");
        }
        if (JUDGMENT_OK.equals(inspectionResult)) {
            return "裁切成品检验片级判定OK";
        }
        if (STATUS_WAITING_QA.equals(order.getStatus())) {
            return "裁切成品检验已提交，等待质量审核";
        }
        return "裁切成品检验处理中";
    }

    private void validateInspectionItemsCompleted(Long fqcId, List<QmsFqcSubmissionDetailDO> details) {
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(fqcId);
        validateInspectionItemsCompleted(items, details);
    }

    private void validateInspectionItemsCompleted(List<QmsFqcItemDO> items,
                                                  List<QmsFqcSubmissionDetailDO> details) {
        if (items.isEmpty()) {
            throw invalidParamException("FQC检验项目不能为空");
        }
        Set<Long> ngDetailIds = (details == null ? Collections.<QmsFqcSubmissionDetailDO>emptyList() : details)
                .stream()
                .filter(detail -> JUDGMENT_NG.equals(detail.getRowJudgment()))
                .map(QmsFqcSubmissionDetailDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasPending = items.stream().anyMatch(item ->
                !ngDetailIds.contains(item.getSubmissionDetailId())
                        && !JUDGMENT_OK.equals(item.getItemResult())
                        && !JUDGMENT_NG.equals(item.getItemResult()));
        if (hasPending) {
            throw invalidParamException("存在未完成的FQC检验项目，片级NG可直接提交，其余片号请完成项目OK/NG");
        }
    }

    private void validateRequiredActualValues(Long fqcId) {
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(fqcId);
        validateRequiredActualValues(items);
    }

    private void validateRequiredActualValues(List<QmsFqcItemDO> items) {
        List<String> missingNames = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getActualValueRequired()))
                .filter(item -> StrUtil.isBlank(item.getActualValue()))
                .map(this::buildActualValueMissingLabel)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
        if (!missingNames.isEmpty()) {
            throw invalidParamException("检验项目【" + String.join("、", missingNames) + "】必须填写实际值");
        }
    }

    private String buildActualValueMissingLabel(QmsFqcItemDO item) {
        String batchNo = firstNotBlank(item.getProductionBatchNo(), item.getParentProductionBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            return firstNotBlank(item.getInspectionItem(), "未命名项目");
        }
        return batchNo + "/" + firstNotBlank(item.getInspectionItem(), "未命名项目");
    }

    private void validateSubmissionDetailsCompleted(List<QmsFqcSubmissionDetailDO> details) {
        if (details == null || details.isEmpty()) {
            throw invalidParamException("FQC送检明细不能为空");
        }
        boolean hasPending = details.stream().anyMatch(detail ->
                !JUDGMENT_OK.equals(detail.getRowJudgment()) && !JUDGMENT_NG.equals(detail.getRowJudgment()));
        if (hasPending) {
            throw invalidParamException("存在未判定的送检片号，请先完成片级OK/NG标记");
        }
    }

    private boolean isNgItem(QmsFqcItemDO item) {
        return JUDGMENT_NG.equals(item.getItemResult())
                || JUDGMENT_NG.equals(item.getOperatorResult())
                || JUDGMENT_NG.equals(item.getQaResult());
    }

    private QmsCutRoundFqcRespVO returnForRework(QmsFqcOrderDO order, String rejectReason) {
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, order.getId())
                .set(QmsFqcOrderDO::getStatus, STATUS_INSPECTING)
                .set(QmsFqcOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsFqcOrderDO::getReleaseResult, null)
                .set(QmsFqcOrderDO::getReleaseTime, null)
                .set(QmsFqcOrderDO::getSheetLocked, false)
                .set(QmsFqcOrderDO::getLastReturnReason, rejectReason)
                .set(QmsFqcOrderDO::getReturnCount, (order.getReturnCount() == null ? 0 : order.getReturnCount()) + 1));
        syncCutRoundInspectionFromSubmissionDetails(order.getId());
        return get(order.getId());
    }

    private String validateAuditResult(QmsFqcAuditReqVO auditReqVO) {
        String auditResult = StrUtil.blankToDefault(auditReqVO.getAuditResult(), "").trim();
        if (!AUDIT_PASS.equals(auditResult) && !AUDIT_REJECT.equals(auditResult)) {
            throw invalidParamException("审核结果只能为 PASS 或 REJECT");
        }
        return auditResult;
    }

    private String requireRejectReason(String rejectReason) {
        String normalizedReason = StrUtil.blankToDefault(rejectReason, "").trim();
        if (normalizedReason.isEmpty()) {
            throw invalidParamException("驳回原因不能为空");
        }
        return normalizedReason;
    }

    private QmsFqcOrderDO validateCutRoundFqc(Long id) {
        QmsFqcOrderDO order = id == null ? null : qmsFqcOrderMapper.selectById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw invalidParamException("裁切成品检验单不存在");
        }
        if (!SOURCE_MODULE_CUT_ROUND_FQC.equals(order.getSourceModule())) {
            throw invalidParamException("当前FQC单不是裁切成品检验旁路单");
        }
        return order;
    }

    private void validateCutRoundFqcEditable(QmsFqcOrderDO order) {
        if (STATUS_COMPLETED.equals(order.getStatus()) || STATUS_CANCELED.equals(order.getStatus())) {
            throw invalidParamException("裁切成品检验单已关闭，不能修改");
        }
        if (Boolean.TRUE.equals(order.getSheetLocked()) || STATUS_WAITING_QA.equals(order.getStatus())) {
            throw invalidParamException("裁切成品检验单已提交或锁定，不能修改");
        }
    }

    private void validateAuditRevokeDownstream(List<QmsFqcSubmissionDetailDO> details) {
        for (QmsFqcSubmissionDetailDO detail : details) {
            if (hcPackagingManualPieceMapper.selectBySliceBatchNo(detail.getProductionBatchNo()) != null
                    || hcFinishedStockMapper.selectBySliceBatchNo(detail.getProductionBatchNo()) != null
                    || (detail.getCutRoundReportId() != null
                    && hcFgShippingNoticePickItemMapper.selectActiveBySourceCutRoundReportId(
                    detail.getCutRoundReportId()) != null)) {
                throw invalidParamException("存在已进入包装、成品库存或发货锁定的片号，不能撤销审核");
            }
        }
    }

    private String normalizeRowJudgment(String rowJudgment) {
        String value = StrUtil.blankToDefault(rowJudgment, JUDGMENT_PENDING).trim().toUpperCase();
        if (!JUDGMENT_OK.equals(value) && !JUDGMENT_NG.equals(value) && !JUDGMENT_PENDING.equals(value)) {
            throw invalidParamException("片级判定只能是 OK、NG 或 PENDING");
        }
        return value;
    }

    private List<String> normalizeSubmissionDetailPhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalizedUrls = new LinkedHashSet<>();
        for (String photoUrl : photoUrls) {
            String normalizedUrl = StrUtil.blankToDefault(photoUrl, "").trim();
            if (normalizedUrl.isEmpty()) {
                continue;
            }
            validateFqcPhotoUrl(normalizedUrl, "片级照片");
            normalizedUrls.add(normalizedUrl);
        }
        if (normalizedUrls.size() > SUBMISSION_DETAIL_PHOTO_MAX_COUNT) {
            throw invalidParamException("片级照片最多上传{}张", SUBMISSION_DETAIL_PHOTO_MAX_COUNT);
        }
        return new ArrayList<>(normalizedUrls);
    }

    private void validateFqcPhotoUrl(String photoUrl, String label) {
        if (photoUrl.length() > SUBMISSION_DETAIL_PHOTO_MAX_URL_LENGTH) {
            throw invalidParamException("{}URL长度不能超过{}个字符", label,
                    SUBMISSION_DETAIL_PHOTO_MAX_URL_LENGTH);
        }
        String path = photoUrl;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int hashIndex = path.indexOf('#');
        if (hashIndex >= 0) {
            path = path.substring(0, hashIndex);
        }
        int slashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int extIndex = path.lastIndexOf('.');
        // 文件服务代理地址可能没有扩展名；仅当最后一个路径段明确带后缀时校验白名单。
        if (extIndex <= slashIndex) {
            return;
        }
        String extension = path.substring(extIndex + 1).toLowerCase(Locale.ROOT);
        if (!SUBMISSION_DETAIL_PHOTO_EXTENSIONS.contains(extension)) {
            throw invalidParamException("{}仅支持jpg、jpeg、png、webp、gif、bmp格式", label);
        }
    }

    private ItemPhotoScope validateItemPhotoScope(QmsFqcOrderDO order, Long submissionDetailId, Long fqcItemId) {
        QmsFqcSubmissionDetailDO detail = qmsFqcSubmissionDetailMapper.selectById(submissionDetailId);
        if (detail == null || Boolean.TRUE.equals(detail.getDeleted())
                || !Objects.equals(detail.getFqcId(), order.getId())) {
            throw invalidParamException("送检明细不存在或不属于当前裁切成品检验单");
        }
        QmsFqcItemDO item = qmsFqcItemMapper.selectById(fqcItemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())
                || !Objects.equals(item.getFqcId(), order.getId())
                || !Objects.equals(item.getSubmissionDetailId(), detail.getId())) {
            throw invalidParamException("检验项目不存在或不属于当前送检片号");
        }
        return new ItemPhotoScope(detail, item);
    }

    private void validateItemPhotoSample(Long sampleId, ItemPhotoScope scope) {
        if (sampleId == null) {
            return;
        }
        QmsFqcSampleDO sample = qmsFqcSampleMapper.selectById(sampleId);
        if (sample == null || Boolean.TRUE.equals(sample.getDeleted())
                || !Objects.equals(sample.getFqcId(), scope.item.getFqcId())
                || !Objects.equals(sample.getSubmissionDetailId(), scope.detail.getId())
                || !Objects.equals(sample.getFqcItemId(), scope.item.getId())) {
            throw invalidParamException("检验样本不存在或不属于当前检验项目");
        }
    }

    private static final class ItemPhotoScope {

        private final QmsFqcSubmissionDetailDO detail;
        private final QmsFqcItemDO item;

        private ItemPhotoScope(QmsFqcSubmissionDetailDO detail, QmsFqcItemDO item) {
            this.detail = detail;
            this.item = item;
        }
    }

    private ScanResolution resolveScanMatches(QmsFqcScanReqVO scanReqVO, String scanCode) {
        if (scanReqVO.getCurrentFqcId() != null) {
            QmsFqcOrderDO currentOrder = qmsFqcOrderMapper.selectById(scanReqVO.getCurrentFqcId());
            if (isCutRoundFqc(currentOrder)) {
                List<ScanMatch> currentPieceMatches = buildScanMatches(
                        qmsFqcSubmissionDetailMapper.selectListByFqcIdAndProductionBatchNo(currentOrder.getId(), scanCode),
                        SCAN_TARGET_PRODUCTION_BATCH_NO, "已定位到当前单据的送检片号");
                if (!currentPieceMatches.isEmpty()) {
                    return new ScanResolution(currentPieceMatches, SCAN_TARGET_PRODUCTION_BATCH_NO);
                }
                List<ScanMatch> currentParentMatches = buildScanMatches(
                        qmsFqcSubmissionDetailMapper.selectListByFqcIdAndParentProductionBatchNo(currentOrder.getId(), scanCode),
                        SCAN_TARGET_PARENT_PRODUCTION_BATCH_NO, "已按当前单据父批次定位到送检明细");
                if (!currentParentMatches.isEmpty()) {
                    return new ScanResolution(currentParentMatches, SCAN_TARGET_PARENT_PRODUCTION_BATCH_NO);
                }
            }
        }

        List<ScanMatch> pieceMatches = buildScanMatches(
                qmsFqcSubmissionDetailMapper.selectListByProductionBatchNo(scanCode),
                SCAN_TARGET_PRODUCTION_BATCH_NO, "已定位到送检片号");
        if (!pieceMatches.isEmpty()) {
            return new ScanResolution(pieceMatches, SCAN_TARGET_PRODUCTION_BATCH_NO);
        }
        List<ScanMatch> parentMatches = buildScanMatches(
                qmsFqcSubmissionDetailMapper.selectListByParentProductionBatchNo(scanCode),
                SCAN_TARGET_PARENT_PRODUCTION_BATCH_NO, "已按父批次定位到送检明细");
        if (!parentMatches.isEmpty()) {
            return new ScanResolution(parentMatches, SCAN_TARGET_PARENT_PRODUCTION_BATCH_NO);
        }

        List<QmsFqcOrderDO> orders = selectCutRoundFqcsByScanCode(scanCode);
        if (orders.isEmpty()) {
            return new ScanResolution(Collections.emptyList(), SCAN_TARGET_PRODUCTION_BATCH_NO);
        }
        List<ScanMatch> orderMatches = orders.stream()
                .flatMap(order -> buildScanMatches(order, SCAN_TARGET_FQC_ORDER,
                        "已按单据号定位到送检明细").stream())
                .collect(Collectors.toList());
        return new ScanResolution(orderMatches, SCAN_TARGET_FQC_ORDER);
    }

    private List<ScanMatch> buildScanMatches(List<QmsFqcSubmissionDetailDO> details,
                                             String targetType, String message) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> fqcIds = details.stream()
                .map(QmsFqcSubmissionDetailDO::getFqcId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (fqcIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, QmsFqcOrderDO> orderMap = qmsFqcOrderMapper.selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                        .in(QmsFqcOrderDO::getId, fqcIds)
                        .eq(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_CUT_ROUND_FQC)
                        .eq(QmsFqcOrderDO::getDeleted, false))
                .stream()
                .filter(this::isCutRoundFqc)
                .collect(Collectors.toMap(QmsFqcOrderDO::getId, item -> item));
        if (orderMap.isEmpty()) {
            return Collections.emptyList();
        }
        return details.stream()
                .map(detail -> {
                    QmsFqcOrderDO order = orderMap.get(detail.getFqcId());
                    return order == null ? null : new ScanMatch(order, detail, targetType, message);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ScanMatch> buildScanMatches(QmsFqcOrderDO order, String targetType, String message) {
        if (!isCutRoundFqc(order)) {
            return Collections.emptyList();
        }
        List<QmsFqcSubmissionDetailDO> details = qmsFqcSubmissionDetailMapper.selectListByFqcId(order.getId());
        if (details.isEmpty()) {
            return Collections.singletonList(new ScanMatch(order, null, targetType,
                    "已打开裁切成品检验单，当前单据缺少送检明细"));
        }
        return details.stream()
                .map(detail -> new ScanMatch(order, detail, targetType, message))
                .collect(Collectors.toList());
    }

    private boolean isCutRoundFqc(QmsFqcOrderDO order) {
        return order != null && !Boolean.TRUE.equals(order.getDeleted())
                && SOURCE_MODULE_CUT_ROUND_FQC.equals(order.getSourceModule());
    }

    private QmsCutRoundFqcScanRespVO.ScanCandidate buildScanCandidate(ScanMatch scanMatch) {
        QmsCutRoundFqcScanRespVO.ScanCandidate candidate = new QmsCutRoundFqcScanRespVO.ScanCandidate();
        candidate.setFqcId(scanMatch.order.getId());
        candidate.setFqcNo(scanMatch.order.getFqcNo());
        candidate.setFqcStatus(scanMatch.order.getStatus());
        if (scanMatch.detail != null) {
            candidate.setSubmissionDetailId(scanMatch.detail.getId());
            candidate.setCutRoundInspectionTaskNo(scanMatch.detail.getCutRoundInspectionTaskNo());
            candidate.setPlanNo(scanMatch.detail.getPlanNo());
            candidate.setProductionBatchNo(scanMatch.detail.getProductionBatchNo());
            candidate.setParentProductionBatchNo(scanMatch.detail.getParentProductionBatchNo());
            candidate.setMaterialCode(scanMatch.detail.getMaterialCode());
            candidate.setModelCode(scanMatch.detail.getModelCode());
        }
        return candidate;
    }

    private String buildScanCandidateIds(List<ScanMatch> scanMatches) {
        String candidateIds = scanMatches.stream()
                .map(item -> item.order.getId() + ":" + (item.detail == null ? "-" : item.detail.getId()))
                .collect(Collectors.joining(","));
        return candidateIds.length() <= 1000 ? candidateIds : candidateIds.substring(0, 1000);
    }

    private List<QmsFqcOrderDO> selectCutRoundFqcsByScanCode(String scanCode) {
        if (StrUtil.isBlank(scanCode)) {
            return Collections.emptyList();
        }
        return qmsFqcOrderMapper.selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_CUT_ROUND_FQC)
                .eq(QmsFqcOrderDO::getDeleted, false)
                .and(wrapper -> wrapper.eq(QmsFqcOrderDO::getFqcNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getReportNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getSourceReportNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getWorkOrderNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getProductBatchNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getBatchNo, scanCode))
                .orderByDesc(QmsFqcOrderDO::getId));
    }

    private void insertScanRecord(QmsFqcScanReqVO scanReqVO, QmsFqcOrderDO order, QmsFqcSubmissionDetailDO detail,
                                  Long tenantIdOverride,
                                  String scanTargetType, String matchResult, int candidateCount,
                                  String candidateIds, String blockedReason) {
        Long matchedFqcId = detail == null ? order == null ? null : order.getId() : detail.getFqcId();
        String matchedFqcNo = detail == null ? order == null ? null : order.getFqcNo() : detail.getFqcNo();
        Long tenantId = tenantIdOverride != null ? tenantIdOverride
                : (detail == null ? (order == null ? null : order.getTenantId()) : detail.getTenantId());
        QmsFqcScanRecordDO record = QmsFqcScanRecordDO.builder()
                .scanCode(StrUtil.trimToEmpty(scanReqVO.getScanCode()))
                .scanTargetType(scanTargetType)
                .scanScene(scanReqVO.getScanScene())
                .matchResult(matchResult)
                .matchedFqcId(matchedFqcId)
                .matchedFqcNo(matchedFqcNo)
                .matchedSubmissionDetailId(detail == null ? null : detail.getId())
                .matchedProductionBatchNo(detail == null ? null : detail.getProductionBatchNo())
                .candidateCount(candidateCount)
                .candidateIds(candidateIds)
                .blockedReason(blockedReason)
                .openTarget(OPEN_TARGET_WORKBENCH)
                .scanUserId(SecurityFrameworkUtils.getLoginUserId())
                .scanUserName(resolveLoginUserName())
                .scanTime(LocalDateTime.now())
                .clientType(scanReqVO.getClientType())
                .terminalCode(scanReqVO.getTerminalCode())
                .tenantId(tenantId)
                .build();
        qmsFqcScanRecordMapper.insert(record);
    }

    private static final class ScanResolution {

        private final List<ScanMatch> matches;
        private final String targetType;

        private ScanResolution(List<ScanMatch> matches, String targetType) {
            this.matches = matches;
            this.targetType = targetType;
        }
    }

    private static final class ScanMatch {

        private final QmsFqcOrderDO order;
        private final QmsFqcSubmissionDetailDO detail;
        private final String targetType;
        private final String message;

        private ScanMatch(QmsFqcOrderDO order, QmsFqcSubmissionDetailDO detail,
                          String targetType, String message) {
            this.order = order;
            this.detail = detail;
            this.targetType = targetType;
            this.message = message;
        }
    }

    private String resolveFqcSubmissionType(HcPlanOrderDO planOrder) {
        String text = (StrUtil.blankToDefault(planOrder.getPlanMode(), "") + " "
                + StrUtil.blankToDefault(planOrder.getSourceType(), "") + " "
                + StrUtil.blankToDefault(planOrder.getProdType(), "") + " "
                + StrUtil.blankToDefault(planOrder.getProdTypeName(), "")).toUpperCase();
        return text.contains("RD") || text.contains("RND") || text.contains("研发") ? "RND" : "MASS_SHIPMENT";
    }

    private String resolveLoginUserName() {
        return firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private void fillRecheckSource(QmsCutRoundFqcRespVO respVO) {
        if (respVO == null) {
            return;
        }
        respVO.setOriginalInspectionNo(firstNotBlank(respVO.getRejectRootInspectionNo(),
                respVO.getRejectPrevInspectionNo()));
    }

    private LocalDateTime firstValidTime(LocalDateTime... values) {
        if (values != null) {
            for (LocalDateTime value : values) {
                if (value != null && value.getYear() >= 2000) {
                    return value;
                }
            }
        }
        return LocalDateTime.now();
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private record FqcAutoNgSuggestion(boolean cutRoundNg, String defectCode, String defectName, String ngReason) {

        private static FqcAutoNgSuggestion none() {
            return new FqcAutoNgSuggestion(false, null, null, null);
        }
    }
}
