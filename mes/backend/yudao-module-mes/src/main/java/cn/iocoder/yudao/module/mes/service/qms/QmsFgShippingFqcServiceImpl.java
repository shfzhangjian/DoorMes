package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentNoticeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcAlignmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcPendingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcShippingDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticePickItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDefectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcScanRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticePickItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleDefectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcScanRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcFinishedPackagingService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class QmsFgShippingFqcServiceImpl implements QmsFgShippingFqcService {

    private static final String SOURCE_MODULE_FG_SHIPPING_FQC = "FG_SHIPPING_FQC";
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
    private static final String ALIGNMENT_PENDING = "PENDING";
    private static final String ALIGNMENT_ALIGNED = "ALIGNED";
    private static final String ALIGNMENT_MISMATCH = "MISMATCH";
    private static final String NOTICE_STATUS_PICKED = "PICKED";
    private static final String NOTICE_STATUS_INSPECTED = "INSPECTED";
    private static final String SCAN_TARGET_ACTUAL_SLICE_BATCH_NO = "ACTUAL_SLICE_BATCH_NO";
    private static final String SCAN_TARGET_FQC_ORDER = "FQC_ORDER";
    private static final String SCAN_RESULT_MATCHED_SINGLE = "MATCHED_SINGLE";
    private static final String SCAN_RESULT_NOT_FOUND = "NOT_FOUND";
    private static final String OPEN_TARGET_WORKBENCH = "WORKBENCH";
    private static final String ENTRY_LAYOUT_PROGRAM_FORM = "PROGRAM_FORM";
    private static final String ENTRY_MODE_MANUAL = "MANUAL";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_FILLING = "FILLING";
    private static final Integer STANDARD_STATUS_ENABLED = 1;
    private static final Integer STANDARD_AUDIT_STATUS_AUDITED = 20;
    private static final String SAMPLE_ROLE_OPERATOR = "OPERATOR";
    private static final String VALUE_SOURCE_MANUAL = "MANUAL";

    private static final Set<String> ACTIVE_PICK_STATUSES = Set.of("PICKED", "SHIP_CONFIRMED", "INSPECTED", "PACKAGED", "LOCKED", "OUTBOUND");
    private static final Set<String> CREATE_ALLOWED_NOTICE_STATUSES = Set.of("SHIP_CONFIRMED", "INSPECTED");

    @Resource
    private QmsFqcService qmsFqcService;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Resource
    private QmsFqcItemMapper qmsFqcItemMapper;
    @Resource
    private QmsFqcSampleMapper qmsFqcSampleMapper;
    @Resource
    private QmsFqcSampleDefectMapper qmsFqcSampleDefectMapper;
    @Resource
    private QmsFqcScanRecordMapper qmsFqcScanRecordMapper;
    @Resource
    private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;
    @Resource
    private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Resource
    private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Resource
    private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    @Lazy
    private HcFinishedPackagingService hcFinishedPackagingService;

    @Override
    public List<QmsFgShippingFqcPendingRespVO> getPendingList(String keyword) {
        return hcFgShippingNoticeMapper.selectOutboundNoticeList(StrUtil.trimToNull(keyword)).stream()
                .filter(notice -> CREATE_ALLOWED_NOTICE_STATUSES.contains(StrUtil.blankToDefault(notice.getNoticeStatus(), "")))
                .map(this::buildPendingResp)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingFqcRespVO createFromShippingNotice(Long shippingNoticeId) {
        HcFgShippingNoticeDO notice = validateShippingNoticeForCreate(shippingNoticeId);
        List<HcFgShippingNoticePickItemDO> pickItems = selectActivePickItems(notice.getId());
        if (pickItems.isEmpty()) {
            throw invalidParamException("发货通知单没有已配货明细，不能生成发货成品检验单");
        }
        QmsFqcOrderDO latestOrder = qmsFqcOrderMapper.selectLatestBySource(SOURCE_MODULE_FG_SHIPPING_FQC, notice.getId());
        if (latestOrder != null && !STATUS_COMPLETED.equals(latestOrder.getStatus())) {
            upsertShippingDetails(latestOrder, notice, pickItems);
            ensurePieceInspectionItems(latestOrder.getId());
            syncActualValueRequiredSnapshot(latestOrder);
            removeObsoleteShippingDetails(latestOrder.getId(), pickItems.stream()
                    .map(HcFgShippingNoticePickItemDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
            syncShippingDetailJudgmentToPickItems(latestOrder.getId(), null);
            refreshOrderItemProgress(latestOrder.getId());
            refreshOrderShippingStats(latestOrder.getId());
            return get(latestOrder.getId());
        }
        List<HcFgShippingNoticePickItemDO> supplementalPickItems = latestOrder == null
                ? pickItems
                : filterUninspectedSupplementalPickItems(notice.getId(), pickItems);
        if (supplementalPickItems.isEmpty()) {
            if (latestOrder != null) {
                return get(latestOrder.getId());
            }
            throw invalidParamException("发货通知单没有需要检验的新增配货片");
        }
        Long fqcId = qmsFqcService.createFqc(buildCreateReq(notice, supplementalPickItems));
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(fqcId);
        markSupplementalFqc(order, latestOrder);
        order = qmsFqcOrderMapper.selectById(fqcId);
        upsertShippingDetails(order, notice, supplementalPickItems);
        ensurePieceInspectionItems(order.getId());
        syncActualValueRequiredSnapshot(order);
        removeObsoleteShippingDetails(order.getId(), supplementalPickItems.stream()
                .map(HcFgShippingNoticePickItemDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        syncShippingDetailJudgmentToPickItems(order.getId(), null);
        refreshOrderItemProgress(order.getId());
        refreshOrderShippingStats(order.getId());
        return get(order.getId());
    }

    @Override
    public PageResult<QmsFgShippingFqcRespVO> getPage(QmsFgShippingFqcPageReqVO pageReqVO) {
        List<Long> fqcIds = resolveFilterFqcIds(pageReqVO);
        if (hasDetailFilter(pageReqVO) && (fqcIds == null || fqcIds.isEmpty())) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<QmsFqcOrderDO> wrapper = new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_FG_SHIPPING_FQC)
                .inIfPresent(QmsFqcOrderDO::getId, fqcIds)
                .likeIfPresent(QmsFqcOrderDO::getFqcNo, pageReqVO.getFqcNo())
                .likeIfPresent(QmsFqcOrderDO::getSourceReportNo, pageReqVO.getShippingNoticeNo())
                .likeIfPresent(QmsFqcOrderDO::getMaterialCode, pageReqVO.getMaterialCode())
                .likeIfPresent(QmsFqcOrderDO::getProductModel, pageReqVO.getModelCode())
                .eqIfPresent(QmsFqcOrderDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(QmsFqcOrderDO::getJudgment, pageReqVO.getJudgment())
                .eqIfPresent(QmsFqcOrderDO::getRecheckFlag, pageReqVO.getRecheckFlag())
                .betweenIfPresent(QmsFqcOrderDO::getSubmissionTime, pageReqVO.getSubmissionTime())
                .orderByDesc(QmsFqcOrderDO::getId);
        PageResult<QmsFqcOrderDO> page = qmsFqcOrderMapper.selectPage(pageReqVO, wrapper);
        List<QmsFgShippingFqcRespVO> records = BeanUtils.toBean(page.getList(), QmsFgShippingFqcRespVO.class);
        records.forEach(this::fillRecheckSource);
        fillPageShippingSummary(records);
        return new PageResult<>(records, page.getTotal());
    }

    private boolean hasDetailFilter(QmsFgShippingFqcPageReqVO pageReqVO) {
        return StrUtil.isNotBlank(pageReqVO.getActualSliceBatchNo())
                || StrUtil.isNotBlank(pageReqVO.getAlignmentStatus())
                || StrUtil.isNotBlank(pageReqVO.getCustomerName())
                || StrUtil.isNotBlank(pageReqVO.getErpOrderNo());
    }

    private List<HcFgShippingNoticePickItemDO> filterUninspectedSupplementalPickItems(Long noticeId,
                                                                                         List<HcFgShippingNoticePickItemDO> pickItems) {
        if (pickItems == null || pickItems.isEmpty()) {
            return Collections.emptyList();
        }
        List<QmsFqcOrderDO> completedOrders = qmsFqcOrderMapper.selectListBySource(SOURCE_MODULE_FG_SHIPPING_FQC, noticeId).stream()
                .filter(order -> STATUS_COMPLETED.equals(order.getStatus()))
                .toList();
        if (completedOrders.isEmpty()) {
            return pickItems;
        }
        Set<Long> inspectedOkPickItemIds = qmsFqcShippingDetailMapper.selectListByFqcIds(completedOrders.stream()
                        .map(QmsFqcOrderDO::getId)
                        .toList())
                .stream()
                .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                .map(QmsFqcShippingDetailDO::getShippingPickItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return pickItems.stream()
                .filter(item -> !inspectedOkPickItemIds.contains(item.getId()))
                .toList();
    }

    private void markSupplementalFqc(QmsFqcOrderDO order, QmsFqcOrderDO previousOrder) {
        if (order == null || previousOrder == null) {
            return;
        }
        Long rootInspectionId = previousOrder.getRejectRootInspectionId() == null
                ? previousOrder.getId() : previousOrder.getRejectRootInspectionId();
        String rootInspectionNo = StrUtil.blankToDefault(previousOrder.getRejectRootInspectionNo(), previousOrder.getFqcNo());
        int recheckRoundNo = (previousOrder.getRecheckRoundNo() == null ? 0 : previousOrder.getRecheckRoundNo()) + 1;
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, order.getId())
                .set(QmsFqcOrderDO::getRecheckFlag, true)
                .set(QmsFqcOrderDO::getRecheckGroupId, rootInspectionId)
                .set(QmsFqcOrderDO::getRecheckRoundNo, recheckRoundNo)
                .set(QmsFqcOrderDO::getRejectPrevInspectionId, previousOrder.getId())
                .set(QmsFqcOrderDO::getRejectPrevInspectionNo, previousOrder.getFqcNo())
                .set(QmsFqcOrderDO::getRejectRootInspectionId, rootInspectionId)
                .set(QmsFqcOrderDO::getRejectRootInspectionNo, rootInspectionNo)
                .set(QmsFqcOrderDO::getRemark, appendRemark(order.getRemark(), "补配新增片检验，来源检验单：" + previousOrder.getFqcNo())));
    }

    private List<QmsFqcShippingDetailDO> selectQualifiedActiveShippingDetails(Long shippingNoticeId) {
        List<QmsFqcOrderDO> completedOrders = qmsFqcOrderMapper.selectListBySource(SOURCE_MODULE_FG_SHIPPING_FQC, shippingNoticeId).stream()
                .filter(order -> STATUS_COMPLETED.equals(order.getStatus()))
                .toList();
        if (completedOrders.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> activePickItemIds = selectActivePickItems(shippingNoticeId).stream()
                .map(HcFgShippingNoticePickItemDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return qmsFqcShippingDetailMapper.selectListByFqcIds(completedOrders.stream()
                        .map(QmsFqcOrderDO::getId)
                        .toList())
                .stream()
                .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                .filter(detail -> activePickItemIds.contains(detail.getShippingPickItemId()))
                .toList();
    }

    @Override
    public QmsFgShippingFqcRespVO get(Long id) {
        QmsFqcOrderDO order = validateFgShippingFqc(id);
        ensurePieceInspectionItems(order.getId());
        syncActualValueRequiredSnapshot(order);
        QmsFgShippingFqcRespVO respVO = BeanUtils.toBean(order, QmsFgShippingFqcRespVO.class);
        fillRecheckSource(respVO);
        HcFgShippingNoticeDO sourceNotice = hcFgShippingNoticeMapper.selectById(order.getSourceReportId());
        respVO.setProductType(sourceNotice == null ? null : sourceNotice.getProductType());
        QmsFqcRespVO fqcResp = qmsFqcService.getFqcResp(order.getId());
        List<QmsFgShippingFqcRespVO.ShippingDetail> detailRespList = BeanUtils.toBean(
                qmsFqcShippingDetailMapper.selectListByFqcId(order.getId()),
                QmsFgShippingFqcRespVO.ShippingDetail.class);
        fillShippingDetailItems(detailRespList, fqcResp.getItems());
        List<QmsFgShippingFqcRespVO.AlignmentRow> alignmentRows =
                buildAlignmentRows(order.getSourceReportId(), detailRespList);
        fillRecordShippingSummary(respVO, detailRespList, alignmentRows);
        respVO.setShippingDetails(detailRespList);
        respVO.setAlignmentRows("SAMPLE".equals(respVO.getProductType()) ? Collections.emptyList() : alignmentRows);
        respVO.setAlignmentCandidates("SAMPLE".equals(respVO.getProductType()) ? Collections.emptyList() : buildAlignmentCandidates(order.getId()));
        respVO.setItems(fqcResp.getItems());
        respVO.setAbnormals(fqcResp.getAbnormals());
        return respVO;
    }

    @Override
    public List<QmsDefectCodeRespVO> getDefectCodeOptions() {
        return qmsFqcService.getFqcDefectCodeOptions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingFqcRespVO saveProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcOrderDO order = validateFgShippingFqc(saveReqVO.getId());
        validateFgShippingFqcEditable(order);
        syncActualValueRequiredSnapshot(order);
        List<QmsFqcItemDO> savedItems = saveFgShippingProgramItems(order, saveReqVO.getItems());
        List<Long> affectedDetailIds = resolveAffectedShippingDetailIds(savedItems);
        refreshShippingDetailsFromItems(saveReqVO.getId(), affectedDetailIds);
        syncShippingDetailJudgmentToPickItems(saveReqVO.getId(), affectedDetailIds);
        refreshOrderItemProgress(saveReqVO.getId());
        refreshOrderShippingStats(saveReqVO.getId());
        refreshOrderDraftStatus(order, saveReqVO);
        return buildProgramEntryLightResp(saveReqVO.getId(), savedItems, affectedDetailIds);
    }

    private List<QmsFqcItemDO> saveFgShippingProgramItems(QmsFqcOrderDO order,
                                                          List<QmsFqcSaveReqVO.FqcItem> incomingItems) {
        if (incomingItems == null || incomingItems.isEmpty()) {
            throw invalidParamException("本次保存的检验项目不能为空");
        }
        Map<Long, QmsFqcSaveReqVO.FqcItem> incomingItemMap = new LinkedHashMap<>();
        List<QmsFqcItemDO> existingItems = new ArrayList<>();
        for (QmsFqcSaveReqVO.FqcItem incomingItem : incomingItems) {
            if (incomingItem == null || incomingItem.getId() == null) {
                throw invalidParamException("发货成品检验项目必须携带项目ID，禁止按标准项兜底匹配");
            }
            if (incomingItemMap.putIfAbsent(incomingItem.getId(), incomingItem) != null) {
                throw invalidParamException("本次保存包含重复的检验项目ID：" + incomingItem.getId());
            }
            QmsFqcItemDO existingItem = qmsFqcItemMapper.selectById(incomingItem.getId());
            validateFgShippingIncomingItem(order, existingItem, incomingItem);
            validateFgShippingIncomingSamples(existingItem, incomingItem.getSamples());
            existingItems.add(existingItem);
        }

        List<Long> itemIds = existingItems.stream().map(QmsFqcItemDO::getId).collect(Collectors.toList());
        qmsFqcSampleDefectMapper.deleteByFqcItemIds(itemIds);
        qmsFqcSampleMapper.deleteByFqcItemIds(itemIds);

        for (QmsFqcItemDO existingItem : existingItems) {
            QmsFqcSaveReqVO.FqcItem incomingItem = incomingItemMap.get(existingItem.getId());
            applyFgShippingActualValue(existingItem, incomingItem);
            applyFgShippingItemStats(existingItem, incomingItem.getSamples());
            qmsFqcItemMapper.updateById(existingItem);
            saveFgShippingSamples(order, existingItem, incomingItem.getSamples());
        }
        return existingItems;
    }

    private void applyFgShippingActualValue(QmsFqcItemDO itemDO, QmsFqcSaveReqVO.FqcItem incomingItem) {
        itemDO.setActualValue(Boolean.TRUE.equals(itemDO.getActualValueRequired())
                ? StrUtil.trimToNull(incomingItem.getActualValue()) : null);
    }

    private void validateFgShippingIncomingItem(QmsFqcOrderDO order, QmsFqcItemDO existingItem,
                                                QmsFqcSaveReqVO.FqcItem incomingItem) {
        if (existingItem == null || Boolean.TRUE.equals(existingItem.getDeleted())
                || !Objects.equals(existingItem.getFqcId(), order.getId())) {
            throw invalidParamException("检验项目不存在或不属于当前发货成品检验单");
        }
        if (existingItem.getSubmissionDetailId() == null) {
            throw invalidParamException("发货成品检验只允许保存片级检验项目");
        }
        if (incomingItem.getSubmissionDetailId() != null
                && !Objects.equals(incomingItem.getSubmissionDetailId(), existingItem.getSubmissionDetailId())) {
            throw invalidParamException("检验项目与发货明细不匹配");
        }
        if (incomingItem.getStandardItemId() != null
                && !Objects.equals(incomingItem.getStandardItemId(), existingItem.getStandardItemId())) {
            throw invalidParamException("检验项目与质量标准项不匹配");
        }
    }

    private void validateFgShippingIncomingSamples(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
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

    private void applyFgShippingItemStats(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
        String result = calculateFgShippingItemResult(itemDO, samples);
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
        itemDO.setCompletedSampleCount((int) samples.stream().filter(this::hasFgShippingSampleValue).count());
        itemDO.setAbnormalSampleCount((int) samples.stream().filter(this::isNgSample).count());
        itemDO.setInputStatus(JUDGMENT_NG.equals(result) ? INPUT_STATUS_ABNORMAL
                : JUDGMENT_OK.equals(result) ? INPUT_STATUS_COMPLETE : INPUT_STATUS_FILLING);
    }

    private String calculateFgShippingItemResult(QmsFqcItemDO itemDO, List<QmsFqcSaveReqVO.FqcSample> samples) {
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

    private boolean hasFgShippingSampleValue(QmsFqcSaveReqVO.FqcSample sample) {
        return sample.getMeasuredValue() != null
                || sample.getResultValue() != null
                || StrUtil.isNotBlank(sample.getRawValuesJson())
                || StrUtil.isNotBlank(sample.getQualitativeValue())
                || JUDGMENT_OK.equals(sample.getSampleResult())
                || JUDGMENT_NG.equals(sample.getSampleResult());
    }

    private void saveFgShippingSamples(QmsFqcOrderDO order, QmsFqcItemDO itemDO,
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
        saveFgShippingSampleDefects(order, itemDO, sampleList, sampleDefectList);
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

    private void saveFgShippingSampleDefects(QmsFqcOrderDO order, QmsFqcItemDO itemDO,
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
        if (hasFgShippingSavedSampleValue(saveReqVO.getItems())) {
            updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setInspectorName(resolveLoginUserName());
            updateObj.setInspectionTime(now);
        }
        qmsFqcOrderMapper.updateById(updateObj);
    }

    private boolean hasFgShippingSavedSampleValue(List<QmsFqcSaveReqVO.FqcItem> items) {
        return items != null && items.stream()
                .filter(Objects::nonNull)
                .map(QmsFqcSaveReqVO.FqcItem::getSamples)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .anyMatch(this::hasFgShippingSampleValue);
    }

    private List<Long> resolveAffectedShippingDetailIds(List<QmsFqcItemDO> savedItems) {
        return (savedItems == null ? Collections.<QmsFqcItemDO>emptyList() : savedItems).stream()
                .map(QmsFqcItemDO::getSubmissionDetailId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private QmsFgShippingFqcRespVO buildProgramEntryLightResp(Long fqcId, List<QmsFqcItemDO> savedItems,
                                                              Collection<Long> affectedDetailIds) {
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(fqcId);
        QmsFgShippingFqcRespVO respVO = BeanUtils.toBean(order, QmsFgShippingFqcRespVO.class);
        fillRecheckSource(respVO);
        HcFgShippingNoticeDO sourceNotice = hcFgShippingNoticeMapper.selectById(order.getSourceReportId());
        respVO.setProductType(sourceNotice == null ? null : sourceNotice.getProductType());
        List<QmsFgShippingFqcRespVO.ShippingDetail> detailRespList = BeanUtils.toBean(
                qmsFqcShippingDetailMapper.selectListByIds(affectedDetailIds),
                QmsFgShippingFqcRespVO.ShippingDetail.class);
        fillShippingDetailStats(detailRespList,
                qmsFqcItemMapper.selectListByFqcIdAndSubmissionDetailIds(fqcId, affectedDetailIds));
        respVO.setShippingDetails(detailRespList);
        respVO.setItems(BeanUtils.toBean(savedItems, QmsFqcRespVO.FqcItem.class));
        respVO.setAbnormals(Collections.emptyList());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingFqcRespVO submitProgramEntry(QmsFqcSaveReqVO saveReqVO) {
        QmsFqcOrderDO order = validateFgShippingFqc(saveReqVO.getId());
        validateFgShippingFqcEditable(order);
        syncActualValueRequiredSnapshot(order);
        saveFgShippingProgramItems(order, saveReqVO.getItems());
        refreshShippingDetailsFromItems(saveReqVO.getId());
        syncShippingDetailJudgmentToPickItems(saveReqVO.getId(), null);
        refreshOrderItemProgress(saveReqVO.getId());
        refreshOrderShippingStats(saveReqVO.getId());
        List<QmsFqcShippingDetailDO> details = qmsFqcShippingDetailMapper.selectListByFqcId(saveReqVO.getId());
        validateInspectionItemsCompleted(saveReqVO.getId(), details);
        validateRequiredActualValues(saveReqVO.getId());
        validateShippingDetailsCompleted(details);
        LocalDateTime now = LocalDateTime.now();
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, saveReqVO.getId())
                .set(QmsFqcOrderDO::getStatus, STATUS_WAITING_QA)
                .set(QmsFqcOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsFqcOrderDO::getReleaseResult, null)
                .set(QmsFqcOrderDO::getSubmissionTime, now)
                .set(QmsFqcOrderDO::getSubmitterName, resolveLoginUserName())
                .set(QmsFqcOrderDO::getSheetLocked, true));
        return get(saveReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingFqcRespVO saveShippingDetails(QmsFgShippingFqcShippingDetailSaveReqVO reqVO) {
        QmsFqcOrderDO order = validateFgShippingFqc(reqVO.getFqcId());
        validateFgShippingFqcEditable(order);
        LocalDateTime now = LocalDateTime.now();
        List<Long> changedDetailIds = new ArrayList<>();
        for (QmsFgShippingFqcShippingDetailSaveReqVO.Detail detailReq : reqVO.getDetails()) {
            QmsFqcShippingDetailDO detail = qmsFqcShippingDetailMapper.selectById(detailReq.getId());
            if (detail == null || Boolean.TRUE.equals(detail.getDeleted())
                    || !Objects.equals(detail.getFqcId(), order.getId())) {
                throw invalidParamException("发货成品检验明细不存在或不属于当前FQC单");
            }
            changedDetailIds.add(detail.getId());
            String rowJudgment = normalizeRowJudgment(detailReq.getRowJudgment());
            qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                    .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                    .set(QmsFqcShippingDetailDO::getRowJudgment, rowJudgment)
                    .set(QmsFqcShippingDetailDO::getDefectCode, detailReq.getDefectCode())
                    .set(QmsFqcShippingDetailDO::getDefectName, detailReq.getDefectName())
                    .set(QmsFqcShippingDetailDO::getNgReason, detailReq.getNgReason())
                    .set(QmsFqcShippingDetailDO::getRemark, detailReq.getRemark())
                    .set(QmsFqcShippingDetailDO::getInspectorId, SecurityFrameworkUtils.getLoginUserId())
                    .set(QmsFqcShippingDetailDO::getInspectorName, resolveLoginUserName())
                    .set(QmsFqcShippingDetailDO::getInspectionTime, JUDGMENT_PENDING.equals(rowJudgment) ? null : now));
        }
        syncShippingDetailJudgmentToPickItems(order.getId(), changedDetailIds);
        refreshOrderShippingStats(order.getId());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<QmsFgShippingAlignmentNoticeRespVO> getAlignmentNoticeList(String keyword) {
        return findAlignmentNoticeIds(keyword).stream()
                .map(this::buildAlignmentNotice)
                .toList();
    }

    @Override
    public PageResult<QmsFgShippingAlignmentNoticeRespVO> getAlignmentNoticePage(
            QmsFgShippingAlignmentPageReqVO pageReqVO) {
        List<Long> noticeIds = findAlignmentNoticeIds(pageReqVO.getKeyword());
        int total = noticeIds.size();
        int fromIndex = Math.min((pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize(), total);
        int toIndex = Math.min(fromIndex + pageReqVO.getPageSize(), total);
        List<QmsFgShippingAlignmentNoticeRespVO> records = noticeIds.subList(fromIndex, toIndex).stream()
                .map(this::buildAlignmentNotice)
                .toList();
        return new PageResult<>(records, (long) total);
    }

    private List<Long> findAlignmentNoticeIds(String keyword) {
        List<QmsFqcOrderDO> orders = qmsFqcOrderMapper.selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_FG_SHIPPING_FQC)
                .eq(QmsFqcOrderDO::getDeleted, false)
                .orderByDesc(QmsFqcOrderDO::getId));
        Set<Long> noticeIds = orders.stream()
                .filter(order -> STATUS_COMPLETED.equals(order.getStatus()))
                .map(QmsFqcOrderDO::getSourceReportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (noticeIds.isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedKeyword = StrUtil.trimToEmpty(keyword);
        Set<Long> matchedNoticeIds = hcFgShippingNoticeMapper.selectList(new LambdaQueryWrapperX<HcFgShippingNoticeDO>()
                        .in(HcFgShippingNoticeDO::getId, noticeIds)
                        .eq(HcFgShippingNoticeDO::getDeleted, false)
                        .eq(HcFgShippingNoticeDO::getNoticeStatus, NOTICE_STATUS_INSPECTED)
                        .and(w -> w.ne(HcFgShippingNoticeDO::getProductType, "SAMPLE").or().isNull(HcFgShippingNoticeDO::getProductType))
                        .and(StrUtil.isNotBlank(normalizedKeyword), wrapper -> wrapper
                                .like(HcFgShippingNoticeDO::getNoticeNo, normalizedKeyword)
                                .or()
                                .like(HcFgShippingNoticeDO::getCustomerName, normalizedKeyword)
                                .or()
                                .like(HcFgShippingNoticeDO::getErpOrderNo, normalizedKeyword)
                                .or()
                                .like(HcFgShippingNoticeDO::getMaterialCode, normalizedKeyword)
                                .or()
                                .like(HcFgShippingNoticeDO::getModelCode, normalizedKeyword)))
                .stream()
                .map(HcFgShippingNoticeDO::getId)
                .collect(Collectors.toSet());
        return noticeIds.stream().filter(matchedNoticeIds::contains).toList();
    }

    private QmsFgShippingAlignmentNoticeRespVO buildAlignmentNotice(Long noticeId) {
        QmsFgShippingAlignmentRespVO alignment = getAlignment(noticeId);
        QmsFgShippingAlignmentNoticeRespVO item = new QmsFgShippingAlignmentNoticeRespVO();
        item.setShippingNoticeId(alignment.getShippingNoticeId());
        item.setShippingNoticeNo(alignment.getShippingNoticeNo());
        item.setCustomerName(alignment.getCustomerName());
        item.setErpOrderNo(alignment.getErpOrderNo());
        item.setMaterialCode(alignment.getMaterialCode());
        item.setMaterialName(alignment.getMaterialName());
        item.setModelCode(alignment.getModelCode());
        item.setNoticeStatus(alignment.getNoticeStatus());
        item.setPlannedCount(alignment.getPlannedCount());
        item.setAlignedCount(alignment.getAlignedCount());
        item.setCandidateCount(alignment.getCandidateCount());
        return item;
    }

    @Override
    public QmsFgShippingAlignmentRespVO getAlignment(Long shippingNoticeId) {
        HcFgShippingNoticeDO notice = shippingNoticeId == null ? null : hcFgShippingNoticeMapper.selectById(shippingNoticeId);
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货通知单不存在");
        }
        assertBatchAlignmentRequired(notice);
        List<HcFgShippingNoticeItemDO> noticeItems = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId());
        List<QmsFqcShippingDetailDO> qualifiedDetails = selectQualifiedActiveShippingDetails(notice.getId());
        Map<Long, QmsFqcShippingDetailDO> alignedDetailMap = qualifiedDetails.stream()
                .filter(detail -> detail.getShippingNoticeItemId() != null)
                .filter(detail -> ALIGNMENT_ALIGNED.equals(detail.getAlignmentStatus()))
                .collect(Collectors.toMap(QmsFqcShippingDetailDO::getShippingNoticeItemId,
                        detail -> detail, (left, right) -> right, LinkedHashMap::new));
        List<QmsFgShippingFqcRespVO.AlignmentRow> rows = noticeItems.stream()
                .<QmsFgShippingFqcRespVO.AlignmentRow>map(item -> buildAlignmentRow(item,
                        alignedDetailMap.get(item.getId())))
                .collect(Collectors.toList());
        List<QmsFgShippingFqcRespVO.AlignmentCandidate> candidates = buildAlignmentCandidates(notice.getId(), qualifiedDetails);
        int alignedCount = (int) rows.stream()
                .filter(row -> ALIGNMENT_ALIGNED.equals(row.getAlignmentStatus()))
                .count();
        QmsFgShippingAlignmentRespVO respVO = new QmsFgShippingAlignmentRespVO();
        respVO.setShippingNoticeId(notice.getId());
        respVO.setShippingNoticeNo(notice.getNoticeNo());
        respVO.setCustomerName(notice.getCustomerName());
        respVO.setErpOrderNo(notice.getErpOrderNo());
        respVO.setMaterialCode(notice.getMaterialCode());
        respVO.setMaterialName(notice.getMaterialName());
        respVO.setModelCode(notice.getModelCode());
        respVO.setNoticeStatus(notice.getNoticeStatus());
        respVO.setPlannedCount(rows.size());
        respVO.setAlignedCount(alignedCount);
        respVO.setCandidateCount(candidates.size());
        respVO.setAlignmentCompleted(!rows.isEmpty() && alignedCount == rows.size());
        respVO.setRows(rows);
        respVO.setCandidates(candidates);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingAlignmentRespVO saveShippingAlignment(QmsFgShippingAlignmentSaveReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getShippingNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货通知单不存在，不能保存客户批号对齐");
        }
        assertBatchAlignmentRequired(notice);
        validateShippingAlignmentEditable(notice);
        List<QmsFqcOrderDO> orders = qmsFqcOrderMapper.selectListBySource(SOURCE_MODULE_FG_SHIPPING_FQC, notice.getId());
        Map<Long, QmsFqcOrderDO> completedOrderMap = orders.stream()
                .filter(order -> STATUS_COMPLETED.equals(order.getStatus()))
                .collect(Collectors.toMap(QmsFqcOrderDO::getId, order -> order, (left, right) -> right));
        if (completedOrderMap.isEmpty()) {
            throw invalidParamException("当前没有已审核完成的发货成品检验单，不能对齐");
        }
        List<QmsFqcShippingDetailDO> allCompletedDetails = qmsFqcShippingDetailMapper
                .selectListByFqcIds(new ArrayList<>(completedOrderMap.keySet()));
        Map<Long, HcFgShippingNoticeItemDO> noticeItemMap = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId()).stream()
                .collect(Collectors.toMap(HcFgShippingNoticeItemDO::getId, item -> item, (left, right) -> left));
        Map<Long, HcFgShippingNoticePickItemDO> pickItemMap = selectActivePickItems(notice.getId()).stream()
                .collect(Collectors.toMap(HcFgShippingNoticePickItemDO::getId, item -> item, (left, right) -> left));
        Map<Long, QmsFqcShippingDetailDO> candidateDetailMap = allCompletedDetails.stream()
                .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                .filter(detail -> detail.getShippingPickItemId() != null && pickItemMap.containsKey(detail.getShippingPickItemId()))
                .collect(Collectors.toMap(QmsFqcShippingDetailDO::getShippingPickItemId,
                        detail -> detail, (left, right) -> right));
        validateAlignmentSnapshot(reqVO.getDetails(), noticeItemMap);
        Set<Long> usedPickItemIds = new HashSet<>();
        Set<String> usedActualSliceBatchNoSet = new HashSet<>();
        Set<Long> affectedFqcIds = new HashSet<>();
        int rowNo = 1;
        for (QmsFgShippingAlignmentSaveReqVO.Detail detailReq : reqVO.getDetails()) {
            HcFgShippingNoticeItemDO noticeItem = noticeItemMap.get(detailReq.getShippingNoticeItemId());
            if (detailReq.getShippingPickItemId() == null) {
                rowNo++;
                continue;
            }
            HcFgShippingNoticePickItemDO pickItem = pickItemMap.get(detailReq.getShippingPickItemId());
            QmsFqcShippingDetailDO detail = candidateDetailMap.get(detailReq.getShippingPickItemId());
            validateSingleAlignmentRow(rowNo, detail, noticeItem, pickItem, usedPickItemIds, usedActualSliceBatchNoSet);
            rowNo++;
        }
        clearShippingAlignmentSnapshot(allCompletedDetails, noticeItemMap.keySet(), pickItemMap.values(), affectedFqcIds);
        for (QmsFgShippingAlignmentSaveReqVO.Detail detailReq : reqVO.getDetails()) {
            if (detailReq.getShippingPickItemId() == null) {
                continue;
            }
            HcFgShippingNoticeItemDO noticeItem = noticeItemMap.get(detailReq.getShippingNoticeItemId());
            HcFgShippingNoticePickItemDO pickItem = pickItemMap.get(detailReq.getShippingPickItemId());
            QmsFqcShippingDetailDO detail = candidateDetailMap.get(detailReq.getShippingPickItemId());
            QmsFqcOrderDO detailOrder = completedOrderMap.get(detail.getFqcId());
            writeBackShippingAlignment(detailOrder, notice, detail, noticeItem, pickItem);
            affectedFqcIds.add(detail.getFqcId());
        }
        affectedFqcIds.forEach(this::refreshOrderShippingStats);
        return getAlignment(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingFqcRespVO audit(QmsFqcAuditReqVO auditReqVO) {
        QmsFqcOrderDO order = validateFgShippingFqc(auditReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus())) {
            throw invalidParamException("发货成品检验单提交检测结果后才允许审核");
        }
        String auditResult = validateAuditResult(auditReqVO);
        if (AUDIT_REJECT.equals(auditResult)) {
            return returnForRework(order, requireRejectReason(auditReqVO.getRejectReason()));
        }
        refreshShippingDetailsFromItems(order.getId());
        refreshOrderShippingStats(order.getId());
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(order.getId());
        List<QmsFqcShippingDetailDO> details = qmsFqcShippingDetailMapper.selectListByFqcId(order.getId());
        validateInspectionItemsCompleted(items, details);
        validateShippingDetailsCompleted(details);
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
        writeBackSampleShippingInspection(order, details, now);
        if (hasNgDetail) {
            List<Long> ngPickItemIds = details.stream()
                    .filter(detail -> JUDGMENT_NG.equals(detail.getRowJudgment()))
                    .map(QmsFqcShippingDetailDO::getShippingPickItemId)
                    .filter(Objects::nonNull)
                    .toList();
            hcFinishedPackagingService.autoReturnShippingFqcNgPicks(order.getSourceReportId(), ngPickItemIds, order.getFqcNo());
        }
        refreshOrderShippingStats(order.getId());
        refreshShippingNoticeStatusAfterInspection(order.getSourceReportId());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFgShippingFqcScanRespVO resolveScan(QmsFqcScanReqVO scanReqVO) {
        String scanCode = StrUtil.trimToEmpty(scanReqVO.getScanCode());
        ScanMatch scanMatch = resolveScanMatch(scanReqVO, scanCode);
        QmsFgShippingFqcScanRespVO respVO = new QmsFgShippingFqcScanRespVO();
        respVO.setScanCode(scanCode);
        respVO.setScanTargetType(scanMatch.targetType);
        respVO.setOpenTarget(OPEN_TARGET_WORKBENCH);
        if (scanMatch.order == null) {
            respVO.setMatchResult(SCAN_RESULT_NOT_FOUND);
            respVO.setMessage("未找到发货成品检验明细");
            insertScanRecord(scanReqVO, null, null, scanMatch.targetType,
                    SCAN_RESULT_NOT_FOUND, "未找到发货成品检验明细");
            return respVO;
        }
        respVO.setMatchResult(SCAN_RESULT_MATCHED_SINGLE);
        respVO.setMessage(scanMatch.message);
        respVO.setRecord(get(scanMatch.order.getId()));
        if (scanMatch.detail != null) {
            respVO.setDetail(BeanUtils.toBean(scanMatch.detail, QmsFgShippingFqcRespVO.ShippingDetail.class));
        }
        insertScanRecord(scanReqVO, scanMatch.order, scanMatch.detail, scanMatch.targetType, SCAN_RESULT_MATCHED_SINGLE, null);
        return respVO;
    }

    private QmsFgShippingFqcPendingRespVO buildPendingResp(HcFgShippingNoticeDO notice) {
        List<HcFgShippingNoticePickItemDO> pickItems = selectActivePickItems(notice.getId());
        if (pickItems.isEmpty()) {
            return null;
        }
        QmsFqcOrderDO existing = qmsFqcOrderMapper.selectLatestBySource(SOURCE_MODULE_FG_SHIPPING_FQC, notice.getId());
        QmsFgShippingFqcPendingRespVO respVO = new QmsFgShippingFqcPendingRespVO();
        respVO.setShippingNoticeId(notice.getId());
        respVO.setShippingNoticeNo(notice.getNoticeNo());
        respVO.setCustomerName(notice.getCustomerName());
        respVO.setErpOrderNo(notice.getErpOrderNo());
        respVO.setMaterialCode(notice.getMaterialCode());
        respVO.setMaterialName(notice.getMaterialName());
        respVO.setModelCode(notice.getModelCode());
        respVO.setProductSize(notice.getProductSize());
        respVO.setNoticeStatus(notice.getNoticeStatus());
        respVO.setShippingTime(notice.getShippingTime());
        respVO.setPickedQty(sumQty(pickItems));
        respVO.setInspectedQty((int) pickItems.stream()
                .filter(item -> StrUtil.isNotBlank(item.getShippingInspectionResult()))
                .count());
        if (existing != null) {
            respVO.setExistingFqcId(existing.getId());
            respVO.setExistingFqcNo(existing.getFqcNo());
            respVO.setExistingFqcStatus(existing.getStatus());
            respVO.setExistingFqcJudgment(existing.getJudgment());
        }
        return respVO;
    }

    private HcFgShippingNoticeDO validateShippingNoticeForCreate(Long shippingNoticeId) {
        HcFgShippingNoticeDO notice = shippingNoticeId == null ? null : hcFgShippingNoticeMapper.selectByIdForUpdate(shippingNoticeId);
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货通知单不存在");
        }
        if (!CREATE_ALLOWED_NOTICE_STATUSES.contains(StrUtil.blankToDefault(notice.getNoticeStatus(), ""))) {
            throw invalidParamException("只有已出货确认或已检验的发货通知单允许生成发货成品检验");
        }
        return notice;
    }

    private QmsFqcSaveReqVO buildCreateReq(HcFgShippingNoticeDO notice, List<HcFgShippingNoticePickItemDO> pickItems) {
        HcFgShippingNoticePickItemDO firstPick = pickItems.get(0);
        QmsFqcSaveReqVO reqVO = new QmsFqcSaveReqVO();
        reqVO.setReportNo(notice.getNoticeNo());
        reqVO.setWorkOrderNo(firstNotBlank(notice.getErpOrderNo(), notice.getOrderNo(), notice.getNoticeNo()));
        reqVO.setSourceReportId(notice.getId());
        reqVO.setSourceReportNo(notice.getNoticeNo());
        reqVO.setSourceModule(SOURCE_MODULE_FG_SHIPPING_FQC);
        reqVO.setMaterialCode(firstNotBlank(notice.getMaterialCode(), firstPick.getMaterialCode()));
        reqVO.setMaterialName(firstNotBlank(notice.getMaterialName(), firstPick.getMaterialName()));
        reqVO.setSpecification(firstNotBlank(notice.getProductSize(), firstPick.getProductSize()));
        reqVO.setProductModel(firstNotBlank(notice.getModelCode(), firstPick.getModelCode()));
        reqVO.setProductBatchNo(firstNotBlank(firstPick.getCustomerProductBatchNo(), notice.getRequiredBatchNo(), firstPick.getBatchNo()));
        reqVO.setBatchNo(firstNotBlank(notice.getNoticeNo(), firstPick.getCustomerProductBatchNo(), firstPick.getActualSliceBatchNo()));
        reqVO.setProduceQty(BigDecimal.valueOf(sumQty(pickItems)));
        reqVO.setUnitCode("PCS");
        reqVO.setUnitName("片");
        reqVO.setSampleQty(1);
        reqVO.setInspectionCategory("FINAL");
        reqVO.setSubmissionType("MASS_SHIPMENT");
        reqVO.setSubmissionTime(LocalDateTime.now());
        reqVO.setSubmitterName(resolveLoginUserName());
        reqVO.setStatus(STATUS_PENDING);
        reqVO.setJudgment(JUDGMENT_PENDING);
        reqVO.setRemark("来源发货通知单：" + notice.getNoticeNo() + "；送检片数：" + pickItems.size());
        return reqVO;
    }

    private void upsertShippingDetails(QmsFqcOrderDO order, HcFgShippingNoticeDO notice,
                                       List<HcFgShippingNoticePickItemDO> pickItems) {
        List<QmsFqcShippingDetailDO> existingDetails = qmsFqcShippingDetailMapper.selectListByFqcId(order.getId());
        Map<Long, QmsFqcShippingDetailDO> detailByPickItemId = existingDetails.stream()
                .filter(detail -> detail.getShippingPickItemId() != null)
                .collect(Collectors.toMap(QmsFqcShippingDetailDO::getShippingPickItemId, detail -> detail,
                        (left, right) -> left, LinkedHashMap::new));
        for (HcFgShippingNoticePickItemDO pickItem : pickItems) {
            if (pickItem.getId() == null) {
                continue;
            }
            QmsFqcShippingDetailDO detail = detailByPickItemId.get(pickItem.getId());
            if (detail == null) {
                detail = qmsFqcShippingDetailMapper.selectByFqcIdAndPickItemId(order.getId(), pickItem.getId());
            }
            if (detail == null) {
                qmsFqcShippingDetailMapper.insert(buildShippingDetail(order, notice, pickItem));
            } else {
                refreshShippingDetailFromSource(order, notice, detail, pickItem);
            }
        }
    }

    private void removeObsoleteShippingDetails(Long fqcId, Set<Long> activePickItemIds) {
        if (fqcId == null || activePickItemIds == null) {
            return;
        }
        List<QmsFqcShippingDetailDO> obsoleteDetails = qmsFqcShippingDetailMapper.selectListByFqcId(fqcId).stream()
                .filter(detail -> detail.getShippingPickItemId() != null)
                .filter(detail -> !activePickItemIds.contains(detail.getShippingPickItemId()))
                .toList();
        if (obsoleteDetails.isEmpty()) {
            return;
        }
        List<Long> obsoleteDetailIds = obsoleteDetails.stream()
                .map(QmsFqcShippingDetailDO::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> obsoleteItemIds = qmsFqcItemMapper.selectListByFqcIdAndSubmissionDetailIds(fqcId, obsoleteDetailIds).stream()
                .map(QmsFqcItemDO::getId)
                .filter(Objects::nonNull)
                .toList();
        qmsFqcSampleDefectMapper.deleteByFqcItemIds(obsoleteItemIds);
        qmsFqcSampleMapper.deleteByFqcItemIds(obsoleteItemIds);
        obsoleteItemIds.forEach(qmsFqcItemMapper::deleteById);
        obsoleteDetailIds.forEach(qmsFqcShippingDetailMapper::deleteById);
    }

    private void syncShippingDetailJudgmentToPickItems(Long fqcId, Collection<Long> shippingDetailIds) {
        List<QmsFqcShippingDetailDO> details = shippingDetailIds == null
                ? qmsFqcShippingDetailMapper.selectListByFqcId(fqcId)
                : qmsFqcShippingDetailMapper.selectListByIds(shippingDetailIds);
        for (QmsFqcShippingDetailDO detail : details) {
            if (detail.getShippingPickItemId() == null) {
                continue;
            }
            String rowJudgment = StrUtil.blankToDefault(detail.getRowJudgment(), JUDGMENT_PENDING);
            boolean judged = JUDGMENT_OK.equals(rowJudgment) || JUDGMENT_NG.equals(rowJudgment);
            LambdaUpdateWrapper<HcFgShippingNoticePickItemDO> wrapper = new LambdaUpdateWrapper<HcFgShippingNoticePickItemDO>()
                    .eq(HcFgShippingNoticePickItemDO::getId, detail.getShippingPickItemId())
                    .set(HcFgShippingNoticePickItemDO::getShippingQualityNo, detail.getFqcNo())
                    .set(HcFgShippingNoticePickItemDO::getShippingInspectionResult, judged ? rowJudgment : null)
                    .set(HcFgShippingNoticePickItemDO::getShippingInspectionRemark,
                            judged ? firstNotBlank(detail.getNgReason(), detail.getRemark()) : null)
                    .set(HcFgShippingNoticePickItemDO::getShippingInspectorName,
                            judged ? detail.getInspectorName() : null)
                    .set(HcFgShippingNoticePickItemDO::getShippingInspectionTime,
                            judged ? detail.getInspectionTime() : null);
            if (judged) {
                wrapper.set(HcFgShippingNoticePickItemDO::getLockStatus, NOTICE_STATUS_INSPECTED);
            }
            hcFgShippingNoticePickItemMapper.update(null, wrapper);
        }
    }

    private HcFgShippingNoticeItemDO findNoticeItem(List<HcFgShippingNoticeItemDO> noticeItems, Long noticeItemId) {
        if (noticeItemId == null || noticeItems == null) {
            return null;
        }
        return noticeItems.stream()
                .filter(item -> Objects.equals(item.getId(), noticeItemId))
                .findFirst()
                .orElse(null);
    }

    private HcFgShippingNoticePickItemDO selectUnusedPickItem(List<HcFgShippingNoticePickItemDO> pickItems,
                                                              Set<Long> usedPickItemIds) {
        if (pickItems == null || pickItems.isEmpty()) {
            return null;
        }
        return pickItems.stream()
                .filter(item -> item.getId() != null && !usedPickItemIds.contains(item.getId()))
                .findFirst()
                .orElse(null);
    }

    private QmsFqcShippingDetailDO buildShippingDetail(QmsFqcOrderDO order, HcFgShippingNoticeDO notice,
                                                       HcFgShippingNoticePickItemDO pickItem) {
        QmsFqcShippingDetailDO detailDO = QmsFqcShippingDetailDO.builder()
                .tenantId(resolveTenantId(order, null, pickItem))
                .fqcId(order.getId())
                .fqcNo(order.getFqcNo())
                .shippingNoticeId(notice.getId())
                .shippingNoticeNo(notice.getNoticeNo())
                .customerId(notice.getCustomerId())
                .customerCode(notice.getCustomerCode())
                .customerName(notice.getCustomerName())
                .erpOrderNo(notice.getErpOrderNo())
                .customerProductBatchNo(pickItem.getCustomerProductBatchNo())
                .materialCode(firstNotBlank(pickItem.getMaterialCode(), notice.getMaterialCode()))
                .materialName(firstNotBlank(pickItem.getMaterialName(), notice.getMaterialName()))
                .modelCode(firstNotBlank(pickItem.getModelCode(), pickItem.getInternalModelCode(), notice.getModelCode()))
                .internalItemCode(pickItem.getInternalItemCode())
                .productSize(firstNotBlank(pickItem.getProductSize(), notice.getProductSize()))
                .shippingQty(firstPositive(pickItem.getActualShipQty(), pickItem.getLockedQty(), 1))
                .rowJudgment(JUDGMENT_PENDING)
                .alignmentStatus("SAMPLE".equals(notice.getProductType()) ? "NOT_REQUIRED" : ALIGNMENT_PENDING)
                .build();
        applyPickItemSnapshot(detailDO, pickItem);
        return detailDO;
    }

    private Long resolveTenantId(QmsFqcOrderDO order, HcFgShippingNoticeItemDO noticeItem, HcFgShippingNoticePickItemDO pickItem) {
        if (pickItem != null && pickItem.getTenantId() != null) {
            return pickItem.getTenantId();
        }
        if (noticeItem != null && noticeItem.getTenantId() != null) {
            return noticeItem.getTenantId();
        }
        return order.getTenantId();
    }

    private void refreshShippingDetailFromSource(QmsFqcOrderDO order, HcFgShippingNoticeDO notice,
                                                 QmsFqcShippingDetailDO detail,
                                                 HcFgShippingNoticePickItemDO pickItem) {
        QmsFqcShippingDetailDO update = new QmsFqcShippingDetailDO();
        update.setId(detail.getId());
        update.setTenantId(resolveTenantId(order, null, pickItem));
        update.setFqcNo(order.getFqcNo());
        update.setShippingNoticeId(notice.getId());
        update.setShippingNoticeNo(notice.getNoticeNo());
        if (detail.getShippingNoticeItemId() == null) {
            update.setCustomerProductBatchNo(pickItem.getCustomerProductBatchNo());
            update.setPackageSliceNo(null);
            update.setMaterialCode(firstNotBlank(pickItem.getMaterialCode(), notice.getMaterialCode()));
            update.setMaterialName(firstNotBlank(pickItem.getMaterialName(), notice.getMaterialName()));
            update.setModelCode(firstNotBlank(pickItem.getModelCode(), pickItem.getInternalModelCode(), notice.getModelCode()));
            update.setInternalItemCode(pickItem.getInternalItemCode());
            update.setProductSize(firstNotBlank(pickItem.getProductSize(), notice.getProductSize()));
            update.setShippingQty(firstPositive(pickItem.getActualShipQty(), pickItem.getLockedQty(), 1));
        }
        applyPickItemSnapshot(update, pickItem);
        qmsFqcShippingDetailMapper.updateById(update);
        updatePieceItemsForShippingDetail(detail.getId(), update.getActualSliceBatchNo(), update.getSliceBatchNo());
    }

    private void applyPickItemSnapshot(QmsFqcShippingDetailDO detail,
                                       HcFgShippingNoticePickItemDO pickItem) {
        if (pickItem == null) {
            detail.setShippingPickItemId(null);
            detail.setFinishedStockId(null);
            detail.setStockNo(null);
            detail.setActualSliceBatchNo(null);
            detail.setSliceBatchNo(null);
            return;
        }
        detail.setShippingPickItemId(pickItem.getId());
        detail.setFinishedStockId(pickItem.getFinishedStockId());
        detail.setStockNo(pickItem.getStockNo());
        detail.setActualSliceBatchNo(pickItem.getActualSliceBatchNo());
        detail.setSliceBatchNo(pickItem.getSliceBatchNo());
    }

    private void updatePieceItemsForShippingDetail(Long shippingDetailId, String actualSliceBatchNo, String packageSliceNo) {
        if (shippingDetailId == null) {
            return;
        }
        qmsFqcItemMapper.update(null, new LambdaUpdateWrapper<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getSubmissionDetailId, shippingDetailId)
                .set(QmsFqcItemDO::getProductionBatchNo, actualSliceBatchNo)
                .set(QmsFqcItemDO::getParentProductionBatchNo, packageSliceNo));
    }

    private void ensurePieceInspectionItems(Long fqcId) {
        List<QmsFqcShippingDetailDO> details = qmsFqcShippingDetailMapper.selectListByFqcId(fqcId);
        if (details.isEmpty()) {
            return;
        }
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(fqcId);
        List<QmsFqcItemDO> templateItems = items.stream()
                .filter(item -> item.getSubmissionDetailId() == null)
                .collect(Collectors.toList());
        Map<Long, List<QmsFqcItemDO>> itemMap = items.stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcItemDO::getSubmissionDetailId));
        boolean removeRootTemplateItems = !templateItems.isEmpty();
        if (templateItems.isEmpty()) {
            templateItems = itemMap.values().stream()
                    .filter(list -> list != null && !list.isEmpty())
                    .findFirst()
                    .orElse(Collections.emptyList());
        }
        if (templateItems.isEmpty()) {
            return;
        }
        int seqNo = 1;
        for (QmsFqcShippingDetailDO detail : details) {
            if (itemMap.getOrDefault(detail.getId(), Collections.emptyList()).isEmpty()) {
                for (QmsFqcItemDO templateItem : templateItems) {
                    qmsFqcItemMapper.insert(buildPieceItem(templateItem, detail, seqNo));
                }
            }
            seqNo++;
        }
        if (removeRootTemplateItems) {
            List<Long> templateItemIds = templateItems.stream()
                    .map(QmsFqcItemDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            qmsFqcSampleDefectMapper.deleteByFqcItemIds(templateItemIds);
            qmsFqcSampleMapper.deleteByFqcItemIds(templateItemIds);
            templateItemIds.forEach(qmsFqcItemMapper::deleteById);
        }
    }

    private QmsFqcItemDO buildPieceItem(QmsFqcItemDO templateItem, QmsFqcShippingDetailDO detail, int seqNo) {
        QmsFqcItemDO item = BeanUtils.toBean(templateItem, QmsFqcItemDO.class);
        item.setId(null);
        item.setSubmissionDetailId(detail.getId());
        item.setCutRoundInspectionDetailId(null);
        item.setSliceSeqNo(seqNo);
        item.setProductionBatchNo(detail.getActualSliceBatchNo());
        item.setParentProductionBatchNo(detail.getSliceBatchNo());
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

    private List<Long> resolveFilterFqcIds(QmsFgShippingFqcPageReqVO pageReqVO) {
        List<Long> fqcIds = null;
        if (StrUtil.isNotBlank(pageReqVO.getActualSliceBatchNo())) {
            fqcIds = intersectFqcIds(fqcIds, qmsFqcShippingDetailMapper.selectFqcIdsByActualSliceBatchNo(pageReqVO.getActualSliceBatchNo().trim()));
        }
        if (StrUtil.isNotBlank(pageReqVO.getAlignmentStatus())) {
            fqcIds = intersectFqcIds(fqcIds, qmsFqcShippingDetailMapper.selectFqcIdsByAlignmentStatus(pageReqVO.getAlignmentStatus().trim()));
        }
        if (StrUtil.isNotBlank(pageReqVO.getCustomerName())) {
            fqcIds = intersectFqcIds(fqcIds, qmsFqcShippingDetailMapper.selectFqcIdsByCustomerName(pageReqVO.getCustomerName().trim()));
        }
        if (StrUtil.isNotBlank(pageReqVO.getErpOrderNo())) {
            fqcIds = intersectFqcIds(fqcIds, qmsFqcShippingDetailMapper.selectFqcIdsByErpOrderNo(pageReqVO.getErpOrderNo().trim()));
        }
        return fqcIds;
    }

    private List<Long> intersectFqcIds(List<Long> baseIds, List<Long> nextIds) {
        if (baseIds == null) {
            return nextIds;
        }
        if (nextIds == null || nextIds.isEmpty()) {
            return Collections.emptyList();
        }
        return baseIds.stream().filter(nextIds::contains).toList();
    }

    private void fillPageShippingSummary(List<QmsFgShippingFqcRespVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> fqcIds = records.stream().map(QmsFgShippingFqcRespVO::getId).filter(Objects::nonNull).toList();
        Map<Long, List<QmsFqcShippingDetailDO>> detailMap = qmsFqcShippingDetailMapper.selectListByFqcIds(fqcIds)
                .stream()
                .collect(Collectors.groupingBy(QmsFqcShippingDetailDO::getFqcId));
        Map<Long, HcFgShippingNoticeDO> notices = hcFgShippingNoticeMapper.selectByIds(records.stream()
                .map(QmsFgShippingFqcRespVO::getSourceReportId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(HcFgShippingNoticeDO::getId, item -> item));
        for (QmsFgShippingFqcRespVO record : records) {
            HcFgShippingNoticeDO notice = notices.get(record.getSourceReportId());
            record.setProductType(notice == null ? null : notice.getProductType());
            List<QmsFgShippingFqcRespVO.ShippingDetail> details = BeanUtils.toBean(
                    detailMap.getOrDefault(record.getId(), Collections.emptyList()),
                    QmsFgShippingFqcRespVO.ShippingDetail.class);
            fillRecordShippingSummary(record, details, buildAlignmentRows(record.getSourceReportId(), details));
        }
    }

    private void fillRecordShippingSummary(QmsFgShippingFqcRespVO record,
                                           List<QmsFgShippingFqcRespVO.ShippingDetail> details,
                                           List<QmsFgShippingFqcRespVO.AlignmentRow> alignmentRows) {
        if (details == null || details.isEmpty()) {
            return;
        }
        QmsFgShippingFqcRespVO.ShippingDetail first = details.get(0);
        record.setShippingNoticeNo(first.getShippingNoticeNo());
        record.setCustomerName(first.getCustomerName());
        record.setErpOrderNo(first.getErpOrderNo());
        List<QmsFgShippingFqcRespVO.AlignmentRow> rows = alignmentRows == null ? Collections.emptyList() : alignmentRows;
        boolean hasMismatch = rows.stream().anyMatch(row -> ALIGNMENT_MISMATCH.equals(row.getAlignmentStatus()));
        boolean hasPending = rows.isEmpty() || rows.stream().anyMatch(row -> !ALIGNMENT_ALIGNED.equals(row.getAlignmentStatus()));
        record.setAlignmentStatus("SAMPLE".equals(record.getProductType()) ? "NOT_REQUIRED"
                : hasMismatch ? ALIGNMENT_MISMATCH : hasPending ? ALIGNMENT_PENDING : ALIGNMENT_ALIGNED);
        record.setMismatchReason(rows.stream()
                .map(QmsFgShippingFqcRespVO.AlignmentRow::getMismatchReason)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；")));
    }

    private List<QmsFgShippingFqcRespVO.AlignmentRow> buildAlignmentRows(Long shippingNoticeId,
                                                                         List<QmsFgShippingFqcRespVO.ShippingDetail> details) {
        if (shippingNoticeId == null) {
            return Collections.emptyList();
        }
        Map<Long, QmsFgShippingFqcRespVO.ShippingDetail> alignedDetailMap =
                (details == null ? Collections.<QmsFgShippingFqcRespVO.ShippingDetail>emptyList() : details)
                        .stream()
                        .filter(detail -> detail.getShippingNoticeItemId() != null)
                        .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                        .collect(Collectors.toMap(QmsFgShippingFqcRespVO.ShippingDetail::getShippingNoticeItemId,
                                detail -> detail, (left, right) -> left, LinkedHashMap::new));
        return hcFgShippingNoticeItemMapper.selectListByNoticeId(shippingNoticeId).stream()
                .map(item -> buildAlignmentRow(item, alignedDetailMap.get(item.getId())))
                .toList();
    }

    private QmsFgShippingFqcRespVO.AlignmentRow buildAlignmentRow(HcFgShippingNoticeItemDO item,
                                                                  QmsFgShippingFqcRespVO.ShippingDetail detail) {
        QmsFgShippingFqcRespVO.AlignmentRow row = new QmsFgShippingFqcRespVO.AlignmentRow();
        row.setId(item.getId());
        row.setShippingNoticeItemId(item.getId());
        row.setCustomerProductBatchNo(item.getCustomerProductBatchNo());
        row.setPackageSliceNo(item.getPackageSliceNo());
        row.setMaterialCode(item.getMaterialCode());
        row.setMaterialName(item.getMaterialName());
        row.setModelCode(firstNotBlank(item.getInternalModelCode(), item.getCustomerModelCode(), item.getModelCode()));
        row.setInternalItemCode(firstNotBlank(item.getInternalItemCode(), item.getCustomerSliceBatchNo()));
        row.setProductSize(item.getProductSize());
        row.setShippingQty(firstPositive(item.getActualShipQty(), item.getLockedQty(), item.getStockQty(), 1));
        row.setSliceBatchNo(firstNotBlank(item.getPackageSliceNo(), item.getSliceBatchNo()));
        if (detail == null) {
            row.setAlignmentStatus(ALIGNMENT_PENDING);
            row.setMismatchReason(StrUtil.blankToDefault(item.getMismatchReason(), "计划包片未绑定OK实际片号"));
            return row;
        }
        row.setShippingDetailId(detail.getId());
        row.setFqcId(detail.getFqcId());
        row.setFqcNo(detail.getFqcNo());
        row.setShippingPickItemId(detail.getShippingPickItemId());
        row.setFinishedStockId(detail.getFinishedStockId());
        row.setStockNo(detail.getStockNo());
        row.setActualSliceBatchNo(detail.getActualSliceBatchNo());
        row.setRowJudgment(detail.getRowJudgment());
        row.setAlignmentStatus(StrUtil.blankToDefault(detail.getAlignmentStatus(), ALIGNMENT_ALIGNED));
        row.setMismatchReason(detail.getMismatchReason());
        return row;
    }

    private QmsFgShippingFqcRespVO.AlignmentRow buildAlignmentRow(HcFgShippingNoticeItemDO item,
                                                                  QmsFqcShippingDetailDO detail) {
        return buildAlignmentRow(item, detail == null ? null
                : BeanUtils.toBean(detail, QmsFgShippingFqcRespVO.ShippingDetail.class));
    }

    private List<QmsFgShippingFqcRespVO.AlignmentCandidate> buildAlignmentCandidates(Long fqcId) {
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(fqcId);
        if (order == null || order.getSourceReportId() == null) {
            return Collections.emptyList();
        }
        return buildAlignmentCandidates(order.getSourceReportId(), qmsFqcShippingDetailMapper.selectListByFqcId(fqcId));
    }

    private List<QmsFgShippingFqcRespVO.AlignmentCandidate> buildAlignmentCandidates(Long shippingNoticeId,
                                                                                       List<QmsFqcShippingDetailDO> details) {
        Map<Long, HcFgShippingNoticePickItemDO> pickItemMap = selectActivePickItems(shippingNoticeId).stream()
                .collect(Collectors.toMap(HcFgShippingNoticePickItemDO::getId, item -> item, (left, right) -> left));
        return (details == null ? Collections.<QmsFqcShippingDetailDO>emptyList() : details).stream()
                .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                .filter(detail -> detail.getShippingPickItemId() != null)
                .filter(detail -> pickItemMap.containsKey(detail.getShippingPickItemId()))
                .filter(detail -> StrUtil.isNotBlank(detail.getActualSliceBatchNo()))
                .map(detail -> buildAlignmentCandidate(detail, pickItemMap.get(detail.getShippingPickItemId())))
                .toList();
    }

    private QmsFgShippingFqcRespVO.AlignmentCandidate buildAlignmentCandidate(QmsFqcShippingDetailDO detail,
                                                                                HcFgShippingNoticePickItemDO pickItem) {
        QmsFgShippingFqcRespVO.AlignmentCandidate candidate = new QmsFgShippingFqcRespVO.AlignmentCandidate();
        candidate.setShippingDetailId(detail.getId());
        candidate.setFqcId(detail.getFqcId());
        candidate.setFqcNo(detail.getFqcNo());
        candidate.setShippingPickItemId(detail.getShippingPickItemId());
        candidate.setSourceNoticeItemId(pickItem == null ? null : pickItem.getSourceNoticeItemId());
        candidate.setFinishedStockId(detail.getFinishedStockId());
        candidate.setStockNo(detail.getStockNo());
        candidate.setActualSliceBatchNo(detail.getActualSliceBatchNo());
        candidate.setSliceBatchNo(detail.getSliceBatchNo());
        candidate.setInternalModelCode(detail.getModelCode());
        candidate.setInternalItemCode(detail.getInternalItemCode());
        candidate.setCustomerProductBatchNo(detail.getCustomerProductBatchNo());
        candidate.setMaterialCode(detail.getMaterialCode());
        candidate.setMaterialName(detail.getMaterialName());
        candidate.setModelCode(detail.getModelCode());
        candidate.setProductSize(detail.getProductSize());
        candidate.setQualityStatus(detail.getRowJudgment());
        candidate.setRowJudgment(detail.getRowJudgment());
        candidate.setAlignmentStatus(detail.getAlignmentStatus());
        return candidate;
    }

    private void fillShippingDetailItems(List<QmsFgShippingFqcRespVO.ShippingDetail> details,
                                         List<QmsFqcRespVO.FqcItem> items) {
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcRespVO.FqcItem>> itemMap = (items == null ? Collections.<QmsFqcRespVO.FqcItem>emptyList() : items)
                .stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcRespVO.FqcItem::getSubmissionDetailId, LinkedHashMap::new, Collectors.toList()));
        for (QmsFgShippingFqcRespVO.ShippingDetail detail : details) {
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

    private void fillShippingDetailStats(List<QmsFgShippingFqcRespVO.ShippingDetail> details,
                                         List<QmsFqcItemDO> items) {
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<Long, List<QmsFqcItemDO>> itemMap = (items == null ? Collections.<QmsFqcItemDO>emptyList() : items)
                .stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcItemDO::getSubmissionDetailId, LinkedHashMap::new, Collectors.toList()));
        for (QmsFgShippingFqcRespVO.ShippingDetail detail : details) {
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

    private void refreshShippingDetailsFromItems(Long fqcId) {
        refreshShippingDetailsFromItems(fqcId, null);
    }

    private void refreshShippingDetailsFromItems(Long fqcId, Collection<Long> shippingDetailIds) {
        List<QmsFqcShippingDetailDO> details = shippingDetailIds == null
                ? qmsFqcShippingDetailMapper.selectListByFqcId(fqcId)
                : qmsFqcShippingDetailMapper.selectListByIds(shippingDetailIds);
        if (details.isEmpty()) {
            return;
        }
        List<QmsFqcItemDO> items = shippingDetailIds == null
                ? qmsFqcItemMapper.selectListByFqcId(fqcId)
                : qmsFqcItemMapper.selectListByFqcIdAndSubmissionDetailIds(fqcId, shippingDetailIds);
        List<Long> itemIds = items.stream().map(QmsFqcItemDO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        List<QmsFqcSampleDO> samples = qmsFqcSampleMapper.selectListByFqcIdAndItemIds(fqcId, itemIds);
        Map<Long, List<QmsFqcItemDO>> itemMap = items.stream()
                .filter(item -> item.getSubmissionDetailId() != null)
                .collect(Collectors.groupingBy(QmsFqcItemDO::getSubmissionDetailId));
        Map<Long, List<QmsFqcSampleDO>> sampleMap = samples.stream()
                .filter(sample -> sample.getFqcItemId() != null)
                .collect(Collectors.groupingBy(QmsFqcSampleDO::getFqcItemId));
        LocalDateTime now = LocalDateTime.now();
        for (QmsFqcShippingDetailDO detail : details) {
            List<QmsFqcItemDO> pieceItems = itemMap.getOrDefault(detail.getId(), Collections.emptyList());
            String itemJudgment = resolvePieceJudgment(pieceItems);
            boolean keepManualNg = JUDGMENT_NG.equals(detail.getRowJudgment()) && JUDGMENT_PENDING.equals(itemJudgment);
            String rowJudgment = keepManualNg ? JUDGMENT_NG : itemJudgment;
            String itemNgReason = JUDGMENT_NG.equals(itemJudgment) ? buildPieceNgReason(pieceItems, sampleMap) : null;
            String ngReason = JUDGMENT_NG.equals(rowJudgment)
                    ? firstNotBlank(itemNgReason, detail.getNgReason(), detail.getRemark(), "发货成品检验片级判定NG")
                    : null;
            QmsFqcSampleDO firstNgSample = JUDGMENT_NG.equals(itemJudgment)
                    ? findFirstNgSample(pieceItems, sampleMap) : null;
            Long inspectorId = JUDGMENT_PENDING.equals(rowJudgment) ? null
                    : detail.getInspectorId() == null ? SecurityFrameworkUtils.getLoginUserId() : detail.getInspectorId();
            String inspectorName = JUDGMENT_PENDING.equals(rowJudgment) ? null
                    : firstNotBlank(detail.getInspectorName(), resolveLoginUserName());
            LocalDateTime inspectionTime = JUDGMENT_PENDING.equals(rowJudgment) ? null
                    : detail.getInspectionTime() == null ? now : detail.getInspectionTime();
            qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                    .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                    .set(QmsFqcShippingDetailDO::getRowJudgment, rowJudgment)
                    .set(QmsFqcShippingDetailDO::getDefectCode, JUDGMENT_NG.equals(rowJudgment)
                            ? firstNotBlank(firstNgSample == null ? null : firstNgSample.getDefectCode(), detail.getDefectCode())
                            : null)
                    .set(QmsFqcShippingDetailDO::getDefectName, JUDGMENT_NG.equals(rowJudgment)
                            ? firstNotBlank(firstNgSample == null ? null : firstNgSample.getDefectName(), detail.getDefectName())
                            : null)
                    .set(QmsFqcShippingDetailDO::getNgReason, ngReason)
                    .set(QmsFqcShippingDetailDO::getInspectorId, inspectorId)
                    .set(QmsFqcShippingDetailDO::getInspectorName, inspectorName)
                    .set(QmsFqcShippingDetailDO::getInspectionTime, inspectionTime));
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
            reasons.add(item.getInspectionItem() + (sampleReasons.isEmpty() ? "" : "：" + String.join("；", sampleReasons)));
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

    private void refreshOrderShippingStats(Long fqcId) {
        Long totalCount = qmsFqcShippingDetailMapper.selectCount(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false));
        Long okCount = qmsFqcShippingDetailMapper.selectCount(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .eq(QmsFqcShippingDetailDO::getRowJudgment, JUDGMENT_OK));
        Long ngCount = qmsFqcShippingDetailMapper.selectCount(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .eq(QmsFqcShippingDetailDO::getRowJudgment, JUDGMENT_NG));
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

    private AlignmentResult alignAndWriteBackShipping(QmsFqcOrderDO order, List<QmsFqcShippingDetailDO> details,
                                                      LocalDateTime now) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectById(order.getSourceReportId());
        List<HcFgShippingNoticeItemDO> noticeItems = notice == null ? Collections.emptyList()
                : hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId());
        Map<Long, HcFgShippingNoticeItemDO> noticeItemMap = noticeItems.stream()
                .collect(Collectors.toMap(HcFgShippingNoticeItemDO::getId, item -> item, (left, right) -> left));
        Map<Long, HcFgShippingNoticePickItemDO> pickItemMap = notice == null ? Collections.emptyMap()
                : selectActivePickItems(notice.getId()).stream()
                .collect(Collectors.toMap(HcFgShippingNoticePickItemDO::getId, item -> item, (left, right) -> left));
        boolean hasMismatch = false;
        for (QmsFqcShippingDetailDO detail : details) {
            HcFgShippingNoticePickItemDO pickItem = pickItemMap.get(detail.getShippingPickItemId());
            HcFgShippingNoticeItemDO noticeItem = noticeItemMap.get(detail.getShippingNoticeItemId());
            if (JUDGMENT_NG.equals(detail.getRowJudgment())) {
                if (noticeItem != null && findOtherOkDetailForNoticeItem(details, noticeItem.getId(), detail.getId()) == null) {
                    clearNoticeItemAlignment(noticeItem.getId());
                }
                qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                        .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                        .set(QmsFqcShippingDetailDO::getShippingNoticeItemId, null)
                        .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_PENDING)
                        .set(QmsFqcShippingDetailDO::getMismatchReason, null));
                writeBackShippingInspection(order, detail, null, pickItem, false,
                        firstNotBlank(detail.getNgReason(), "实际片号FQC检验NG，不能对齐计划包片"), now);
                continue;
            }
            if (detail.getShippingNoticeItemId() == null) {
                qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                        .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                        .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_PENDING)
                        .set(QmsFqcShippingDetailDO::getMismatchReason, null));
                writeBackShippingInspection(order, detail, null, pickItem, true, null, now);
                continue;
            }
            List<String> mismatchReasons = buildMismatchReasons(notice, noticeItem, pickItem, detail);
            boolean aligned = mismatchReasons.isEmpty();
            if (!aligned) {
                hasMismatch = true;
            }
            String alignmentStatus = aligned ? ALIGNMENT_ALIGNED : ALIGNMENT_MISMATCH;
            String mismatchReason = aligned ? null : String.join("；", mismatchReasons);
            qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                    .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                    .set(QmsFqcShippingDetailDO::getAlignmentStatus, alignmentStatus)
                    .set(QmsFqcShippingDetailDO::getMismatchReason, mismatchReason));
            writeBackShippingInspection(order, detail, noticeItem, pickItem, aligned, mismatchReason, now);
        }
        List<QmsFqcShippingDetailDO> refreshedDetails = qmsFqcShippingDetailMapper.selectListByFqcId(order.getId());
        boolean hasPendingPlan = noticeItems.stream()
                .anyMatch(item -> findOkAlignedDetail(refreshedDetails, item.getId()) == null);
        return new AlignmentResult(hasMismatch, hasPendingPlan);
    }

    private QmsFqcShippingDetailDO findOtherOkDetailForNoticeItem(List<QmsFqcShippingDetailDO> details,
                                                                  Long noticeItemId,
                                                                  Long excludeDetailId) {
        if (noticeItemId == null || details == null) {
            return null;
        }
        return details.stream()
                .filter(detail -> !Objects.equals(detail.getId(), excludeDetailId))
                .filter(detail -> Objects.equals(detail.getShippingNoticeItemId(), noticeItemId))
                .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                .findFirst()
                .orElse(null);
    }

    private QmsFqcShippingDetailDO findOkAlignedDetail(List<QmsFqcShippingDetailDO> details, Long noticeItemId) {
        if (noticeItemId == null || details == null) {
            return null;
        }
        return details.stream()
                .filter(detail -> Objects.equals(detail.getShippingNoticeItemId(), noticeItemId))
                .filter(detail -> JUDGMENT_OK.equals(detail.getRowJudgment()))
                .filter(detail -> ALIGNMENT_ALIGNED.equals(detail.getAlignmentStatus()))
                .findFirst()
                .orElse(null);
    }

    private void clearNoticeItemAlignment(Long noticeItemId) {
        if (noticeItemId == null) {
            return;
        }
        hcFgShippingNoticeItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getId, noticeItemId)
                .set(HcFgShippingNoticeItemDO::getActualFinishedStockId, null)
                .set(HcFgShippingNoticeItemDO::getActualStockNo, null)
                .set(HcFgShippingNoticeItemDO::getOuterBoxNo, null)
                .set(HcFgShippingNoticeItemDO::getInnerUnitNo, null)
                .set(HcFgShippingNoticeItemDO::getPackageNo, null)
                .set(HcFgShippingNoticeItemDO::getActualSliceBatchNo, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectionResult, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectionRemark, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectionTime, null)
                .set(HcFgShippingNoticeItemDO::getMismatchReason, "计划包片未绑定OK实际片号"));
    }

    private List<String> buildMismatchReasons(HcFgShippingNoticeDO notice,
                                              HcFgShippingNoticeItemDO noticeItem,
                                              HcFgShippingNoticePickItemDO pickItem,
                                              QmsFqcShippingDetailDO detail) {
        List<String> reasons = new ArrayList<>();
        if (notice == null) {
            reasons.add("发货通知单不存在");
            return reasons;
        }
        if (noticeItem == null) {
            reasons.add("发货通知单明细不存在");
        }
        if (pickItem == null) {
            reasons.add("发货配货明细不存在");
        }
        appendMismatch(reasons, "发货通知单", detail.getShippingNoticeNo(), notice.getNoticeNo());
        appendMismatch(reasons, "客户", detail.getCustomerName(), notice.getCustomerName());
        appendMismatch(reasons, "ERP订单", detail.getErpOrderNo(), notice.getErpOrderNo());
        appendMismatch(reasons, "物料", detail.getMaterialCode(), firstNotBlank(noticeItem == null ? null : noticeItem.getMaterialCode(), pickItem == null ? null : pickItem.getMaterialCode(), notice.getMaterialCode()));
        appendMismatch(reasons, "产品型号", detail.getModelCode(), firstNotBlank(noticeItem == null ? null : noticeItem.getInternalModelCode(), pickItem == null ? null : pickItem.getModelCode(), notice.getModelCode()));
        String requiredPrefix = firstNotBlank(noticeItem == null ? null : noticeItem.getInternalItemCode(),
                noticeItem == null ? null : noticeItem.getCustomerSliceBatchNo(), detail.getInternalItemCode());
        String actualSliceBatchNo = firstNotBlank(detail.getActualSliceBatchNo(), pickItem == null ? null : pickItem.getActualSliceBatchNo());
        if (StrUtil.isNotBlank(requiredPrefix)
                && (StrUtil.isBlank(actualSliceBatchNo) || !actualSliceBatchNo.toLowerCase().startsWith(requiredPrefix.toLowerCase()))) {
            reasons.add("内部编号前缀不一致");
        }
        appendMismatch(reasons, "客户产品批号", detail.getCustomerProductBatchNo(), firstNotBlank(noticeItem == null ? null : noticeItem.getCustomerProductBatchNo(), pickItem == null ? null : pickItem.getCustomerProductBatchNo()));
        appendMismatch(reasons, "包装片号", detail.getPackageSliceNo(), noticeItem == null ? null : noticeItem.getPackageSliceNo());
        appendMismatch(reasons, "实际片号", detail.getActualSliceBatchNo(), pickItem == null ? null : pickItem.getActualSliceBatchNo());
        Integer actualQty = pickItem == null ? null : firstPositive(pickItem.getActualShipQty(), pickItem.getLockedQty(), 1);
        if (!Objects.equals(firstPositive(detail.getShippingQty(), 1), actualQty)) {
            reasons.add("发货数量不一致");
        }
        if (!JUDGMENT_OK.equals(detail.getRowJudgment()) && !JUDGMENT_NG.equals(detail.getRowJudgment())) {
            reasons.add("FQC片级结果未判定");
        }
        return reasons;
    }

    private void writeBackShippingInspection(QmsFqcOrderDO order, QmsFqcShippingDetailDO detail,
                                             HcFgShippingNoticeItemDO noticeItem,
                                             HcFgShippingNoticePickItemDO pickItem,
                                             boolean aligned, String mismatchReason, LocalDateTime now) {
        String result = aligned && JUDGMENT_OK.equals(detail.getRowJudgment()) ? JUDGMENT_OK : JUDGMENT_NG;
        String inspectorName = firstNotBlank(detail.getInspectorName(), order.getQaInspectorName(), resolveLoginUserName());
        LocalDateTime inspectionTime = firstValidTime(detail.getInspectionTime(), order.getQaTime(), now);
        String remark = aligned ? "发货成品检验对齐通过，片级结果OK"
                : firstNotBlank(mismatchReason, detail.getNgReason(), "发货成品检验对齐失败");
        if (pickItem != null) {
            HcFgShippingNoticePickItemDO updatePick = new HcFgShippingNoticePickItemDO();
            updatePick.setId(pickItem.getId());
            updatePick.setShippingQualityNo(order.getFqcNo());
            updatePick.setShippingInspectorName(inspectorName);
            updatePick.setShippingInspectionTime(inspectionTime);
            updatePick.setShippingInspectionResult(result);
            updatePick.setShippingInspectionRemark(remark);
            updatePick.setQualityStatus(result);
            updatePick.setLockStatus(NOTICE_STATUS_INSPECTED);
            hcFgShippingNoticePickItemMapper.updateById(updatePick);
        }
        if (noticeItem != null) {
            HcFgShippingNoticeItemDO updateNoticeItem = new HcFgShippingNoticeItemDO();
            updateNoticeItem.setId(noticeItem.getId());
            if (pickItem != null) {
                updateNoticeItem.setActualFinishedStockId(pickItem.getFinishedStockId());
                updateNoticeItem.setActualStockNo(pickItem.getStockNo());
                updateNoticeItem.setOuterBoxNo(pickItem.getOuterBoxNo());
                updateNoticeItem.setInnerUnitNo(pickItem.getInnerUnitNo());
                updateNoticeItem.setPackageNo(pickItem.getPackageNo());
                updateNoticeItem.setActualSliceBatchNo(pickItem.getActualSliceBatchNo());
                updateNoticeItem.setSliceBatchNo(firstNotBlank(noticeItem.getSliceBatchNo(), pickItem.getSliceBatchNo()));
                updateNoticeItem.setBatchNo(firstNotBlank(noticeItem.getCustomerProductBatchNo(), pickItem.getBatchNo()));
                updateNoticeItem.setMaterialCode(firstNotBlank(noticeItem.getMaterialCode(), pickItem.getMaterialCode()));
                updateNoticeItem.setMaterialName(firstNotBlank(noticeItem.getMaterialName(), pickItem.getMaterialName()));
                updateNoticeItem.setModelCode(firstNotBlank(noticeItem.getInternalModelCode(), pickItem.getModelCode()));
                updateNoticeItem.setStockQty(firstPositive(noticeItem.getStockQty(), pickItem.getStockQty(), 1));
                updateNoticeItem.setActualShipQty(firstPositive(pickItem.getActualShipQty(), 1));
                updateNoticeItem.setWarehouseCode(pickItem.getWarehouseCode());
                updateNoticeItem.setWarehouseName(pickItem.getWarehouseName());
                updateNoticeItem.setActualLocationCode(pickItem.getLocationCode());
                updateNoticeItem.setActualLocationName(pickItem.getLocationName());
                updateNoticeItem.setInboundNo(pickItem.getInboundNo());
                updateNoticeItem.setInboundTime(pickItem.getInboundTime());
            }
            updateNoticeItem.setShippingQualityNo(order.getFqcNo());
            updateNoticeItem.setShippingInspectorName(inspectorName);
            updateNoticeItem.setShippingInspectionTime(inspectionTime);
            updateNoticeItem.setShippingInspectionResult(result);
            updateNoticeItem.setShippingInspectionRemark(remark);
            updateNoticeItem.setMismatchReason(mismatchReason);
            updateNoticeItem.setQualityStatus(result);
            updateNoticeItem.setLockStatus(NOTICE_STATUS_INSPECTED);
            updateNoticeItem.setLockName(inspectorName);
            updateNoticeItem.setLockTime(inspectionTime);
            hcFgShippingNoticeItemMapper.updateById(updateNoticeItem);
        }
    }

    /** 样品按配货来源回写执行行，不进行客户批号匹配，也不伪造 ALIGNED 状态。 */
    private void writeBackSampleShippingInspection(QmsFqcOrderDO order,
                                                    List<QmsFqcShippingDetailDO> details, LocalDateTime now) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectById(order.getSourceReportId());
        if (notice == null || !"SAMPLE".equals(notice.getProductType())) {
            return;
        }
        Map<Long, HcFgShippingNoticePickItemDO> picks = selectActivePickItems(notice.getId()).stream()
                .collect(Collectors.toMap(HcFgShippingNoticePickItemDO::getId, item -> item));
        Map<Long, HcFgShippingNoticeItemDO> itemMap = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId()).stream()
                .collect(Collectors.toMap(HcFgShippingNoticeItemDO::getId, item -> item));
        Set<Long> usedItemIds = new HashSet<>();
        for (QmsFqcShippingDetailDO detail : details) {
            HcFgShippingNoticePickItemDO pick = picks.get(detail.getShippingPickItemId());
            HcFgShippingNoticeItemDO item = pick == null ? null : itemMap.get(pick.getSourceNoticeItemId());
            if (pick == null || item == null || !usedItemIds.add(item.getId())
                    || StrUtil.isBlank(detail.getActualSliceBatchNo())
                    || !Objects.equals(pick.getActualSliceBatchNo(), detail.getActualSliceBatchNo())) {
                throw invalidParamException("样品实际配货关联不完整或重复，请核对逐片配货记录后重试");
            }
            boolean ok = JUDGMENT_OK.equals(detail.getRowJudgment());
            qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                    .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                    .set(QmsFqcShippingDetailDO::getShippingNoticeItemId, ok ? item.getId() : null)
                    .set(QmsFqcShippingDetailDO::getAlignmentStatus, "NOT_REQUIRED")
                    .set(QmsFqcShippingDetailDO::getMismatchReason, null));
            // NG 行由原有退回流程释放；保留检验记录，补片后复用执行行。
            if (ok) {
                writeBackShippingInspection(order, detail, item, pick, true, null, now);
                hcFgShippingNoticeItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticeItemDO>()
                        .eq(HcFgShippingNoticeItemDO::getId, item.getId())
                        .set(HcFgShippingNoticeItemDO::getShippingInspectionRemark, "样品检验合格，无需批号对齐")
                        .set(HcFgShippingNoticeItemDO::getMismatchReason, null));
                hcFgShippingNoticePickItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticePickItemDO>()
                        .eq(HcFgShippingNoticePickItemDO::getId, pick.getId())
                        .set(HcFgShippingNoticePickItemDO::getShippingInspectionRemark, "样品检验合格，无需批号对齐"));
            }
        }
    }

    private void refreshShippingNoticeStatusAfterInspection(Long noticeId) {
        if (noticeId == null) {
            return;
        }
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectById(noticeId);
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            return;
        }
        List<HcFgShippingNoticePickItemDO> pickItems = selectActivePickItems(noticeId);
        int requiredQty = firstPositive(notice.getRequiredShipQty(), notice.getNoticeQty());
        boolean allInspected = !pickItems.isEmpty() && pickItems.stream()
                .allMatch(item -> StrUtil.isNotBlank(item.getShippingInspectionResult()));
        if (allInspected && pickItems.size() >= requiredQty) {
            HcFgShippingNoticeDO updateNotice = new HcFgShippingNoticeDO();
            updateNotice.setId(noticeId);
            updateNotice.setNoticeStatus(NOTICE_STATUS_INSPECTED);
            hcFgShippingNoticeMapper.updateById(updateNotice);
        }
    }

    private QmsFgShippingFqcRespVO returnForRework(QmsFqcOrderDO order, String rejectReason) {
        qmsFqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, order.getId())
                .set(QmsFqcOrderDO::getStatus, STATUS_INSPECTING)
                .set(QmsFqcOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsFqcOrderDO::getReleaseResult, null)
                .set(QmsFqcOrderDO::getReleaseTime, null)
                .set(QmsFqcOrderDO::getSheetLocked, false)
                .set(QmsFqcOrderDO::getLastReturnReason, rejectReason)
                .set(QmsFqcOrderDO::getReturnCount, (order.getReturnCount() == null ? 0 : order.getReturnCount()) + 1));
        qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, order.getId())
                .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_PENDING)
                .set(QmsFqcShippingDetailDO::getMismatchReason, null));
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

    private QmsFqcOrderDO validateFgShippingFqc(Long id) {
        QmsFqcOrderDO order = id == null ? null : qmsFqcOrderMapper.selectById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw invalidParamException("发货成品检验单不存在");
        }
        if (!SOURCE_MODULE_FG_SHIPPING_FQC.equals(order.getSourceModule())) {
            throw invalidParamException("当前FQC单不是发货成品检验单");
        }
        return order;
    }

    private void validateFgShippingFqcEditable(QmsFqcOrderDO order) {
        if (STATUS_COMPLETED.equals(order.getStatus()) || STATUS_CANCELED.equals(order.getStatus())) {
            throw invalidParamException("发货成品检验单已关闭，不能修改");
        }
    }

    private void assertBatchAlignmentRequired(HcFgShippingNoticeDO notice) {
        if (notice != null && "SAMPLE".equals(notice.getProductType())) {
            throw invalidParamException("样品无需批号对齐");
        }
    }

    private void validateFgShippingFqcAlignmentEditable(QmsFqcOrderDO order) {
        assertBatchAlignmentRequired(hcFgShippingNoticeMapper.selectById(order.getSourceReportId()));
        validateFgShippingFqcEditable(order);
        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_INSPECTING.equals(order.getStatus())) {
            throw invalidParamException("发货成品检验单已提交审核，不能再修改客户批号对齐");
        }
    }

    private void validateShippingAlignmentEditable(HcFgShippingNoticeDO notice) {
        if (!NOTICE_STATUS_INSPECTED.equals(notice.getNoticeStatus())) {
            throw invalidParamException("只有发货成品检验完成、且尚未进入包装的发货通知单允许调整客户批号对齐");
        }
    }

    private void validateAlignmentSnapshot(List<QmsFgShippingAlignmentSaveReqVO.Detail> detailReqs,
                                           Map<Long, HcFgShippingNoticeItemDO> noticeItemMap) {
        if (detailReqs == null || detailReqs.isEmpty()) {
            throw invalidParamException("客户批号对齐明细不能为空");
        }
        Set<Long> requestItemIds = detailReqs.stream()
                .map(QmsFgShippingAlignmentSaveReqVO.Detail::getShippingNoticeItemId)
                .collect(Collectors.toSet());
        if (requestItemIds.contains(null) || requestItemIds.size() != detailReqs.size()
                || !requestItemIds.equals(noticeItemMap.keySet())) {
            throw invalidParamException("客户批号对齐必须提交当前发货通知单的全部计划包片，请刷新后重试");
        }
    }

    private void clearShippingAlignmentSnapshot(List<QmsFqcShippingDetailDO> details,
                                                Set<Long> noticeItemIds,
                                                Collection<HcFgShippingNoticePickItemDO> pickItems,
                                                Set<Long> affectedFqcIds) {
        noticeItemIds.forEach(this::clearNoticeItemAlignment);
        for (QmsFqcShippingDetailDO detail : details) {
            if (!noticeItemIds.contains(detail.getShippingNoticeItemId())) {
                continue;
            }
            qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                    .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                    .set(QmsFqcShippingDetailDO::getShippingNoticeItemId, null)
                    .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_PENDING)
                    .set(QmsFqcShippingDetailDO::getMismatchReason, null));
            affectedFqcIds.add(detail.getFqcId());
        }
        for (HcFgShippingNoticePickItemDO pickItem : pickItems) {
            if (!noticeItemIds.contains(pickItem.getSourceNoticeItemId())) {
                continue;
            }
            hcFgShippingNoticePickItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticePickItemDO>()
                    .eq(HcFgShippingNoticePickItemDO::getId, pickItem.getId())
                    .set(HcFgShippingNoticePickItemDO::getSourceNoticeItemId, null));
        }
    }

    private void validateAlignmentRequestDetailsBelongToOrder(List<QmsFqcShippingDetailDO> details,
                                                              List<QmsFgShippingFqcAlignmentSaveReqVO.Detail> detailReqs) {
        if (details == null || details.isEmpty()) {
            throw invalidParamException("FQC发货明细不能为空");
        }
        if (detailReqs == null || detailReqs.isEmpty()) {
            throw invalidParamException("请选择需要对齐的计划包片行");
        }
        Long noticeId = details.get(0).getShippingNoticeId();
        Set<Long> noticeItemIds = hcFgShippingNoticeItemMapper.selectListByNoticeId(noticeId).stream()
                .map(HcFgShippingNoticeItemDO::getId)
                .collect(Collectors.toSet());
        Set<Long> reqNoticeItemIds = new HashSet<>();
        for (QmsFgShippingFqcAlignmentSaveReqVO.Detail detailReq : detailReqs) {
            Long noticeItemId = detailReq.getId();
            if (noticeItemId == null || !noticeItemIds.contains(noticeItemId)) {
                throw invalidParamException("客户批号对齐行与当前发货计划包片明细不一致，请刷新后重试");
            }
            if (!reqNoticeItemIds.add(noticeItemId)) {
                throw invalidParamException("客户批号对齐计划包片行重复，请刷新后重试");
            }
        }
    }

    private void validateSingleAlignmentRow(int rowNo,
                                            QmsFqcShippingDetailDO detail,
                                            HcFgShippingNoticeItemDO noticeItem,
                                            HcFgShippingNoticePickItemDO pickItem,
                                            Set<Long> usedPickItemIds,
                                            Set<String> usedActualSliceBatchNoSet) {
        if (detail == null) {
            throw invalidParamException("第 " + rowNo + " 行选择的实际片号未生成FQC检验明细，请刷新后重试");
        }
        if (!JUDGMENT_OK.equals(detail.getRowJudgment())) {
            throw invalidParamException("第 " + rowNo + " 行选择的实际片号未检验OK，不能对齐计划包片");
        }
        if (noticeItem == null || Boolean.TRUE.equals(noticeItem.getDeleted())) {
            throw invalidParamException("第 " + rowNo + " 行计划包片明细已不存在，请刷新发货需求单");
        }
        if (pickItem == null || Boolean.TRUE.equals(pickItem.getDeleted())) {
            throw invalidParamException("第 " + rowNo + " 行实际片号不属于当前发货需求单配货明细");
        }
        if (!usedPickItemIds.add(pickItem.getId())) {
            throw invalidParamException("实际片号不能重复绑定：" + firstNotBlank(pickItem.getActualSliceBatchNo(), pickItem.getStockNo()));
        }
        String actualSliceBatchNo = StrUtil.trimToNull(pickItem.getActualSliceBatchNo());
        if (StrUtil.isBlank(actualSliceBatchNo)) {
            throw invalidParamException("第 " + rowNo + " 行配货明细缺少实际片号");
        }
        String actualKey = actualSliceBatchNo.toLowerCase();
        if (!usedActualSliceBatchNoSet.add(actualKey)) {
            throw invalidParamException("实际片号不能重复绑定：" + actualSliceBatchNo);
        }
        String requiredModel = firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(),
                noticeItem.getModelCode(), detail.getModelCode());
        String actualModel = firstNotBlank(detail.getModelCode(), pickItem.getModelCode(), pickItem.getInternalModelCode());
        if (StrUtil.isNotBlank(requiredModel) && StrUtil.isNotBlank(actualModel)
                && !requiredModel.equalsIgnoreCase(actualModel)) {
            throw invalidParamException("第 " + rowNo + " 行型号不一致，客户批号要求 " + requiredModel + "，实际片号为 " + actualModel);
        }
        String requiredPrefix = firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo(), detail.getInternalItemCode());
        if (StrUtil.isNotBlank(requiredPrefix) && !actualSliceBatchNo.toLowerCase().startsWith(requiredPrefix.toLowerCase())) {
            throw invalidParamException("第 " + rowNo + " 行实际片号不满足内部编号前缀：" + requiredPrefix);
        }
    }

    private void unbindPreviousAlignment(Long fqcId, Long noticeItemId, Long keepDetailId) {
        QmsFqcShippingDetailDO previous = qmsFqcShippingDetailMapper.selectByFqcIdAndNoticeItemId(fqcId, noticeItemId);
        if (previous == null || Objects.equals(previous.getId(), keepDetailId)) {
            return;
        }
        qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getId, previous.getId())
                .set(QmsFqcShippingDetailDO::getShippingNoticeItemId, null)
                .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_PENDING)
                .set(QmsFqcShippingDetailDO::getMismatchReason, null));
    }

    private void unbindPreviousAlignmentAcrossNotice(List<QmsFqcShippingDetailDO> details,
                                                      Long noticeItemId, Long keepDetailId) {
        if (details == null || noticeItemId == null) {
            return;
        }
        details.stream()
                .filter(item -> Objects.equals(item.getShippingNoticeItemId(), noticeItemId))
                .filter(item -> !Objects.equals(item.getId(), keepDetailId))
                .forEach(item -> qmsFqcShippingDetailMapper.update(null,
                        new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                                .eq(QmsFqcShippingDetailDO::getId, item.getId())
                                .set(QmsFqcShippingDetailDO::getShippingNoticeItemId, null)
                                .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_PENDING)
                                .set(QmsFqcShippingDetailDO::getMismatchReason, null)));
    }

    private void writeBackShippingAlignment(QmsFqcOrderDO order,
                                            HcFgShippingNoticeDO notice,
                                            QmsFqcShippingDetailDO detail,
                                            HcFgShippingNoticeItemDO noticeItem,
                                            HcFgShippingNoticePickItemDO pickItem) {
        String actualSliceBatchNo = pickItem.getActualSliceBatchNo();
        String packageSliceNo = noticeItem.getPackageSliceNo();
        String modelCode = firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(),
                noticeItem.getModelCode(), pickItem.getModelCode(), notice.getModelCode());
        String internalItemCode = firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo());
        String materialCode = firstNotBlank(noticeItem.getMaterialCode(), pickItem.getMaterialCode(), notice.getMaterialCode());
        String materialName = firstNotBlank(noticeItem.getMaterialName(), pickItem.getMaterialName(), notice.getMaterialName());
        String productSize = firstNotBlank(noticeItem.getProductSize(), pickItem.getProductSize(), notice.getProductSize());
        String sliceBatchNo = firstNotBlank(packageSliceNo, noticeItem.getSliceBatchNo(), pickItem.getSliceBatchNo());
        qmsFqcShippingDetailMapper.update(null, new LambdaUpdateWrapper<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getId, detail.getId())
                .set(QmsFqcShippingDetailDO::getShippingNoticeItemId, noticeItem.getId())
                .set(QmsFqcShippingDetailDO::getShippingPickItemId, pickItem.getId())
                .set(QmsFqcShippingDetailDO::getFinishedStockId, pickItem.getFinishedStockId())
                .set(QmsFqcShippingDetailDO::getStockNo, pickItem.getStockNo())
                .set(QmsFqcShippingDetailDO::getActualSliceBatchNo, actualSliceBatchNo)
                .set(QmsFqcShippingDetailDO::getSliceBatchNo, sliceBatchNo)
                .set(QmsFqcShippingDetailDO::getCustomerId, notice.getCustomerId())
                .set(QmsFqcShippingDetailDO::getCustomerCode, notice.getCustomerCode())
                .set(QmsFqcShippingDetailDO::getCustomerName, notice.getCustomerName())
                .set(QmsFqcShippingDetailDO::getErpOrderNo, notice.getErpOrderNo())
                .set(QmsFqcShippingDetailDO::getCustomerProductBatchNo, noticeItem.getCustomerProductBatchNo())
                .set(QmsFqcShippingDetailDO::getPackageSliceNo, packageSliceNo)
                .set(QmsFqcShippingDetailDO::getMaterialCode, materialCode)
                .set(QmsFqcShippingDetailDO::getMaterialName, materialName)
                .set(QmsFqcShippingDetailDO::getModelCode, modelCode)
                .set(QmsFqcShippingDetailDO::getInternalItemCode, internalItemCode)
                .set(QmsFqcShippingDetailDO::getProductSize, productSize)
                .set(QmsFqcShippingDetailDO::getShippingQty, firstPositive(noticeItem.getActualShipQty(), noticeItem.getLockedQty(), pickItem.getActualShipQty(), 1))
                .set(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_ALIGNED)
                .set(QmsFqcShippingDetailDO::getMismatchReason, null));
        updatePieceItemsForShippingDetail(detail.getId(), actualSliceBatchNo, sliceBatchNo);

        hcFgShippingNoticeItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getId, noticeItem.getId())
                .set(HcFgShippingNoticeItemDO::getActualFinishedStockId, pickItem.getFinishedStockId())
                .set(HcFgShippingNoticeItemDO::getActualStockNo, pickItem.getStockNo())
                .set(HcFgShippingNoticeItemDO::getOuterBoxNo, pickItem.getOuterBoxNo())
                .set(HcFgShippingNoticeItemDO::getInnerUnitNo, pickItem.getInnerUnitNo())
                .set(HcFgShippingNoticeItemDO::getPackageNo, pickItem.getPackageNo())
                .set(HcFgShippingNoticeItemDO::getActualSliceBatchNo, actualSliceBatchNo)
                .set(HcFgShippingNoticeItemDO::getSliceBatchNo, sliceBatchNo)
                .set(HcFgShippingNoticeItemDO::getBatchNo, firstNotBlank(noticeItem.getCustomerProductBatchNo(), pickItem.getBatchNo()))
                .set(HcFgShippingNoticeItemDO::getMaterialCode, materialCode)
                .set(HcFgShippingNoticeItemDO::getMaterialName, materialName)
                .set(HcFgShippingNoticeItemDO::getModelCode, modelCode)
                .set(HcFgShippingNoticeItemDO::getProductSize, productSize)
                .set(HcFgShippingNoticeItemDO::getStockQty, firstPositive(pickItem.getStockQty(), 1))
                .set(HcFgShippingNoticeItemDO::getActualShipQty, firstPositive(pickItem.getActualShipQty(), 1))
                .set(HcFgShippingNoticeItemDO::getQualityStatus, pickItem.getQualityStatus())
                .set(HcFgShippingNoticeItemDO::getWarehouseCode, pickItem.getWarehouseCode())
                .set(HcFgShippingNoticeItemDO::getWarehouseName, pickItem.getWarehouseName())
                .set(HcFgShippingNoticeItemDO::getLocationCode, pickItem.getLocationCode())
                .set(HcFgShippingNoticeItemDO::getLocationName, pickItem.getLocationName())
                .set(HcFgShippingNoticeItemDO::getActualLocationCode, pickItem.getLocationCode())
                .set(HcFgShippingNoticeItemDO::getActualLocationName, pickItem.getLocationName())
                .set(HcFgShippingNoticeItemDO::getInboundNo, pickItem.getInboundNo())
                .set(HcFgShippingNoticeItemDO::getInboundTime, pickItem.getInboundTime())
                .set(HcFgShippingNoticeItemDO::getShippingQualityNo, order.getFqcNo())
                .set(HcFgShippingNoticeItemDO::getMismatchReason, null)
                .set(HcFgShippingNoticeItemDO::getLockStatus, firstNotBlank(pickItem.getLockStatus(), NOTICE_STATUS_PICKED)));

        hcFgShippingNoticePickItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getId, pickItem.getId())
                .set(HcFgShippingNoticePickItemDO::getSourceNoticeItemId, noticeItem.getId())
                .set(HcFgShippingNoticePickItemDO::getBatchNo, firstNotBlank(noticeItem.getCustomerProductBatchNo(), pickItem.getBatchNo()))
                .set(HcFgShippingNoticePickItemDO::getInternalModelCode, modelCode)
                .set(HcFgShippingNoticePickItemDO::getInternalItemCode, internalItemCode)
                .set(HcFgShippingNoticePickItemDO::getCustomerProductBatchNo, noticeItem.getCustomerProductBatchNo())
                .set(HcFgShippingNoticePickItemDO::getMaterialCode, materialCode)
                .set(HcFgShippingNoticePickItemDO::getMaterialName, materialName)
                .set(HcFgShippingNoticePickItemDO::getModelCode, modelCode)
                .set(HcFgShippingNoticePickItemDO::getProductSize, productSize)
                .set(HcFgShippingNoticePickItemDO::getShippingQualityNo, order.getFqcNo()));
    }

    private void validateInspectionItemsCompleted(Long fqcId, List<QmsFqcShippingDetailDO> details) {
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(fqcId);
        validateInspectionItemsCompleted(items, details);
    }

    private void validateInspectionItemsCompleted(List<QmsFqcItemDO> items,
                                                  List<QmsFqcShippingDetailDO> details) {
        if (items.isEmpty()) {
            throw invalidParamException("FQC检验项目不能为空");
        }
        Set<Long> ngDetailIds = (details == null ? Collections.<QmsFqcShippingDetailDO>emptyList() : details)
                .stream()
                .filter(detail -> JUDGMENT_NG.equals(detail.getRowJudgment()))
                .map(QmsFqcShippingDetailDO::getId)
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

    private void validateShippingDetailsCompleted(List<QmsFqcShippingDetailDO> details) {
        if (details == null || details.isEmpty()) {
            throw invalidParamException("FQC发货明细不能为空");
        }
        boolean hasPending = details.stream().anyMatch(detail ->
                !JUDGMENT_OK.equals(detail.getRowJudgment()) && !JUDGMENT_NG.equals(detail.getRowJudgment()));
        if (hasPending) {
            throw invalidParamException("存在未判定的发货片号，请先完成片级OK/NG标记");
        }
    }

    private void validateRequiredActualValues(Long fqcId) {
        List<String> missingNames = qmsFqcItemMapper.selectListByFqcId(fqcId).stream()
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

    private boolean isCompletedItem(QmsFqcRespVO.FqcItem item) {
        return JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult());
    }

    private boolean isCompletedItem(QmsFqcItemDO item) {
        return JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult());
    }

    private boolean isNgRespItem(QmsFqcRespVO.FqcItem item) {
        return JUDGMENT_NG.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getQaResult());
    }

    private boolean isNgItem(QmsFqcItemDO item) {
        return JUDGMENT_NG.equals(item.getItemResult())
                || JUDGMENT_NG.equals(item.getOperatorResult())
                || JUDGMENT_NG.equals(item.getQaResult());
    }

    private String normalizeRowJudgment(String rowJudgment) {
        String value = StrUtil.blankToDefault(rowJudgment, JUDGMENT_PENDING).trim().toUpperCase();
        if (!JUDGMENT_OK.equals(value) && !JUDGMENT_NG.equals(value) && !JUDGMENT_PENDING.equals(value)) {
            throw invalidParamException("片级判定只能是 OK、NG 或 PENDING");
        }
        return value;
    }

    private ScanMatch resolveScanMatch(QmsFqcScanReqVO scanReqVO, String scanCode) {
        QmsFqcShippingDetailDO detail = null;
        if (scanReqVO.getCurrentFqcId() != null) {
            detail = qmsFqcShippingDetailMapper.selectByFqcIdAndActualSliceBatchNo(scanReqVO.getCurrentFqcId(), scanCode);
        }
        if (detail == null) {
            detail = qmsFqcShippingDetailMapper.selectLatestByActualSliceBatchNo(scanCode);
        }
        if (detail != null) {
            return new ScanMatch(qmsFqcOrderMapper.selectById(detail.getFqcId()), detail,
                    SCAN_TARGET_ACTUAL_SLICE_BATCH_NO, "已定位到发货实际片号");
        }
        QmsFqcOrderDO order = selectLatestFgShippingFqcByScanCode(scanCode);
        if (order == null) {
            return new ScanMatch(null, null, SCAN_TARGET_ACTUAL_SLICE_BATCH_NO, null);
        }
        detail = qmsFqcShippingDetailMapper.selectFirstByFqcId(order.getId());
        return new ScanMatch(order, detail, SCAN_TARGET_FQC_ORDER,
                detail == null ? "已打开发货成品检验单，当前单据缺少发货明细" : "已按单据号定位到发货明细");
    }

    private QmsFqcOrderDO selectLatestFgShippingFqcByScanCode(String scanCode) {
        if (StrUtil.isBlank(scanCode)) {
            return null;
        }
        return qmsFqcOrderMapper.selectOne(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_FG_SHIPPING_FQC)
                .eq(QmsFqcOrderDO::getDeleted, false)
                .and(wrapper -> wrapper.eq(QmsFqcOrderDO::getFqcNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getReportNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getSourceReportNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getWorkOrderNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getProductBatchNo, scanCode)
                        .or().eq(QmsFqcOrderDO::getBatchNo, scanCode))
                .orderByDesc(QmsFqcOrderDO::getId)
                .last("LIMIT 1"));
    }

    private void insertScanRecord(QmsFqcScanReqVO scanReqVO, QmsFqcOrderDO order, QmsFqcShippingDetailDO detail,
                                  String scanTargetType, String matchResult, String blockedReason) {
        Long matchedFqcId = detail == null ? order == null ? null : order.getId() : detail.getFqcId();
        String matchedFqcNo = detail == null ? order == null ? null : order.getFqcNo() : detail.getFqcNo();
        Long tenantId = detail == null ? order == null ? null : order.getTenantId() : detail.getTenantId();
        QmsFqcScanRecordDO record = QmsFqcScanRecordDO.builder()
                .scanCode(StrUtil.trimToEmpty(scanReqVO.getScanCode()))
                .scanTargetType(scanTargetType)
                .scanScene(scanReqVO.getScanScene())
                .matchResult(matchResult)
                .matchedFqcId(matchedFqcId)
                .matchedFqcNo(matchedFqcNo)
                .matchedSubmissionDetailId(detail == null ? null : detail.getId())
                .matchedProductionBatchNo(detail == null ? null : detail.getActualSliceBatchNo())
                .candidateCount(order == null ? 0 : 1)
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

    private List<HcFgShippingNoticePickItemDO> selectActivePickItems(Long noticeId) {
        if (noticeId == null) {
            return Collections.emptyList();
        }
        return hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(noticeId).stream()
                .filter(item -> ACTIVE_PICK_STATUSES.contains(StrUtil.blankToDefault(item.getLockStatus(), "")))
                .toList();
    }

    private void appendMismatch(List<String> reasons, String label, String expected, String actual) {
        String left = StrUtil.trimToEmpty(expected);
        String right = StrUtil.trimToEmpty(actual);
        if (StrUtil.isBlank(left) && StrUtil.isBlank(right)) {
            return;
        }
        if (!left.equalsIgnoreCase(right)) {
            reasons.add(label + "不一致");
        }
    }

    private int sumQty(Collection<HcFgShippingNoticePickItemDO> pickItems) {
        if (pickItems == null || pickItems.isEmpty()) {
            return 0;
        }
        return pickItems.stream()
                .mapToInt(item -> firstPositive(item.getActualShipQty(), item.getLockedQty(), 1))
                .sum();
    }

    private Integer firstPositive(Integer... values) {
        if (values != null) {
            for (Integer value : values) {
                if (value != null && value > 0) {
                    return value;
                }
            }
        }
        return 1;
    }

    private String resolveLoginUserName() {
        return firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private void fillRecheckSource(QmsFgShippingFqcRespVO respVO) {
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

    private String appendRemark(String current, String addition) {
        String normalizedCurrent = StrUtil.trimToNull(current);
        String normalizedAddition = StrUtil.trimToNull(addition);
        if (normalizedCurrent == null) {
            return normalizedAddition;
        }
        if (normalizedAddition == null || normalizedCurrent.contains(normalizedAddition)) {
            return normalizedCurrent;
        }
        return normalizedCurrent + "；" + normalizedAddition;
    }

    private static final class ScanMatch {

        private final QmsFqcOrderDO order;
        private final QmsFqcShippingDetailDO detail;
        private final String targetType;
        private final String message;

        private ScanMatch(QmsFqcOrderDO order, QmsFqcShippingDetailDO detail,
                          String targetType, String message) {
            this.order = order;
            this.detail = detail;
            this.targetType = targetType;
            this.message = message;
        }
    }

    private static final class AlignmentResult {

        private final boolean hasMismatch;
        private final boolean hasPendingPlan;

        private AlignmentResult(boolean hasMismatch, boolean hasPendingPlan) {
            this.hasMismatch = hasMismatch;
            this.hasPendingPlan = hasPendingPlan;
        }
    }
}
