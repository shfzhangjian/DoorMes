package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceImportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsCmpWarpageSliceStatDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsCmpWarpageSliceStatMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSubmissionDetailMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsCmpWarpageSliceStatServiceImpl implements QmsCmpWarpageSliceStatService {

    private static final String WARPAGE_ITEM_NAME = "Pad边缘翘曲";
    private static final String ALIGNMENT_ALIGNED = "ALIGNED";
    private static final String RESULT_OK = "OK";
    private static final String RESULT_NG = "NG";
    private static final BigDecimal WARPAGE_LIMIT_MM = new BigDecimal("20");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
    private static final int IMPORT_QUERY_BATCH_SIZE = 500;

    private static final ErrorCode CMP_WARPAGE_STAT_NOT_EXISTS =
            new ErrorCode(1008100190, "CMP软垫翘曲片号统计记录不存在");
    private static final ErrorCode CMP_WARPAGE_STAT_DUPLICATE =
            new ErrorCode(1008100191, "该生产片号已存在于CMP软垫翘曲片号统计中");

    @Resource
    private QmsCmpWarpageSliceStatMapper qmsCmpWarpageSliceStatMapper;
    @Resource
    private QmsFqcSubmissionDetailMapper qmsFqcSubmissionDetailMapper;
    @Resource
    private QmsFqcItemMapper qmsFqcItemMapper;
    @Resource
    private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;

    @Override
    public PageResult<QmsCmpWarpageSliceStatRespVO> getPage(QmsCmpWarpageSliceStatPageReqVO reqVO) {
        normalizePageReq(reqVO);
        PageResult<QmsCmpWarpageSliceStatDO> page = qmsCmpWarpageSliceStatMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public QmsCmpWarpageSliceStatRespVO get(Long id) {
        return toResp(validateExists(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(QmsCmpWarpageSliceStatSaveReqVO reqVO) {
        String productionSliceNo = normalizeSliceNo(reqVO.getProductionSliceNo());
        validateDuplicate(productionSliceNo, null);
        QmsCmpWarpageSliceStatDO entity = new QmsCmpWarpageSliceStatDO();
        fillEditableFields(entity, reqVO);
        entity.setProductionSliceNo(productionSliceNo);
        boolean manualOverride = Boolean.TRUE.equals(reqVO.getManualOverride()) || reqVO.getWarpageValueMm() != null;
        entity.setManualOverride(manualOverride);
        entity.setWarpageValueMm(reqVO.getWarpageValueMm());
        entity.setInspectionResult(judgeByValue(reqVO.getWarpageValueMm()));
        qmsCmpWarpageSliceStatMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(QmsCmpWarpageSliceStatUpdateReqVO reqVO) {
        QmsCmpWarpageSliceStatDO existing = validateExists(reqVO.getId());
        QmsCmpWarpageSliceStatDO update = new QmsCmpWarpageSliceStatDO();
        update.setId(existing.getId());
        update.setCustomerSliceNo(StrUtil.trimToNull(reqVO.getCustomerSliceNo()));
        update.setWarpageValueMm(reqVO.getWarpageValueMm());
        update.setInspectionResult(firstResult(reqVO.getInspectionResult(), judgeByValue(reqVO.getWarpageValueMm())));
        update.setManualOverride(true);
        persistManualUpdate(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        validateExists(id);
        qmsCmpWarpageSliceStatMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCmpWarpageSliceStatSyncRespVO sync(QmsCmpWarpageSliceStatSyncReqVO reqVO) {
        Set<Long> ids = new LinkedHashSet<>(reqVO.getIds());
        int syncedCount = 0;
        int sourceMissingCount = 0;
        int customerMissingCount = 0;
        int manualProtectedCount = 0;
        int invalidValueCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            SyncOutcome outcome = syncOne(validateExists(id), now);
            if (outcome.sourceFound()) {
                syncedCount++;
            } else {
                sourceMissingCount++;
            }
            if (!outcome.customerFound()) {
                customerMissingCount++;
            }
            if (outcome.manualProtected()) {
                manualProtectedCount++;
            }
            if (outcome.invalidValue()) {
                invalidValueCount++;
            }
        }
        return QmsCmpWarpageSliceStatSyncRespVO.builder()
                .requestedCount(ids.size())
                .syncedCount(syncedCount)
                .sourceMissingCount(sourceMissingCount)
                .customerMissingCount(customerMissingCount)
                .manualProtectedCount(manualProtectedCount)
                .invalidValueCount(invalidValueCount)
                .completedTime(now)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsCmpWarpageSliceImportRespVO syncSlices(QmsCmpWarpageSliceImportReqVO reqVO) {
        normalizeImportReq(reqVO);
        LambdaQueryWrapperX<QmsFqcSubmissionDetailDO> sourceQuery =
                new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                        .likeIfPresent(QmsFqcSubmissionDetailDO::getModelCode, reqVO.getModelCode());
        sourceQuery.isNotNull(QmsFqcSubmissionDetailDO::getProductionBatchNo);
        sourceQuery.ne(QmsFqcSubmissionDetailDO::getProductionBatchNo, "");
        sourceQuery.eq(QmsFqcSubmissionDetailDO::getDeleted, false);
        sourceQuery.orderByDesc(QmsFqcSubmissionDetailDO::getId);
        List<QmsFqcSubmissionDetailDO> filteredDetails = qmsFqcSubmissionDetailMapper
                .selectList(sourceQuery)
                .stream()
                .filter(detail -> matchesImportCondition(detail, reqVO))
                .toList();

        Map<String, QmsFqcSubmissionDetailDO> latestBySlice = new LinkedHashMap<>();
        for (QmsFqcSubmissionDetailDO detail : filteredDetails) {
            latestBySlice.putIfAbsent(normalizeSliceKey(detail.getProductionBatchNo()), detail);
        }
        Set<String> existingSliceKeys = loadExistingSliceKeys(latestBySlice.values().stream()
                .map(QmsFqcSubmissionDetailDO::getProductionBatchNo)
                .toList());
        List<QmsCmpWarpageSliceStatDO> rowsToInsert = new ArrayList<>();
        for (Map.Entry<String, QmsFqcSubmissionDetailDO> entry : latestBySlice.entrySet()) {
            if (existingSliceKeys.contains(entry.getKey())) {
                continue;
            }
            QmsFqcSubmissionDetailDO detail = entry.getValue();
            String segmentBatchNo = StrUtil.trimToNull(detail.getParentProductionBatchNo());
            QmsCmpWarpageSliceStatDO row = new QmsCmpWarpageSliceStatDO();
            row.setRecordDate(resolveSubmissionDate(detail));
            row.setModelCode(StrUtil.trimToNull(detail.getModelCode()));
            row.setParentBatchNo(resolveMotherBatchNo(segmentBatchNo));
            row.setSegmentSliceNo(segmentBatchNo);
            row.setProductionSliceNo(StrUtil.trim(detail.getProductionBatchNo()));
            row.setManualOverride(false);
            row.setSourceFqcId(detail.getFqcId());
            row.setSourceFqcNo(StrUtil.trimToNull(detail.getFqcNo()));
            row.setSourceSubmissionDetailId(detail.getId());
            row.setSyncMessage("片号已从裁切送检明细导入，待同步检测值或人工填写");
            rowsToInsert.add(row);
        }
        if (!rowsToInsert.isEmpty()) {
            qmsCmpWarpageSliceStatMapper.insertBatch(rowsToInsert, IMPORT_QUERY_BATCH_SIZE);
        }
        LocalDateTime completedTime = LocalDateTime.now();
        return QmsCmpWarpageSliceImportRespVO.builder()
                .sourceCount(latestBySlice.size())
                .importedCount(rowsToInsert.size())
                .existingCount(latestBySlice.size() - rowsToInsert.size())
                .duplicateSourceCount(filteredDetails.size() - latestBySlice.size())
                .completedTime(completedTime)
                .build();
    }

    private SyncOutcome syncOne(QmsCmpWarpageSliceStatDO current, LocalDateTime now) {
        QmsFqcShippingDetailDO shippingDetail = selectAlignedShippingDetail(current.getProductionSliceNo());
        boolean manualProtected = Boolean.TRUE.equals(current.getManualOverride());
        boolean customerFound = shippingDetail != null || StrUtil.isNotBlank(current.getCustomerSliceNo());
        QmsCmpWarpageSliceStatDO update = new QmsCmpWarpageSliceStatDO();
        update.setId(current.getId());
        update.setRecordDate(current.getRecordDate());
        update.setModelCode(current.getModelCode());
        update.setParentBatchNo(current.getParentBatchNo());
        update.setSegmentSliceNo(current.getSegmentSliceNo());
        update.setWarpageValueMm(current.getWarpageValueMm());
        update.setInspectionResult(current.getInspectionResult());
        update.setLastSyncTime(now);
        fillShippingSnapshot(update, shippingDetail);
        if (manualProtected && StrUtil.isNotBlank(current.getCustomerSliceNo())) {
            update.setCustomerSliceNo(current.getCustomerSliceNo());
        }

        QmsFqcSubmissionDetailDO submissionDetail =
                qmsFqcSubmissionDetailMapper.selectLatestByProductionBatchNo(current.getProductionSliceNo());
        if (submissionDetail == null) {
            clearInspectionSource(update, current);
            update.setSyncMessage(buildSyncMessage("未找到裁切成品检验片级明细", shippingDetail, current, false));
            persistSyncUpdate(update);
            return new SyncOutcome(false, customerFound, manualProtected, false);
        }

        QmsFqcItemDO warpageItem = qmsFqcItemMapper
                .selectListByFqcIdAndSubmissionDetailId(submissionDetail.getFqcId(), submissionDetail.getId())
                .stream()
                .filter(item -> WARPAGE_ITEM_NAME.equalsIgnoreCase(StrUtil.trim(item.getInspectionItem())))
                .max(Comparator.comparing(QmsFqcItemDO::getId, Comparator.nullsFirst(Long::compareTo)))
                .orElse(null);
        if (warpageItem == null) {
            clearInspectionSource(update, current);
            update.setSourceFqcId(submissionDetail.getFqcId());
            update.setSourceFqcNo(submissionDetail.getFqcNo());
            update.setSourceSubmissionDetailId(submissionDetail.getId());
            fillBasicSourceFields(update, submissionDetail, null, current);
            update.setSyncMessage(buildSyncMessage("未找到检测项目“Pad边缘翘曲”", shippingDetail, current, false));
            persistSyncUpdate(update);
            return new SyncOutcome(false, customerFound, manualProtected, false);
        }

        String sourceActualValue = StrUtil.trimToNull(warpageItem.getActualValue());
        BigDecimal parsedValue = parseWarpageValue(sourceActualValue);
        String sourceResult = firstResult(warpageItem.getItemResult(), warpageItem.getOperatorResult(),
                judgeByValue(parsedValue));
        boolean invalidValue = StrUtil.isNotBlank(sourceActualValue) && parsedValue == null;
        update.setSourceActualValue(sourceActualValue);
        update.setSourceItemResult(sourceResult);
        update.setSourceFqcId(submissionDetail.getFqcId());
        update.setSourceFqcNo(submissionDetail.getFqcNo());
        update.setSourceSubmissionDetailId(submissionDetail.getId());
        update.setSourceFqcItemId(warpageItem.getId());
        fillBasicSourceFields(update, submissionDetail, warpageItem, current);
        if (!manualProtected) {
            update.setWarpageValueMm(parsedValue);
            update.setInspectionResult(sourceResult);
        }
        String message;
        if (invalidValue) {
            message = "已找到翘曲项目，但源实际值无法按毫米数值解析";
        } else if (StrUtil.isBlank(sourceActualValue)) {
            message = "已找到翘曲项目，但尚未填写实际值";
        } else {
            message = "裁切FQC翘曲值与判定已同步";
        }
        update.setSyncMessage(buildSyncMessage(message, shippingDetail, current, manualProtected));
        persistSyncUpdate(update);
        return new SyncOutcome(true, customerFound, manualProtected, invalidValue);
    }

    private void persistManualUpdate(QmsCmpWarpageSliceStatDO update) {
        qmsCmpWarpageSliceStatMapper.update(null, new LambdaUpdateWrapper<QmsCmpWarpageSliceStatDO>()
                .eq(QmsCmpWarpageSliceStatDO::getId, update.getId())
                .set(QmsCmpWarpageSliceStatDO::getCustomerSliceNo, update.getCustomerSliceNo())
                .set(QmsCmpWarpageSliceStatDO::getWarpageValueMm, update.getWarpageValueMm())
                .set(QmsCmpWarpageSliceStatDO::getInspectionResult, update.getInspectionResult())
                .set(QmsCmpWarpageSliceStatDO::getManualOverride, update.getManualOverride()));
    }

    private void persistSyncUpdate(QmsCmpWarpageSliceStatDO update) {
        qmsCmpWarpageSliceStatMapper.update(null, new LambdaUpdateWrapper<QmsCmpWarpageSliceStatDO>()
                .eq(QmsCmpWarpageSliceStatDO::getId, update.getId())
                .set(QmsCmpWarpageSliceStatDO::getRecordDate, update.getRecordDate())
                .set(QmsCmpWarpageSliceStatDO::getModelCode, update.getModelCode())
                .set(QmsCmpWarpageSliceStatDO::getParentBatchNo, update.getParentBatchNo())
                .set(QmsCmpWarpageSliceStatDO::getSegmentSliceNo, update.getSegmentSliceNo())
                .set(QmsCmpWarpageSliceStatDO::getCustomerSliceNo, update.getCustomerSliceNo())
                .set(QmsCmpWarpageSliceStatDO::getCustomerCode, update.getCustomerCode())
                .set(QmsCmpWarpageSliceStatDO::getCustomerName, update.getCustomerName())
                .set(QmsCmpWarpageSliceStatDO::getShippingNoticeNo, update.getShippingNoticeNo())
                .set(QmsCmpWarpageSliceStatDO::getWarpageValueMm, update.getWarpageValueMm())
                .set(QmsCmpWarpageSliceStatDO::getInspectionResult, update.getInspectionResult())
                .set(QmsCmpWarpageSliceStatDO::getSourceActualValue, update.getSourceActualValue())
                .set(QmsCmpWarpageSliceStatDO::getSourceItemResult, update.getSourceItemResult())
                .set(QmsCmpWarpageSliceStatDO::getSourceFqcId, update.getSourceFqcId())
                .set(QmsCmpWarpageSliceStatDO::getSourceFqcNo, update.getSourceFqcNo())
                .set(QmsCmpWarpageSliceStatDO::getSourceSubmissionDetailId, update.getSourceSubmissionDetailId())
                .set(QmsCmpWarpageSliceStatDO::getSourceFqcItemId, update.getSourceFqcItemId())
                .set(QmsCmpWarpageSliceStatDO::getShippingFqcDetailId, update.getShippingFqcDetailId())
                .set(QmsCmpWarpageSliceStatDO::getLastSyncTime, update.getLastSyncTime())
                .set(QmsCmpWarpageSliceStatDO::getSyncMessage, update.getSyncMessage()));
    }

    private void fillBasicSourceFields(QmsCmpWarpageSliceStatDO update,
                                       QmsFqcSubmissionDetailDO submissionDetail,
                                       QmsFqcItemDO item,
                                       QmsCmpWarpageSliceStatDO current) {
        String segmentBatchNo = firstNotBlank(submissionDetail.getParentProductionBatchNo(),
                current.getSegmentSliceNo());
        update.setModelCode(firstNotBlank(submissionDetail.getModelCode(), current.getModelCode()));
        update.setParentBatchNo(firstNotBlank(resolveMotherBatchNo(segmentBatchNo), current.getParentBatchNo()));
        update.setSegmentSliceNo(segmentBatchNo);
        LocalDateTime inspectionTime = submissionDetail.getInspectionTime();
        if (inspectionTime == null && item != null) {
            inspectionTime = item.getOperatorTime();
        }
        if (inspectionTime == null && item != null) {
            inspectionTime = item.getUpdateTime();
        }
        update.setRecordDate(inspectionTime == null ? current.getRecordDate() : inspectionTime.toLocalDate());
    }

    private String resolveMotherBatchNo(String segmentBatchNo) {
        String value = StrUtil.trimToNull(segmentBatchNo);
        if (value == null) {
            return null;
        }
        String upperValue = value.toUpperCase(Locale.ROOT);
        return upperValue.matches(".*[PQRS]$") ? value.substring(0, value.length() - 1) : value;
    }

    private void fillShippingSnapshot(QmsCmpWarpageSliceStatDO update, QmsFqcShippingDetailDO shippingDetail) {
        if (shippingDetail == null) {
            update.setShippingFqcDetailId(null);
            update.setCustomerSliceNo(null);
            update.setCustomerCode(null);
            update.setCustomerName(null);
            update.setShippingNoticeNo(null);
            return;
        }
        update.setShippingFqcDetailId(shippingDetail.getId());
        update.setCustomerSliceNo(StrUtil.trimToNull(shippingDetail.getPackageSliceNo()));
        update.setCustomerCode(StrUtil.trimToNull(shippingDetail.getCustomerCode()));
        update.setCustomerName(StrUtil.trimToNull(shippingDetail.getCustomerName()));
        update.setShippingNoticeNo(StrUtil.trimToNull(shippingDetail.getShippingNoticeNo()));
    }

    private void clearInspectionSource(QmsCmpWarpageSliceStatDO update, QmsCmpWarpageSliceStatDO current) {
        update.setSourceActualValue(null);
        update.setSourceItemResult(null);
        update.setSourceFqcId(null);
        update.setSourceFqcNo(null);
        update.setSourceSubmissionDetailId(null);
        update.setSourceFqcItemId(null);
        if (!Boolean.TRUE.equals(current.getManualOverride())) {
            update.setWarpageValueMm(null);
            update.setInspectionResult(null);
        }
    }

    private QmsFqcShippingDetailDO selectAlignedShippingDetail(String productionSliceNo) {
        return qmsFqcShippingDetailMapper.selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getActualSliceBatchNo, productionSliceNo)
                .eq(QmsFqcShippingDetailDO::getAlignmentStatus, ALIGNMENT_ALIGNED)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getFqcId)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    private void fillEditableFields(QmsCmpWarpageSliceStatDO entity, QmsCmpWarpageSliceStatSaveReqVO reqVO) {
        entity.setRecordDate(reqVO.getRecordDate());
        entity.setModelCode(StrUtil.trimToNull(reqVO.getModelCode()));
        entity.setParentBatchNo(StrUtil.trimToNull(reqVO.getParentBatchNo()));
        entity.setSegmentSliceNo(StrUtil.trimToNull(reqVO.getSegmentSliceNo()));
        entity.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
    }

    private void normalizePageReq(QmsCmpWarpageSliceStatPageReqVO reqVO) {
        reqVO.setKeyword(StrUtil.trimToNull(reqVO.getKeyword()));
        reqVO.setModelCode(StrUtil.trimToNull(reqVO.getModelCode()));
        reqVO.setParentBatchNo(StrUtil.trimToNull(reqVO.getParentBatchNo()));
        reqVO.setSegmentSliceNo(StrUtil.trimToNull(reqVO.getSegmentSliceNo()));
        String result = StrUtil.trimToNull(reqVO.getInspectionResult());
        reqVO.setInspectionResult("PENDING".equalsIgnoreCase(result) ? "PENDING" : normalizeResult(result));
    }

    private void normalizeImportReq(QmsCmpWarpageSliceImportReqVO reqVO) {
        reqVO.setKeyword(StrUtil.trimToNull(reqVO.getKeyword()));
        reqVO.setModelCode(StrUtil.trimToNull(reqVO.getModelCode()));
        reqVO.setParentBatchNo(StrUtil.trimToNull(reqVO.getParentBatchNo()));
        reqVO.setSegmentSliceNo(StrUtil.trimToNull(reqVO.getSegmentSliceNo()));
    }

    private boolean matchesImportCondition(QmsFqcSubmissionDetailDO detail,
                                           QmsCmpWarpageSliceImportReqVO reqVO) {
        LocalDate sourceDate = resolveSubmissionDate(detail);
        if (reqVO.getRecordDateStart() != null
                && (sourceDate == null || sourceDate.isBefore(reqVO.getRecordDateStart()))) {
            return false;
        }
        if (reqVO.getRecordDateEnd() != null
                && (sourceDate == null || sourceDate.isAfter(reqVO.getRecordDateEnd()))) {
            return false;
        }
        String segmentBatchNo = StrUtil.trimToNull(detail.getParentProductionBatchNo());
        String motherBatchNo = resolveMotherBatchNo(segmentBatchNo);
        if (!containsIgnoreCase(motherBatchNo, reqVO.getParentBatchNo())) {
            return false;
        }
        if (!containsIgnoreCase(segmentBatchNo, reqVO.getSegmentSliceNo())) {
            return false;
        }
        if (StrUtil.isBlank(reqVO.getKeyword())) {
            return true;
        }
        return containsIgnoreCase(detail.getProductionBatchNo(), reqVO.getKeyword())
                || containsIgnoreCase(segmentBatchNo, reqVO.getKeyword())
                || containsIgnoreCase(motherBatchNo, reqVO.getKeyword())
                || containsIgnoreCase(detail.getFqcNo(), reqVO.getKeyword());
    }

    private boolean containsIgnoreCase(String value, String condition) {
        if (StrUtil.isBlank(condition)) {
            return true;
        }
        return StrUtil.isNotBlank(value)
                && value.toUpperCase(Locale.ROOT).contains(condition.toUpperCase(Locale.ROOT));
    }

    private LocalDate resolveSubmissionDate(QmsFqcSubmissionDetailDO detail) {
        LocalDateTime sourceTime = detail.getInspectionTime() == null
                ? detail.getCreateTime() : detail.getInspectionTime();
        return sourceTime == null ? null : sourceTime.toLocalDate();
    }

    private Set<String> loadExistingSliceKeys(List<String> sourceSliceNos) {
        Set<String> existingKeys = new HashSet<>();
        for (int start = 0; start < sourceSliceNos.size(); start += IMPORT_QUERY_BATCH_SIZE) {
            int end = Math.min(start + IMPORT_QUERY_BATCH_SIZE, sourceSliceNos.size());
            qmsCmpWarpageSliceStatMapper.selectList(new LambdaQueryWrapperX<QmsCmpWarpageSliceStatDO>()
                            .in(QmsCmpWarpageSliceStatDO::getProductionSliceNo, sourceSliceNos.subList(start, end))
                            .eq(QmsCmpWarpageSliceStatDO::getDeleted, false))
                    .forEach(row -> existingKeys.add(normalizeSliceKey(row.getProductionSliceNo())));
        }
        return existingKeys;
    }

    private String normalizeSliceKey(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private QmsCmpWarpageSliceStatDO validateExists(Long id) {
        QmsCmpWarpageSliceStatDO entity = id == null ? null : qmsCmpWarpageSliceStatMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw exception(CMP_WARPAGE_STAT_NOT_EXISTS);
        }
        return entity;
    }

    private void validateDuplicate(String productionSliceNo, Long excludeId) {
        QmsCmpWarpageSliceStatDO duplicate = qmsCmpWarpageSliceStatMapper.selectByProductionSliceNo(productionSliceNo);
        if (duplicate != null && !Objects.equals(duplicate.getId(), excludeId)) {
            throw exception(CMP_WARPAGE_STAT_DUPLICATE);
        }
    }

    private String normalizeSliceNo(String value) {
        return StrUtil.trim(value);
    }

    private BigDecimal parseWarpageValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value.replace(',', '.'));
        if (!matcher.find()) {
            return null;
        }
        try {
            BigDecimal result = new BigDecimal(matcher.group());
            String lowerValue = value.toLowerCase(Locale.ROOT);
            if (lowerValue.contains("cm") || lowerValue.contains("厘米")) {
                result = result.multiply(BigDecimal.TEN);
            }
            return result.signum() < 0 ? null : result.stripTrailingZeros();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String judgeByValue(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.compareTo(WARPAGE_LIMIT_MM) <= 0 ? RESULT_OK : RESULT_NG;
    }

    private String normalizeResult(String result) {
        if (StrUtil.isBlank(result)) {
            return null;
        }
        String normalized = StrUtil.trim(result).toUpperCase(Locale.ROOT);
        if (List.of("OK", "PASS", "QUALIFIED", "合格").contains(normalized)) {
            return RESULT_OK;
        }
        if (List.of("NG", "FAIL", "FAILED", "UNQUALIFIED", "不合格").contains(normalized)) {
            return RESULT_NG;
        }
        return null;
    }

    private String firstResult(String... candidates) {
        for (String candidate : candidates) {
            String normalized = normalizeResult(candidate);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }

    private String buildSyncMessage(String mainMessage,
                                    QmsFqcShippingDetailDO shippingDetail,
                                    QmsCmpWarpageSliceStatDO current,
                                    boolean manualProtected) {
        StringBuilder message = new StringBuilder(mainMessage);
        if (shippingDetail == null) {
            if (Boolean.TRUE.equals(current.getManualOverride()) && StrUtil.isNotBlank(current.getCustomerSliceNo())) {
                message.append("；未找到已对齐客户片号，人工发货片号已保留");
            } else {
                message.append("；未找到已对齐客户片号，发货片号保持为空");
            }
        }
        if (manualProtected || Boolean.TRUE.equals(current.getManualOverride())) {
            message.append("；人工编辑数据已保留");
        }
        return message.length() <= 500 ? message.toString() : message.substring(0, 500);
    }

    private QmsCmpWarpageSliceStatRespVO toResp(QmsCmpWarpageSliceStatDO entity) {
        QmsCmpWarpageSliceStatRespVO resp = new QmsCmpWarpageSliceStatRespVO();
        resp.setId(entity.getId());
        resp.setRecordDate(entity.getRecordDate());
        resp.setModelCode(entity.getModelCode());
        resp.setParentBatchNo(entity.getParentBatchNo());
        resp.setSegmentSliceNo(entity.getSegmentSliceNo());
        resp.setProductionSliceNo(entity.getProductionSliceNo());
        resp.setCustomerSliceNo(entity.getCustomerSliceNo());
        resp.setCustomerCode(entity.getCustomerCode());
        resp.setCustomerName(entity.getCustomerName());
        resp.setShippingNoticeNo(entity.getShippingNoticeNo());
        resp.setWarpageValueMm(entity.getWarpageValueMm());
        resp.setInspectionResult(entity.getInspectionResult());
        resp.setManualOverride(Boolean.TRUE.equals(entity.getManualOverride()));
        resp.setSourceActualValue(entity.getSourceActualValue());
        resp.setSourceItemResult(entity.getSourceItemResult());
        resp.setSourceFqcId(entity.getSourceFqcId());
        resp.setSourceFqcNo(entity.getSourceFqcNo());
        resp.setSourceSubmissionDetailId(entity.getSourceSubmissionDetailId());
        resp.setSourceFqcItemId(entity.getSourceFqcItemId());
        resp.setShippingFqcDetailId(entity.getShippingFqcDetailId());
        resp.setLastSyncTime(entity.getLastSyncTime());
        resp.setSyncMessage(entity.getSyncMessage());
        resp.setRemark(entity.getRemark());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private record SyncOutcome(boolean sourceFound, boolean customerFound, boolean manualProtected,
                               boolean invalidValue) {
    }
}
