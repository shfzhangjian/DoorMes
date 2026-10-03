package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPendingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticePickItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticePickItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_FINISHED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_ITEMS_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_ITEM_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_SHIPPING_NOTICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_STANDARD_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOQC_STANDARD_NOT_EXISTS;

@Service
@Validated
public class QmsOqcServiceImpl implements QmsOqcService {

    private static final String APPLY_TYPE_OQC = "OQC";
    private static final Integer ENABLED = 1;
    private static final Integer AUDITED = 20;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_WAITING_QA = "WAITING_QA";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String NOTICE_STATUS_OQC_INSPECTING = "OQC_INSPECTING";
    private static final String NOTICE_STATUS_OQC_PASSED = "OQC_PASSED";
    private static final String NOTICE_STATUS_OQC_REJECTED = "OQC_REJECTED";
    private static final String NOTICE_STATUS_PACKAGED = "PACKAGED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String JUDGMENT_NA = "NA";
    private static final String PROCESS_PENDING = "PENDING";
    private static final String ALLOW_SHIPMENT = "ALLOW_SHIPMENT";
    private static final String FREEZE_SHIPMENT = "FREEZE_SHIPMENT";
    private static final String ENTRY_LAYOUT_PROGRAM_FORM = "PROGRAM_FORM";
    private static final String ENTRY_MODE_MANUAL = "MANUAL";
    private static final String INPUT_STATUS_EMPTY = "EMPTY";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final String VALUE_SOURCE_MANUAL = "MANUAL";
    private static final String AUDIT_PASS = "PASS";
    private static final String AUDIT_REJECT = "REJECT";
    private static final String AUDIT_FAIL = "FAIL";
    private static final String SCAN_SCENE_LEDGER_TOOLBAR = "LEDGER_TOOLBAR";
    private static final String SCAN_TARGET_UNKNOWN = "UNKNOWN";
    private static final String SCAN_TARGET_OQC_NO = "OQC_NO";
    private static final String SCAN_TARGET_SHIPPING_NOTICE = "SHIPPING_NOTICE";
    private static final String SCAN_TARGET_SHIPPING_DETAIL = "SHIPPING_DETAIL";
    private static final String SCAN_TARGET_BATCH_OR_MATERIAL = "BATCH_OR_MATERIAL";
    private static final String SCAN_RESULT_CREATED = "CREATED";
    private static final String SCAN_RESULT_MATCHED_SINGLE = "MATCHED_SINGLE";
    private static final String SCAN_RESULT_MATCHED_MULTIPLE = "MATCHED_MULTIPLE";
    private static final String SCAN_RESULT_NOT_FOUND = "NOT_FOUND";
    private static final String SCAN_RESULT_STATUS_BLOCKED = "STATUS_BLOCKED";
    private static final String OPEN_TARGET_CANDIDATE_MODAL = "CANDIDATE_MODAL";
    private static final String OPEN_TARGET_ENTRY = "ENTRY";
    private static final String OPEN_TARGET_REPORT = "REPORT";
    private static final LocalDateTime MIN_VALID_BUSINESS_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Resource
    private QmsOqcOrderMapper qmsOqcOrderMapper;
    @Resource
    private QmsOqcItemMapper qmsOqcItemMapper;
    @Resource
    private QmsOqcSampleMapper qmsOqcSampleMapper;
    @Resource
    private QmsOqcAbnormalMapper qmsOqcAbnormalMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Resource
    private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Resource
    private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Resource
    private QmsAuditTodoNotifyService qmsAuditTodoNotifyService;
    @Resource
    private QmsNoGeneratorService qmsNoGeneratorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOqc(QmsOqcSaveReqVO createReqVO) {
        String oqcNo = generateOqcNo();
        validateOqcNoUnique(null, oqcNo);
        QmsQualityStandardDO standard = selectOqcStandard(createReqVO.getStandardId(), createReqVO.getMaterialCode(), createReqVO.getModelCode());
        List<QmsQualityStandardItemDO> standardItems = selectStandardItems(standard.getId());
        QmsOqcOrderDO entity = BeanUtils.toBean(createReqVO, QmsOqcOrderDO.class);
        entity.setId(null);
        entity.setOqcNo(oqcNo);
        applyStandardSnapshot(entity, standard);
        entity.setInspectionTime(resolveCreateInspectionTime(entity.getInspectionTime()));
        entity.setNoticeNo(defaultIfBlank(entity.getNoticeNo(), entity.getShippingNo()));
        entity.setStatus(defaultIfBlank(entity.getStatus(), STATUS_PENDING));
        entity.setJudgment(defaultIfBlank(entity.getJudgment(), JUDGMENT_PENDING));
        entity.setEntryLayout(defaultIfBlank(entity.getEntryLayout(), ENTRY_LAYOUT_PROGRAM_FORM));
        entity.setEntryMode(defaultIfBlank(entity.getEntryMode(), ENTRY_MODE_MANUAL));
        qmsOqcOrderMapper.insert(entity);
        saveStandardSnapshotDetails(entity, standardItems, createReqVO.getAbnormals());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOqcFromShippingNotice(Long shippingNoticeItemId, Long standardId) {
        HcFgShippingNoticeItemDO item = hcFgShippingNoticeItemMapper.selectById(shippingNoticeItemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted()) || item.getNoticeId() == null) {
            throw exception(HCOQC_SHIPPING_NOTICE_NOT_EXISTS);
        }
        return createOqcFromShippingNoticeId(item.getNoticeId(), standardId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOqcFromShippingNoticeId(Long shippingNoticeId, Long standardId) {
        HcFgShippingNoticeDO notice = shippingNoticeId == null ? null : hcFgShippingNoticeMapper.selectById(shippingNoticeId);
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw exception(HCOQC_SHIPPING_NOTICE_NOT_EXISTS);
        }
        List<HcFgShippingNoticeItemDO> items = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId()).stream()
                .filter(item -> !"SAMPLE".equals(notice.getProductType()) || !"CANCELLED".equals(item.getLockStatus()))
                .toList();
        if (items.isEmpty()) {
            throw exception(HCOQC_SHIPPING_NOTICE_NOT_EXISTS);
        }
        QmsOqcOrderDO existing = findActiveOqcByShippingNotice(notice.getId(), items);
        if (existing != null) {
            return existing.getId();
        }

        HcFgShippingNoticeItemDO firstItem = items.get(0);
        BigDecimal totalPieceQty = resolveShippingPieceQty(items);
        QmsOqcSaveReqVO reqVO = new QmsOqcSaveReqVO();
        reqVO.setShippingNoticeId(notice.getId());
        reqVO.setShippingNoticeItemId(firstItem.getId());
        reqVO.setShippingNo(notice.getNoticeNo());
        reqVO.setNoticeNo(notice.getNoticeNo());
        reqVO.setCustomerId(notice.getCustomerId());
        reqVO.setCustomerCode(notice.getCustomerCode());
        reqVO.setCustomerName(notice.getCustomerName());
        reqVO.setMaterialCode(defaultIfBlank(notice.getMaterialCode(), firstItem.getMaterialCode()));
        reqVO.setMaterialName(defaultIfBlank(notice.getMaterialName(), firstItem.getMaterialName()));
        reqVO.setModelCode(resolveShippingOqcModelCode(notice, firstItem));
        reqVO.setSpecification(defaultIfBlank(notice.getProductSize(), firstItem.getProductSize()));
        reqVO.setProductSize(defaultIfBlank(notice.getProductSize(), firstItem.getProductSize()));
        reqVO.setBatchNo(notice.getNoticeNo());
        reqVO.setCustomerBatchNo("SAMPLE".equals(notice.getProductType()) ? null
                : defaultIfBlank(firstItem.getCustomerProductBatchNo(), firstItem.getCustomerSliceBatchNo()));
        reqVO.setShippingPieceQty(totalPieceQty);
        reqVO.setShippingQty(totalPieceQty);
        reqVO.setStandardId(standardId);
        return createOqc(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOqc(QmsOqcSaveReqVO updateReqVO) {
        QmsOqcOrderDO old = validateOqcExists(updateReqVO.getId());
        validateEditable(old);
        QmsOqcOrderDO updateObj = BeanUtils.toBean(updateReqVO, QmsOqcOrderDO.class);
        updateObj.setOqcNo(old.getOqcNo());
        updateObj.setStatus(defaultIfBlank(updateReqVO.getStatus(), old.getStatus()));
        updateObj.setJudgment(defaultIfBlank(updateReqVO.getJudgment(), old.getJudgment()));
        qmsOqcOrderMapper.updateById(updateObj);
        if (updateReqVO.getItems() != null && !updateReqVO.getItems().isEmpty()) {
            updateExecutionDetails(validateOqcExists(updateReqVO.getId()), updateReqVO.getItems(), updateReqVO.getAbnormals());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOqc(Long id) {
        QmsOqcOrderDO entity = validateOqcExists(id);
        validateEditable(entity);
        qmsOqcSampleMapper.deleteByOqcId(id);
        qmsOqcAbnormalMapper.deleteByOqcId(id);
        qmsOqcItemMapper.deleteByOqcId(id);
        qmsOqcOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOqcList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> validateEditable(validateOqcExists(id)));
        qmsOqcSampleMapper.deleteByOqcIds(ids);
        qmsOqcAbnormalMapper.deleteByOqcIds(ids);
        qmsOqcItemMapper.deleteByOqcIds(ids);
        qmsOqcOrderMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendOqc(Long id) {
        QmsOqcOrderDO entity = validateOqcExists(id);
        validateEditable(entity);
        QmsOqcOrderDO updateObj = new QmsOqcOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SUSPENDED);
        qmsOqcOrderMapper.updateById(updateObj);
    }

    @Override
    public QmsOqcRespVO getOqcResp(Long id) {
        QmsOqcOrderDO entity = validateOqcExists(id);
        QmsOqcRespVO respVO = BeanUtils.toBean(entity, QmsOqcRespVO.class);
        HcFgShippingNoticeDO sourceNotice = entity.getShippingNoticeId() == null ? null
                : hcFgShippingNoticeMapper.selectById(entity.getShippingNoticeId());
        respVO.setProductType(sourceNotice == null ? null : sourceNotice.getProductType());
        respVO.setOriginalInspectionNo(respVO.getRejectRootInspectionNo() != null
                ? respVO.getRejectRootInspectionNo() : respVO.getRejectPrevInspectionNo());
        normalizeRespTimes(respVO);
        fillDetails(respVO);
        return respVO;
    }

    @Override
    public PageResult<QmsOqcOrderDO> getOqcPage(QmsOqcPageReqVO pageReqVO) {
        PageResult<QmsOqcOrderDO> pageResult = qmsOqcOrderMapper.selectPage(pageReqVO);
        if (pageResult.getList() != null) {
            pageResult.getList().forEach(this::normalizeOrderDisplayTimes);
            List<Long> noticeIds = pageResult.getList().stream().map(QmsOqcOrderDO::getShippingNoticeId)
                    .filter(Objects::nonNull).distinct().toList();
            if (!noticeIds.isEmpty()) {
                Map<Long, HcFgShippingNoticeDO> notices = hcFgShippingNoticeMapper.selectByIds(noticeIds).stream()
                        .collect(Collectors.toMap(HcFgShippingNoticeDO::getId, notice -> notice));
                pageResult.getList().forEach(order -> {
                    HcFgShippingNoticeDO notice = notices.get(order.getShippingNoticeId());
                    order.setProductType(notice == null ? null : notice.getProductType());
                });
            }
        }
        return pageResult;
    }

    @Override
    public List<QmsOqcPendingRespVO> getPendingOqcList(String keyword) {
        return hcFgShippingNoticeMapper.selectOutboundNoticeList(keyword).stream()
                .filter(notice -> NOTICE_STATUS_OQC_INSPECTING.equals(notice.getNoticeStatus()))
                .flatMap(notice -> {
                    List<HcFgShippingNoticeItemDO> noticeItems = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId());
                    return hcFgShippingNoticeItemMapper.selectLockedListByNoticeId(notice.getId()).stream()
                            .map(item -> buildPendingResp(notice, item, noticeItems));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsOqcScanRespVO resolveScan(QmsOqcScanReqVO scanReqVO) {
        String scanCode = scanReqVO.getScanCode().trim();
        String scanScene = defaultIfBlank(scanReqVO.getScanScene(), SCAN_SCENE_LEDGER_TOOLBAR);
        QmsOqcScanRespVO respVO = new QmsOqcScanRespVO();
        respVO.setScanCode(scanCode);
        respVO.setScanScene(scanScene);
        respVO.setScanTargetType(SCAN_TARGET_UNKNOWN);
        respVO.setMatchResult(SCAN_RESULT_NOT_FOUND);
        respVO.setCandidateCount(0);
        respVO.setScanTime(LocalDateTime.now());
        respVO.setMessage("未找到对应的 OQC 出货检验单或待检发货明细，请确认条码是否正确");

        if (scanReqVO.getCurrentOqcId() != null) {
            QmsOqcOrderDO currentOrder = qmsOqcOrderMapper.selectById(scanReqVO.getCurrentOqcId());
            if (currentOrder != null && equalsAny(scanCode, currentOrder.getOqcNo(), currentOrder.getShippingNo(),
                    currentOrder.getNoticeNo(), currentOrder.getBatchNo(), currentOrder.getCustomerBatchNo(),
                    currentOrder.getMaterialCode())) {
                fillSingleScanResp(respVO, currentOrder, resolveOqcScanTarget(scanCode, currentOrder));
                return respVO;
            }
        }

        QmsOqcOrderDO byOqcNo = qmsOqcOrderMapper.selectByOqcNo(scanCode, null);
        if (byOqcNo != null) {
            fillSingleScanResp(respVO, byOqcNo, SCAN_TARGET_OQC_NO);
            return respVO;
        }

        List<QmsOqcOrderDO> orderCandidates = qmsOqcOrderMapper.selectListByScanCode(scanCode, null);
        if (orderCandidates.size() == 1) {
            fillSingleScanResp(respVO, orderCandidates.get(0), resolveOqcScanTarget(scanCode, orderCandidates.get(0)));
            return respVO;
        }
        if (orderCandidates.size() > 1) {
            respVO.setScanTargetType(SCAN_TARGET_BATCH_OR_MATERIAL);
            respVO.setMatchResult(SCAN_RESULT_MATCHED_MULTIPLE);
            respVO.setOpenTarget(OPEN_TARGET_CANDIDATE_MODAL);
            respVO.setCandidateCount(orderCandidates.size());
            respVO.setCandidates(orderCandidates.stream().map(this::buildOrderCandidate).collect(Collectors.toList()));
            respVO.setMessage("找到多张 OQC 出货检验单，请选择要填写的单据");
            return respVO;
        }

        List<QmsOqcPendingRespVO> pendingCandidates = findPendingScanCandidates(scanCode);
        if (pendingCandidates.size() == 1) {
            QmsOqcPendingRespVO pending = pendingCandidates.get(0);
            Long oqcId = pending.getExistingOqcId() != null
                    ? pending.getExistingOqcId()
                    : createOqcFromShippingNotice(pending.getShippingNoticeItemId(), null);
            QmsOqcOrderDO order = validateOqcExists(oqcId);
            fillSingleScanResp(respVO, order, SCAN_TARGET_SHIPPING_DETAIL);
            respVO.setMatchResult(pending.getExistingOqcId() == null ? SCAN_RESULT_CREATED : respVO.getMatchResult());
            respVO.setMessage(pending.getExistingOqcId() == null
                    ? "已根据发货通知单明细生成 OQC 出货检验单，准备进入录入"
                    : respVO.getMessage());
            return respVO;
        }
        if (pendingCandidates.size() > 1) {
            respVO.setScanTargetType(SCAN_TARGET_SHIPPING_NOTICE);
            respVO.setMatchResult(SCAN_RESULT_MATCHED_MULTIPLE);
            respVO.setOpenTarget(OPEN_TARGET_CANDIDATE_MODAL);
            respVO.setCandidateCount(pendingCandidates.size());
            respVO.setCandidates(pendingCandidates.stream().map(this::buildPendingCandidate).collect(Collectors.toList()));
            respVO.setMessage("找到多条待检发货明细，请选择后生成或打开 OQC 出货检验单");
        }
        return respVO;
    }

    @Override
    public QmsOqcStandardRespVO getOqcStandard(String materialCode, String modelCode, Long standardId) {
        QmsQualityStandardDO standard = selectOqcStandard(standardId, materialCode, modelCode);
        List<QmsQualityStandardItemDO> standardItems = selectStandardItems(standard.getId());
        QmsOqcStandardRespVO respVO = BeanUtils.toBean(standard, QmsOqcStandardRespVO.class);
        respVO.setStandardId(standard.getId());
        respVO.setItems(standardItems.stream().map(this::buildStandardItem).collect(Collectors.toList()));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsOqcRespVO saveProgramEntry(QmsOqcSaveReqVO saveReqVO) {
        return saveEntryDraft(saveReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsOqcRespVO recalculateProgramEntry(QmsOqcSaveReqVO saveReqVO) {
        saveEntryDraft(saveReqVO);
        QmsOqcOrderDO updateObj = new QmsOqcOrderDO();
        updateObj.setId(saveReqVO.getId());
        updateObj.setLastCalculateTime(LocalDateTime.now());
        qmsOqcOrderMapper.updateById(updateObj);
        return getOqcResp(saveReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsOqcRespVO submitProgramEntry(QmsOqcSaveReqVO saveReqVO) {
        saveEntryDraft(saveReqVO);
        QmsOqcOrderDO order = validateOqcExists(saveReqVO.getId());
        String submitJudgment = calculateOrderJudgment(qmsOqcItemMapper.selectListByOqcId(order.getId()));
        QmsOqcOrderDO updateObj = buildProgressUpdate(order.getId(), qmsOqcItemMapper.selectListByOqcId(order.getId()));
        LocalDateTime now = LocalDateTime.now();
        updateObj.setStatus(STATUS_WAITING_QA);
        updateObj.setJudgment(submitJudgment);
        updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setInspectorName(resolveLoginUserName());
        updateObj.setInspectionTime(now);
        updateObj.setSheetLocked(true);
        qmsOqcOrderMapper.updateById(updateObj);
        qmsAuditTodoNotifyService.sendAuditTodoIfNeeded(order.getAuditNotifyTime(),
                "出货检验单(OQC)",
                order.getOqcNo(),
                buildOqcAuditBizName(order),
                "请进入质量管理-出货检验单(OQC)，使用整单审核按钮完成审核。",
                now,
                time -> qmsOqcOrderMapper.updateAuditNotifyTime(order.getId(), time));
        return getOqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsOqcRespVO auditProgramEntry(QmsOqcAuditReqVO auditReqVO) {
        QmsOqcOrderDO order = validateOqcExists(auditReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus())) {
            throw exception(HCOQC_ITEMS_NOT_COMPLETED);
        }
        if (AUDIT_REJECT.equals(auditReqVO.getAuditResult())) {
            QmsOqcOrderDO rejectObj = new QmsOqcOrderDO();
            rejectObj.setId(order.getId());
            rejectObj.setStatus(STATUS_INSPECTING);
            rejectObj.setJudgment(JUDGMENT_PENDING);
            rejectObj.setSheetLocked(false);
            rejectObj.setRemark(defaultIfBlank(auditReqVO.getRejectReason(), order.getRemark()));
            qmsOqcOrderMapper.updateById(rejectObj);
            qmsOqcOrderMapper.clearAuditNotifyTime(order.getId());
            return getOqcResp(order.getId());
        }
        List<QmsOqcItemDO> items = qmsOqcItemMapper.selectListByOqcId(order.getId());
        String finalJudgment = AUDIT_FAIL.equals(auditReqVO.getAuditResult()) ? JUDGMENT_NG : calculateOrderJudgment(items);
        LocalDateTime now = LocalDateTime.now();
        QmsOqcOrderDO updateObj = buildProgressUpdate(order.getId(), items);
        updateObj.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setQaInspectorName(resolveLoginUserName());
        updateObj.setQaTime(now);
        updateObj.setReleaseTime(now);
        updateObj.setSheetLocked(true);
        if (JUDGMENT_OK.equals(finalJudgment) && AUDIT_PASS.equals(auditReqVO.getAuditResult())) {
            updateObj.setStatus(STATUS_COMPLETED);
            updateObj.setJudgment(JUDGMENT_OK);
            updateObj.setReleaseResult(ALLOW_SHIPMENT);
        } else {
            updateObj.setStatus(STATUS_REJECTED);
            updateObj.setJudgment(JUDGMENT_NG);
            updateObj.setReleaseResult(FREEZE_SHIPMENT);
            updateObj.setRelatedNcrNo(defaultIfBlank(order.getRelatedNcrNo(), generateNcrNo()));
            updateObj.setNcrStatus(defaultIfBlank(order.getNcrStatus(), "APPROVING"));
        }
        qmsOqcOrderMapper.updateById(updateObj);
        qmsOqcOrderMapper.clearAuditNotifyTime(order.getId());
        syncShippingNoticeAfterOqcAudit(order, updateObj, now);
        if (JUDGMENT_NG.equals(updateObj.getJudgment())) {
            ensureNgAbnormal(order.getId(), order.getOqcNo(), items, updateObj.getRelatedNcrNo());
        }
        return getOqcResp(order.getId());
    }

    private void syncShippingNoticeAfterOqcAudit(QmsOqcOrderDO order, QmsOqcOrderDO auditResult, LocalDateTime auditTime) {
        if (order.getShippingNoticeId() == null) {
            return;
        }
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectById(order.getShippingNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            return;
        }
        List<HcFgShippingNoticeItemDO> noticeItems = hcFgShippingNoticeItemMapper.selectListByNoticeId(order.getShippingNoticeId());
        if (noticeItems.isEmpty()) {
            return;
        }
        boolean oqcPassed = STATUS_COMPLETED.equals(auditResult.getStatus())
                && JUDGMENT_OK.equals(auditResult.getJudgment())
                && ALLOW_SHIPMENT.equals(auditResult.getReleaseResult());
        String targetNoticeStatus = oqcPassed
                ? (noticeItems.stream().allMatch(this::isOuterPackageCompleted)
                ? NOTICE_STATUS_PACKAGED : NOTICE_STATUS_OQC_PASSED)
                : NOTICE_STATUS_OQC_REJECTED;
        for (HcFgShippingNoticeItemDO item : noticeItems) {
            HcFgShippingNoticeItemDO itemUpdate = new HcFgShippingNoticeItemDO();
            itemUpdate.setId(item.getId());
            // 复检通过时必须切换到最新 OQC，避免发货完成仍校验到原始不合格检验单。
            itemUpdate.setShippingQualityNo(order.getOqcNo());
            itemUpdate.setOqcOrderId(order.getId());
            itemUpdate.setOqcStatus(auditResult.getStatus());
            itemUpdate.setLockStatus(targetNoticeStatus);
            hcFgShippingNoticeItemMapper.updateById(itemUpdate);
        }

        for (HcFgShippingNoticePickItemDO pickItem : hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(order.getShippingNoticeId())) {
            HcFgShippingNoticePickItemDO pickUpdate = new HcFgShippingNoticePickItemDO();
            pickUpdate.setId(pickItem.getId());
            pickUpdate.setShippingQualityNo(order.getOqcNo());
            pickUpdate.setLockStatus(targetNoticeStatus);
            hcFgShippingNoticePickItemMapper.updateById(pickUpdate);
        }

        HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
        noticeUpdate.setId(order.getShippingNoticeId());
        noticeUpdate.setNoticeStatus(targetNoticeStatus);
        hcFgShippingNoticeMapper.updateById(noticeUpdate);
    }

    private boolean isOuterPackageCompleted(HcFgShippingNoticeItemDO item) {
        return StringUtils.hasText(defaultIfBlank(item.getPackageNo(), item.getOuterBoxNo()))
                && item.getShippingPackageTime() != null
                && StringUtils.hasText(item.getShippingPackageName());
    }

    private QmsOqcRespVO saveEntryDraft(QmsOqcSaveReqVO saveReqVO) {
        QmsOqcOrderDO order = validateOqcExists(saveReqVO.getId());
        validateEntryEditable(order);
        if (saveReqVO.getItems() != null && !saveReqVO.getItems().isEmpty()) {
            updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        }
        List<QmsOqcItemDO> items = qmsOqcItemMapper.selectListByOqcId(order.getId());
        QmsOqcOrderDO updateObj = buildProgressUpdate(order.getId(), items);
        updateObj.setStatus(STATUS_INSPECTING);
        updateObj.setJudgment(resolveDraftJudgment(items));
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setEntryMode(defaultIfBlank(saveReqVO.getEntryMode(), ENTRY_MODE_MANUAL));
        updateObj.setSheetLocked(false);
        updateObj.setLastSaveTime(LocalDateTime.now());
        qmsOqcOrderMapper.updateById(updateObj);
        return getOqcResp(order.getId());
    }

    private void updateExecutionDetails(QmsOqcOrderDO order, List<QmsOqcSaveReqVO.OqcItem> items,
                                        List<QmsOqcSaveReqVO.OqcAbnormal> abnormals) {
        validateItems(items);
        List<QmsOqcItemDO> existingItems = qmsOqcItemMapper.selectListByOqcId(order.getId());
        validateItemsExist(existingItems);
        for (QmsOqcSaveReqVO.OqcItem item : items) {
            if (!matchesExistingItem(item, existingItems)) {
                throw exception(HCOQC_ITEM_SOURCE_INVALID);
            }
        }
        qmsOqcSampleMapper.deleteByOqcId(order.getId());
        for (QmsOqcItemDO existingItem : existingItems) {
            QmsOqcSaveReqVO.OqcItem incomingItem = findIncomingItem(existingItem, items);
            if (incomingItem == null) {
                continue;
            }
            List<QmsOqcSaveReqVO.OqcSample> incomingSamples = incomingItem.getSamples() == null
                    ? Collections.emptyList() : incomingItem.getSamples();
            applyItemStats(existingItem, incomingSamples);
            qmsOqcItemMapper.updateById(existingItem);
            saveSamples(order, existingItem, incomingSamples);
        }
        if (abnormals != null) {
            qmsOqcAbnormalMapper.deleteByOqcId(order.getId());
            saveAbnormals(order, abnormals);
        }
    }

    private void applyItemStats(QmsOqcItemDO itemDO, List<QmsOqcSaveReqVO.OqcSample> samples) {
        String result = calculateItemResult(itemDO, samples);
        List<BigDecimal> measuredValues = samples.stream()
                .map(this::resolveMeasuredValue)
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
        } else {
            itemDO.setMaxValue(null);
            itemDO.setMinValue(null);
            itemDO.setAverageValue(null);
            itemDO.setOperatorMax(null);
            itemDO.setOperatorMin(null);
            itemDO.setOperatorAvg(null);
            itemDO.setCalculatedMax(null);
            itemDO.setCalculatedMin(null);
            itemDO.setCalculatedAvg(null);
        }
        itemDO.setItemResult(result);
        itemDO.setOperatorResult(result);
        itemDO.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        itemDO.setOperatorName(resolveLoginUserName());
        itemDO.setOperatorTime(LocalDateTime.now());
        itemDO.setRequiredSampleCount(itemDO.getSampleSize());
        int completedSampleCount = (int) samples.stream().filter(this::isCompletedSample).count();
        itemDO.setCompletedSampleCount(completedSampleCount);
        itemDO.setAbnormalSampleCount((int) samples.stream().filter(this::isNgSample).count());
        itemDO.setInputStatus(JUDGMENT_NG.equals(result) ? INPUT_STATUS_ABNORMAL
                : JUDGMENT_OK.equals(result) ? INPUT_STATUS_COMPLETE
                : completedSampleCount == 0 ? INPUT_STATUS_EMPTY : "FILLING");
    }

    private String calculateItemResult(QmsOqcItemDO itemDO, List<QmsOqcSaveReqVO.OqcSample> samples) {
        List<QmsOqcSaveReqVO.OqcSample> completedSamples = samples == null ? Collections.emptyList()
                : samples.stream().filter(this::isCompletedSample).collect(Collectors.toList());
        if (completedSamples.size() < itemDO.getSampleSize()) {
            return JUDGMENT_PENDING;
        }
        boolean hasPending = false;
        boolean hasOk = false;
        boolean hasNa = false;
        for (QmsOqcSaveReqVO.OqcSample sample : completedSamples) {
            if (isNgSample(sample)) {
                return JUDGMENT_NG;
            }
            BigDecimal value = resolveMeasuredValue(sample);
            if (value != null) {
                if (itemDO.getMinValueLimit() != null && value.compareTo(itemDO.getMinValueLimit()) < 0) {
                    return JUDGMENT_NG;
                }
                if (itemDO.getMaxValueLimit() != null && value.compareTo(itemDO.getMaxValueLimit()) > 0) {
                    return JUDGMENT_NG;
                }
                hasOk = true;
                continue;
            }
            if (JUDGMENT_NA.equals(sample.getSampleResult()) || JUDGMENT_NA.equals(sample.getQualitativeValue())) {
                hasNa = true;
                continue;
            }
            if (JUDGMENT_OK.equals(sample.getSampleResult()) || JUDGMENT_OK.equals(sample.getQualitativeValue())) {
                hasOk = true;
            } else {
                hasPending = true;
            }
        }
        if (!hasPending && hasNa && !hasOk) {
            return JUDGMENT_NA;
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private String calculateOrderJudgment(List<QmsOqcItemDO> items) {
        validateItemsExist(items);
        boolean hasPending = false;
        for (QmsOqcItemDO item : items) {
            if (JUDGMENT_NG.equals(item.getItemResult())) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(item.getItemResult()) && !JUDGMENT_NA.equals(item.getItemResult())) {
                hasPending = true;
            }
        }
        if (hasPending) {
            throw exception(HCOQC_ITEMS_NOT_COMPLETED);
        }
        return JUDGMENT_OK;
    }

    private String resolveDraftJudgment(List<QmsOqcItemDO> items) {
        if (items == null || items.isEmpty()) {
            return JUDGMENT_PENDING;
        }
        boolean hasPending = false;
        for (QmsOqcItemDO item : items) {
            if (JUDGMENT_NG.equals(item.getItemResult())) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(item.getItemResult()) && !JUDGMENT_NA.equals(item.getItemResult())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private void saveSamples(QmsOqcOrderDO order, QmsOqcItemDO itemDO, List<QmsOqcSaveReqVO.OqcSample> samples) {
        List<QmsOqcSampleDO> sampleList = new ArrayList<>();
        for (QmsOqcSaveReqVO.OqcSample sample : samples) {
            if (!isCompletedSample(sample)) {
                continue;
            }
            QmsOqcSampleDO sampleDO = BeanUtils.toBean(sample, QmsOqcSampleDO.class);
            sampleDO.setId(null);
            sampleDO.setOqcId(order.getId());
            sampleDO.setOqcItemId(itemDO.getId());
            sampleDO.setOqcNo(order.getOqcNo());
            sampleDO.setSampleRole(defaultIfBlank(sample.getSampleRole(), "OPERATOR"));
            sampleDO.setValueSource(defaultIfBlank(sample.getValueSource(), VALUE_SOURCE_MANUAL));
            sampleDO.setInputTime(sample.getInputTime() == null ? LocalDateTime.now() : sample.getInputTime());
            sampleDO.setMeasuredValue(resolveMeasuredValue(sample));
            sampleDO.setSampleResult(defaultIfBlank(sample.getSampleResult(), JUDGMENT_PENDING));
            sampleList.add(sampleDO);
        }
        if (!sampleList.isEmpty()) {
            qmsOqcSampleMapper.insertBatch(sampleList);
        }
    }

    private void saveAbnormals(QmsOqcOrderDO order, List<QmsOqcSaveReqVO.OqcAbnormal> abnormals) {
        if (abnormals == null || abnormals.isEmpty()) {
            return;
        }
        List<QmsOqcAbnormalDO> abnormalList = abnormals.stream().map(abnormal -> {
            QmsOqcAbnormalDO abnormalDO = BeanUtils.toBean(abnormal, QmsOqcAbnormalDO.class);
            abnormalDO.setId(null);
            abnormalDO.setOqcId(order.getId());
            abnormalDO.setOqcNo(order.getOqcNo());
            abnormalDO.setProcessStatus(defaultIfBlank(abnormal.getProcessStatus(), PROCESS_PENDING));
            return abnormalDO;
        }).collect(Collectors.toList());
        qmsOqcAbnormalMapper.insertBatch(abnormalList);
    }

    private void ensureNgAbnormal(Long oqcId, String oqcNo, List<QmsOqcItemDO> items, String relatedNcrNo) {
        if (!qmsOqcAbnormalMapper.selectListByOqcId(oqcId).isEmpty()) {
            return;
        }
        List<QmsOqcAbnormalDO> abnormalList = items.stream()
                .filter(item -> JUDGMENT_NG.equals(item.getItemResult()))
                .map(item -> QmsOqcAbnormalDO.builder()
                        .oqcId(oqcId)
                        .oqcNo(oqcNo)
                        .oqcItemId(item.getId())
                        .abnormalDesc("出货检验项判定不合格：" + item.getInspectionItem())
                        .processStatus(PROCESS_PENDING)
                        .actionRequired(FREEZE_SHIPMENT)
                        .relatedNcrNo(relatedNcrNo)
                        .dispositionStatus("APPROVING")
                        .build())
                .collect(Collectors.toList());
        if (!abnormalList.isEmpty()) {
            qmsOqcAbnormalMapper.insertBatch(abnormalList);
        }
    }

    private QmsQualityStandardDO selectOqcStandard(Long standardId, String materialCode, String modelCode) {
        if (standardId != null) {
            QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(standardId);
            validateStandardUsable(standard);
            return standard;
        }
        QmsQualityStandardDO standard = qmsQualityStandardMapper.selectList(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                        .eq(QmsQualityStandardDO::getApplyType, APPLY_TYPE_OQC)
                        .eq(QmsQualityStandardDO::getStatus, ENABLED)
                        .eq(QmsQualityStandardDO::getAuditStatus, AUDITED)
                        .orderByDesc(QmsQualityStandardDO::getId))
                .stream()
                .filter(item -> matchesOqcScope(item, materialCode, modelCode))
                .max(Comparator.comparingInt((QmsQualityStandardDO item) -> calculateStandardMatchScore(item, materialCode, modelCode))
                        .thenComparing(QmsQualityStandardDO::getId))
                .orElse(null);
        if (standard == null) {
            throw exception(HCOQC_STANDARD_NOT_EXISTS);
        }
        return standard;
    }

    private void validateStandardUsable(QmsQualityStandardDO standard) {
        if (standard == null || !APPLY_TYPE_OQC.equals(standard.getApplyType())
                || !ENABLED.equals(standard.getStatus()) || !AUDITED.equals(standard.getAuditStatus())) {
            throw exception(HCOQC_STANDARD_NOT_EXISTS);
        }
    }

    private List<QmsQualityStandardItemDO> selectStandardItems(Long standardId) {
        List<QmsQualityStandardItemDO> standardItems = qmsQualityStandardItemMapper.selectListByStandardId(standardId);
        if (standardItems == null || standardItems.isEmpty()) {
            throw exception(HCOQC_STANDARD_ITEMS_EMPTY);
        }
        return standardItems;
    }

    private boolean matchesOqcScope(QmsQualityStandardDO standard, String materialCode, String modelCode) {
        boolean materialMatched = !StringUtils.hasText(standard.getMaterialCode())
                || (StringUtils.hasText(materialCode) && standard.getMaterialCode().equals(materialCode));
        boolean modelMatched = !StringUtils.hasText(standard.getProductModelCode())
                || (StringUtils.hasText(modelCode) && standard.getProductModelCode().equals(modelCode));
        return materialMatched && modelMatched;
    }

    private int calculateStandardMatchScore(QmsQualityStandardDO standard, String materialCode, String modelCode) {
        int score = 0;
        if (StringUtils.hasText(standard.getMaterialCode()) && standard.getMaterialCode().equals(materialCode)) {
            score += 4;
        }
        if (StringUtils.hasText(standard.getProductModelCode()) && standard.getProductModelCode().equals(modelCode)) {
            score += 2;
        }
        return score;
    }

    private void applyStandardSnapshot(QmsOqcOrderDO order, QmsQualityStandardDO standard) {
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
        if (!StringUtils.hasText(order.getModelCode())) {
            order.setModelCode(standard.getProductModelCode());
        }
    }

    private void saveStandardSnapshotDetails(QmsOqcOrderDO order, List<QmsQualityStandardItemDO> standardItems,
                                             List<QmsOqcSaveReqVO.OqcAbnormal> abnormals) {
        for (int i = 0; i < standardItems.size(); i++) {
            QmsQualityStandardItemDO standardItem = standardItems.get(i);
            QmsOqcItemDO itemDO = new QmsOqcItemDO();
            itemDO.setOqcId(order.getId());
            itemDO.setOqcNo(order.getOqcNo());
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
            itemDO.setJudgmentMetric(standardItem.getJudgmentMetric());
            itemDO.setTemplateParams(standardItem.getTemplateParams());
            itemDO.setAvgMinLimit(standardItem.getAvgMinLimit());
            itemDO.setAvgMaxLimit(standardItem.getAvgMaxLimit());
            itemDO.setStdMinLimit(standardItem.getStdMinLimit());
            itemDO.setStdMaxLimit(standardItem.getStdMaxLimit());
            itemDO.setTestTool(standardItem.getTestTool());
            itemDO.setSampleSize(order.getSampleQty() != null && order.getSampleQty() > 0 ? order.getSampleQty() : standardItem.getSampleSize());
            itemDO.setMinValueLimit(standardItem.getMinValue());
            itemDO.setMaxValueLimit(standardItem.getMaxValue());
            itemDO.setItemResult(JUDGMENT_PENDING);
            itemDO.setOperatorResult(JUDGMENT_PENDING);
            itemDO.setQaResult(JUDGMENT_PENDING);
            itemDO.setRequiredSampleCount(itemDO.getSampleSize());
            itemDO.setCompletedSampleCount(0);
            itemDO.setAbnormalSampleCount(0);
            itemDO.setInputStatus(INPUT_STATUS_EMPTY);
            itemDO.setIsSpc(Boolean.TRUE.equals(standardItem.getIsSpc()));
            itemDO.setSort(standardItem.getSort() == null ? (i + 1) * 10 : standardItem.getSort());
            qmsOqcItemMapper.insert(itemDO);
        }
        saveAbnormals(order, abnormals);
    }

    private void fillDetails(QmsOqcRespVO respVO) {
        List<QmsOqcItemDO> items = qmsOqcItemMapper.selectListByOqcId(respVO.getId());
        List<QmsOqcSampleDO> samples = qmsOqcSampleMapper.selectListByOqcId(respVO.getId());
        Map<Long, List<QmsOqcRespVO.OqcSample>> sampleMap = BeanUtils.toBean(samples, QmsOqcRespVO.OqcSample.class).stream()
                .collect(Collectors.groupingBy(QmsOqcRespVO.OqcSample::getOqcItemId));
        List<QmsOqcRespVO.OqcItem> itemRespList = BeanUtils.toBean(items, QmsOqcRespVO.OqcItem.class);
        itemRespList.forEach(item -> item.setSamples(sampleMap.getOrDefault(item.getId(), Collections.emptyList())));
        respVO.setItems(itemRespList);
        respVO.setAbnormals(BeanUtils.toBean(qmsOqcAbnormalMapper.selectListByOqcId(respVO.getId()), QmsOqcRespVO.OqcAbnormal.class));
    }

    private LocalDateTime resolveCreateInspectionTime(LocalDateTime inspectionTime) {
        return isInvalidBusinessTime(inspectionTime) ? LocalDateTime.now() : inspectionTime;
    }

    private void normalizeRespTimes(QmsOqcRespVO respVO) {
        respVO.setInspectionTime(normalizeDisplayTime(respVO.getInspectionTime()));
        respVO.setQaTime(normalizeDisplayTime(respVO.getQaTime()));
        respVO.setReleaseTime(normalizeDisplayTime(respVO.getReleaseTime()));
        respVO.setLastSaveTime(normalizeDisplayTime(respVO.getLastSaveTime()));
        respVO.setLastCalculateTime(normalizeDisplayTime(respVO.getLastCalculateTime()));
    }

    private void normalizeOrderDisplayTimes(QmsOqcOrderDO order) {
        order.setInspectionTime(normalizeDisplayTime(order.getInspectionTime()));
        order.setQaTime(normalizeDisplayTime(order.getQaTime()));
        order.setReleaseTime(normalizeDisplayTime(order.getReleaseTime()));
        order.setLastSaveTime(normalizeDisplayTime(order.getLastSaveTime()));
        order.setLastCalculateTime(normalizeDisplayTime(order.getLastCalculateTime()));
    }

    private LocalDateTime normalizeDisplayTime(LocalDateTime time) {
        return isInvalidBusinessTime(time) ? null : time;
    }

    private boolean isInvalidBusinessTime(LocalDateTime time) {
        return time == null || time.isBefore(MIN_VALID_BUSINESS_TIME);
    }

    private QmsOqcPendingRespVO buildPendingResp(HcFgShippingNoticeDO notice, HcFgShippingNoticeItemDO item,
                                                 List<HcFgShippingNoticeItemDO> noticeItems) {
        QmsOqcPendingRespVO respVO = new QmsOqcPendingRespVO();
        respVO.setShippingNoticeId(notice.getId());
        respVO.setShippingNoticeItemId(item.getId());
        respVO.setShippingNo(notice.getNoticeNo());
        respVO.setNoticeNo(notice.getNoticeNo());
        respVO.setCustomerId(notice.getCustomerId());
        respVO.setCustomerCode(notice.getCustomerCode());
        respVO.setProductType(notice.getProductType());
        respVO.setCustomerName(notice.getCustomerName());
        respVO.setMaterialCode(defaultIfBlank(notice.getMaterialCode(), item.getMaterialCode()));
        respVO.setMaterialName(defaultIfBlank(notice.getMaterialName(), item.getMaterialName()));
        respVO.setModelCode(resolveShippingOqcModelCode(notice, item));
        respVO.setSpecification(defaultIfBlank(notice.getProductSize(), item.getProductSize()));
        respVO.setProductSize(respVO.getSpecification());
        respVO.setBatchNo(notice.getNoticeNo());
        respVO.setCustomerBatchNo(item.getCustomerSliceBatchNo());
        List<HcFgShippingNoticeItemDO> summaryItems = noticeItems == null || noticeItems.isEmpty() ? List.of(item) : noticeItems;
        BigDecimal totalPieceQty = resolveShippingPieceQty(summaryItems);
        respVO.setShippingQty(totalPieceQty);
        respVO.setShippingPieceQty(totalPieceQty);
        QmsOqcOrderDO existing = findActiveOqcByShippingNotice(notice.getId(), summaryItems);
        if (existing != null) {
            respVO.setExistingOqcId(existing.getId());
            respVO.setExistingOqcNo(existing.getOqcNo());
            respVO.setExistingStatus(existing.getStatus());
        }
        return respVO;
    }

    private List<QmsOqcPendingRespVO> findPendingScanCandidates(String scanCode) {
        List<QmsOqcPendingRespVO> candidates = getPendingOqcList(scanCode).stream()
                .collect(Collectors.toMap(QmsOqcPendingRespVO::getShippingNoticeItemId, item -> item, (left, right) -> left,
                        LinkedHashMap::new))
                .values()
                .stream()
                .collect(Collectors.toList());
        List<QmsOqcPendingRespVO> exactCandidates = candidates.stream()
                .filter(item -> equalsAny(scanCode, item.getShippingNo(), item.getNoticeNo(), item.getBatchNo(),
                        item.getCustomerBatchNo(), item.getMaterialCode(), item.getModelCode()))
                .collect(Collectors.toList());
        return exactCandidates.isEmpty() ? candidates : exactCandidates;
    }

    private QmsOqcScanRespVO.Candidate buildOrderCandidate(QmsOqcOrderDO order) {
        return BeanUtils.toBean(order, QmsOqcScanRespVO.Candidate.class);
    }

    private QmsOqcScanRespVO.Candidate buildPendingCandidate(QmsOqcPendingRespVO pending) {
        QmsOqcScanRespVO.Candidate candidate = new QmsOqcScanRespVO.Candidate();
        candidate.setId(pending.getExistingOqcId());
        candidate.setOqcNo(pending.getExistingOqcNo());
        candidate.setShippingNoticeId(pending.getShippingNoticeId());
        candidate.setShippingNoticeItemId(pending.getShippingNoticeItemId());
        candidate.setShippingNo(pending.getShippingNo());
        candidate.setNoticeNo(pending.getNoticeNo());
        candidate.setCustomerName(pending.getCustomerName());
        candidate.setMaterialCode(pending.getMaterialCode());
        candidate.setMaterialName(pending.getMaterialName());
        candidate.setModelCode(pending.getModelCode());
        candidate.setBatchNo(pending.getBatchNo());
        candidate.setCustomerBatchNo(pending.getCustomerBatchNo());
        candidate.setShippingQty(pending.getShippingQty());
        candidate.setStatus(pending.getExistingStatus());
        return candidate;
    }

    private void fillSingleScanResp(QmsOqcScanRespVO respVO, QmsOqcOrderDO order, String targetType) {
        respVO.setScanTargetType(targetType);
        respVO.setMatchedOqcId(order.getId());
        respVO.setMatchedOqcNo(order.getOqcNo());
        respVO.setCandidateCount(1);
        respVO.setRecord(getOqcResp(order.getId()));
        if (isReadonlyStatus(order.getStatus())) {
            respVO.setMatchResult(SCAN_RESULT_STATUS_BLOCKED);
            respVO.setOpenTarget(STATUS_CANCELED.equals(order.getStatus()) ? null : OPEN_TARGET_REPORT);
            respVO.setMessage(readonlyMessage(order.getStatus()));
        } else {
            respVO.setMatchResult(SCAN_RESULT_MATCHED_SINGLE);
            respVO.setOpenTarget(OPEN_TARGET_ENTRY);
            respVO.setMessage("已命中 OQC 出货检验单，准备进入录入");
        }
    }

    private String resolveOqcScanTarget(String scanCode, QmsOqcOrderDO order) {
        if (Objects.equals(scanCode, order.getOqcNo())) {
            return SCAN_TARGET_OQC_NO;
        }
        if (equalsAny(scanCode, order.getShippingNo(), order.getNoticeNo())) {
            return SCAN_TARGET_SHIPPING_NOTICE;
        }
        return SCAN_TARGET_BATCH_OR_MATERIAL;
    }

    private boolean isReadonlyStatus(String status) {
        return STATUS_COMPLETED.equals(status) || STATUS_REJECTED.equals(status) || STATUS_CANCELED.equals(status);
    }

    private String readonlyMessage(String status) {
        if (STATUS_COMPLETED.equals(status)) {
            return "该 OQC 出货检验单已完成，只能查看详情";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "该 OQC 出货检验单已拦截，只能查看详情";
        }
        if (STATUS_CANCELED.equals(status)) {
            return "该 OQC 出货检验单已取消，不能继续填写";
        }
        return "当前单据状态不允许扫码填写";
    }

    private boolean equalsAny(String source, String... targets) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (String target : targets) {
            if (source.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private QmsOqcStandardRespVO.StandardItem buildStandardItem(QmsQualityStandardItemDO item) {
        QmsOqcStandardRespVO.StandardItem resp = BeanUtils.toBean(item, QmsOqcStandardRespVO.StandardItem.class);
        resp.setStandardItemId(item.getId());
        resp.setCategory(resolveCategory(item));
        resp.setMinValueLimit(item.getMinValue());
        resp.setMaxValueLimit(item.getMaxValue());
        return resp;
    }

    private QmsOqcOrderDO buildProgressUpdate(Long oqcId, List<QmsOqcItemDO> items) {
        QmsOqcOrderDO updateObj = new QmsOqcOrderDO();
        updateObj.setId(oqcId);
        int required = items == null ? 0 : items.size();
        int completed = items == null ? 0 : (int) items.stream()
                .filter(item -> JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult()) || JUDGMENT_NA.equals(item.getItemResult()))
                .count();
        int abnormal = items == null ? 0 : (int) items.stream().filter(item -> JUDGMENT_NG.equals(item.getItemResult())).count();
        updateObj.setRequiredItemCount(required);
        updateObj.setCompletedItemCount(completed);
        updateObj.setAbnormalItemCount(abnormal);
        updateObj.setEntryProgress(required == 0 ? 0 : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(required), 0, RoundingMode.DOWN).intValue());
        return updateObj;
    }

    private QmsOqcSaveReqVO.OqcItem findIncomingItem(QmsOqcItemDO existingItem, List<QmsOqcSaveReqVO.OqcItem> items) {
        return items.stream().filter(item ->
                (item.getId() != null && item.getId().equals(existingItem.getId()))
                        || (item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId())))
                .findFirst().orElse(null);
    }

    private boolean matchesExistingItem(QmsOqcSaveReqVO.OqcItem item, List<QmsOqcItemDO> existingItems) {
        return existingItems.stream().anyMatch(existingItem ->
                (item.getId() != null && item.getId().equals(existingItem.getId()))
                        || (item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId())));
    }

    private QmsOqcOrderDO validateOqcExists(Long id) {
        QmsOqcOrderDO entity = qmsOqcOrderMapper.selectById(id);
        if (entity == null) {
            throw exception(HCOQC_NOT_EXISTS);
        }
        return entity;
    }

    private void validateEditable(QmsOqcOrderDO entity) {
        if (STATUS_COMPLETED.equals(entity.getStatus()) || STATUS_REJECTED.equals(entity.getStatus()) || STATUS_CANCELED.equals(entity.getStatus())) {
            throw exception(HCOQC_FINISHED_LOCKED);
        }
    }

    private void validateEntryEditable(QmsOqcOrderDO entity) {
        validateEditable(entity);
        if (Boolean.TRUE.equals(entity.getSheetLocked()) || STATUS_WAITING_QA.equals(entity.getStatus())) {
            throw exception(HCOQC_FINISHED_LOCKED);
        }
    }

    private void validateOqcNoUnique(Long id, String oqcNo) {
        if (qmsOqcOrderMapper.selectByOqcNo(oqcNo, id) != null) {
            throw exception(HCOQC_NO_EXISTS);
        }
    }

    private void validateItems(List<QmsOqcSaveReqVO.OqcItem> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCOQC_ITEMS_EMPTY);
        }
    }

    private void validateItemsExist(Collection<QmsOqcItemDO> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCOQC_ITEMS_EMPTY);
        }
    }

    private boolean isNgSample(QmsOqcSaveReqVO.OqcSample sample) {
        return JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue());
    }

    private boolean isCompletedSample(QmsOqcSaveReqVO.OqcSample sample) {
        return resolveMeasuredValue(sample) != null
                || JUDGMENT_OK.equals(sample.getQualitativeValue())
                || JUDGMENT_NG.equals(sample.getQualitativeValue())
                || JUDGMENT_NA.equals(sample.getQualitativeValue())
                || JUDGMENT_OK.equals(sample.getSampleResult())
                || JUDGMENT_NG.equals(sample.getSampleResult())
                || JUDGMENT_NA.equals(sample.getSampleResult());
    }

    private BigDecimal resolveMeasuredValue(QmsOqcSaveReqVO.OqcSample sample) {
        return sample.getMeasuredValue() == null ? sample.getResultValue() : sample.getMeasuredValue();
    }

    private QmsOqcOrderDO findActiveOqcByShippingNotice(Long shippingNoticeId, List<HcFgShippingNoticeItemDO> items) {
        QmsOqcOrderDO existing = qmsOqcOrderMapper.selectActiveByShippingNoticeId(shippingNoticeId, STATUS_CANCELED);
        if (existing != null) {
            return existing;
        }
        List<Long> oqcOrderIds = items == null ? Collections.emptyList() : items.stream()
                .map(HcFgShippingNoticeItemDO::getOqcOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return qmsOqcOrderMapper.selectFirstActiveByIds(oqcOrderIds, STATUS_CANCELED);
    }

    private BigDecimal resolveShippingPieceQty(List<HcFgShippingNoticeItemDO> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int totalQty = items.stream()
                .mapToInt(this::resolveShippingItemPieceQty)
                .sum();
        return BigDecimal.valueOf(totalQty);
    }

    private int resolveShippingItemPieceQty(HcFgShippingNoticeItemDO item) {
        if (item.getActualShipQty() != null && item.getActualShipQty() > 0) {
            return item.getActualShipQty();
        }
        if (item.getLockedQty() != null && item.getLockedQty() > 0) {
            return item.getLockedQty();
        }
        if (item.getStockQty() != null && item.getStockQty() > 0) {
            return item.getStockQty();
        }
        return 1;
    }

    private String resolveCategory(QmsQualityStandardItemDO item) {
        if (StringUtils.hasText(item.getSheetSectionCode())) {
            String sheetSectionCode = item.getSheetSectionCode().trim();
            if (sheetSectionCode.startsWith("OQC_COA")) {
                return "COA";
            }
            if (sheetSectionCode.startsWith("OQC_LABEL")) {
                return "LABEL";
            }
            if (sheetSectionCode.startsWith("OQC_PACKING")) {
                return "PACKING";
            }
            if (sheetSectionCode.startsWith("OQC_OTHER")) {
                return "OTHER";
            }
        }
        String text = (item.getInspectionItem() == null ? "" : item.getInspectionItem()) + (item.getStandardDesc() == null ? "" : item.getStandardDesc());
        if (text.contains("CoA") || text.contains("COA")) {
            return "COA";
        }
        if (text.contains("包装") || text.contains("外箱") || text.contains("装柜")) {
            return "PACKING";
        }
        if (text.contains("标签") || text.contains("标识") || text.contains("条码")) {
            return "LABEL";
        }
        return "PRODUCT";
    }

    private String generateOqcNo() {
        return qmsNoGeneratorService.generateNo(APPLY_TYPE_OQC);
    }

    private String generateNcrNo() {
        return "NCR-OQC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.valueOf(System.currentTimeMillis() % 1_000_000L);
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private String buildOqcAuditBizName(QmsOqcOrderDO order) {
        List<String> parts = new ArrayList<>();
        appendBizNamePart(parts, order.getShippingNo());
        appendBizNamePart(parts, order.getNoticeNo());
        appendBizNamePart(parts, order.getMaterialName());
        appendBizNamePart(parts, order.getMaterialCode());
        appendBizNamePart(parts, order.getBatchNo());
        return parts.isEmpty() ? defaultIfBlank(order.getOqcNo(), "") : String.join(" / ", parts);
    }

    private void appendBizNamePart(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) && !"-".equals(value) ? value : defaultValue;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String resolveShippingOqcModelCode(HcFgShippingNoticeDO notice, HcFgShippingNoticeItemDO item) {
        String productType = defaultIfBlank(notice.getProductType(), "");
        if ("SAMPLE".equalsIgnoreCase(productType) || "RND".equalsIgnoreCase(productType)
                || "样品".equals(productType) || "研发".equals(productType)) {
            return defaultIfBlank(notice.getExternalProductModel(),
                    defaultIfBlank(item.getCustomerModelCode(),
                            defaultIfBlank(item.getInternalModelCode(),
                                    defaultIfBlank(item.getModelCode(), notice.getModelCode()))));
        }
        return defaultIfBlank(item.getInternalModelCode(),
                defaultIfBlank(item.getCustomerModelCode(),
                        defaultIfBlank(item.getModelCode(), notice.getModelCode())));
    }

}
