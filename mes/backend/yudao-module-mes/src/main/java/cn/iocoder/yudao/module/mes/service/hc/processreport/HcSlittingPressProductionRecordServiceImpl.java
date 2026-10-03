package cn.iocoder.yudao.module.mes.service.hc.processreport;

import com.fasterxml.jackson.core.type.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotSpareRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionServiceImpl;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionKeyUtils;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcSlittingPressProductionRecordServiceImpl implements HcSlittingPressProductionRecordService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String SPARE_TYPE_PRESS_ROLLER = "PRESS_ROLLER";
    private static final String SPARE_TYPE_BEARING = "BEARING";
    private static final String EVENT_REPLACE = "REPLACE";
    private static final String EVENT_CLEAN_RESET = "CLEAN_RESET";
    private static final String EVENT_CONFIG = "CONFIG";
    private static final String SELF_CHECK_NG = "NG";
    private static final String OUTPUT_STATUS_WAIT_NG_SHELF = "WAIT_NG_SHELF";
    private static final String EVENT_RND_MANUAL_USE = "RND_MANUAL_USE";
    private static final String RECORD_SOURCE_RND_MANUAL = "RND_MANUAL";
    private static final String DISPLAY_SOURCE_REPORT = "量产报工";
    private static final String DISPLAY_SOURCE_RND_MANUAL = "研发手工备件登记";
    private static final String PROCESS_CODE_SLITTING = "SLITTING";
    private static final String PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE = "PRE_PROCESS_SELF_CHECK";

    @Resource
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcAdhesiveReportMapper hcAdhesiveReportMapper;
    @Resource
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcPressSlotSpareRecordMapper hcPressSlotSpareRecordMapper;
    @Resource
    private HcProductionRecordRevisionService productionRecordRevisionService;
    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;
    @Resource
    private HcProductionRecorderNameResolver recorderNameResolver;

    @Override
    public PageResult<HcSlittingPressProductionRecordRespVO> getPage(HcSlittingPressProductionRecordPageReqVO reqVO) {
        List<HcSlittingPressProductionRecordRespVO> rows = getList(reqVO);
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        if (pageSize <= 0) {
            return new PageResult<>(rows, (long) rows.size());
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    @Override
    public List<HcSlittingPressProductionRecordRespVO> getList(HcSlittingPressProductionRecordPageReqVO reqVO) {
        List<HcPressSlotReportDO> reports = selectPressSlotReports(reqVO);
        List<RndManualProductionRecord> rndManualRecords = selectRndManualProductionRecords(reqVO);
        Map<Long, HcSlittingSliceRecordDO> sliceMap = Collections.emptyMap();
        Map<Long, HcPlanOrderOperationDO> operationMap = Collections.emptyMap();
        Map<String, List<HcPressSlotSpareRecordDO>> spareEventMap = Collections.emptyMap();
        if (!reports.isEmpty()) {
            sliceMap = selectSlittingSliceMap(reports);
            operationMap = selectOperationMap(reports);
            spareEventMap = selectSpareEventMap(reports, operationMap);
        }
        fillRndManualUseDays(rndManualRecords);
        Map<SlittingPressRecordKey, SlittingPressRecordAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (HcPressSlotReportDO report : reports) {
            HcSlittingSliceRecordDO slice = sliceMap.get(report.getSourceSlittingSliceId());
            HcPlanOrderOperationDO operation = operationMap.get(report.getPlanOperationId());
            SlittingPressRecordKey key = new SlittingPressRecordKey(
                    resolveReportDate(report),
                    firstNotBlank(report.getModelCode(), slice == null ? null : slice.getSizeCode()),
                    report.getMaterialCode(),
                    resolveBatchNo(report, slice));
            accumulatorMap.computeIfAbsent(key, SlittingPressRecordAccumulator::new)
                    .accept(report, slice, operation, spareEventMap);
        }
        for (RndManualProductionRecord record : rndManualRecords) {
            SlittingPressRecordKey key = new SlittingPressRecordKey(
                    record.reportDate,
                    record.modelCode,
                    record.materialCode,
                    record.productionBatchNo, record.padType);
            accumulatorMap.computeIfAbsent(key, SlittingPressRecordAccumulator::new)
                    .acceptRndManual(record);
        }
        appendDailySlittingRecords(accumulatorMap, reqVO);
        List<HcSlittingPressProductionRecordRespVO> result = accumulatorMap.values().stream()
                .map(SlittingPressRecordAccumulator::toRespVO)
                .sorted(Comparator
                        .comparing((HcSlittingPressProductionRecordRespVO item) -> firstNotNull(
                                        item.getRecordTime(),
                                        item.getReportDate() == null ? null : item.getReportDate().atStartOfDay()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcSlittingPressProductionRecordRespVO::getReportDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcSlittingPressProductionRecordRespVO::getModelCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(HcSlittingPressProductionRecordRespVO::getMaterialCode,
                                Comparator.nullsLast(String::compareTo))
                .thenComparing(HcSlittingPressProductionRecordRespVO::getBatchNo,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
        fillPadTypes(result);
        List<HcSlittingPressProductionRecordRespVO> revisedRows = productionRecordRevisionService.applyRevisions(
                HcProductionRecordRevisionServiceImpl.MODULE_SLITTING_PRESS, result);
        HcProductionRecorderNameResolver.Names names = recorderNameResolver.loadNames();
        revisedRows.forEach(row -> row.setRecorderName(names.normalize(row.getRecorderName())));
        return revisedRows.stream()
                .filter(row -> names.matches(row.getRecorderName(), reqVO.getRecorderName()))
                .filter(row -> padTypeResolver.matchesFilter(reqVO.getPadType(), row.getPadType()))
                .toList();
    }

    private void fillPadTypes(List<HcSlittingPressProductionRecordRespVO> rows) {
        Map<String, String> padTypes = padTypeResolver.resolveByModelCodes(
                rows.stream().map(HcSlittingPressProductionRecordRespVO::getModelCode).toList());
        rows.stream().filter(row -> row.getPadType() == null)
                .forEach(row -> row.setPadType(row.getModelCode() == null ? null : padTypes.get(row.getModelCode())));
    }

    private List<HcPressSlotReportDO> selectPressSlotReports(HcSlittingPressProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcPressSlotReportDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(HcPressSlotReportDO::getReportStatus, STATUS_CONFIRMED, STATUS_SUBMITTED);
        wrapper.eq(HcPressSlotReportDO::getDeleted, false);
        if (reqVO.getReportDateStart() != null) {
            wrapper.apply("COALESCE(report_date, DATE(confirmer_time), DATE(end_time), DATE(recorder_time)) >= {0}", reqVO.getReportDateStart());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.apply("COALESCE(report_date, DATE(confirmer_time), DATE(end_time), DATE(recorder_time)) <= {0}", reqVO.getReportDateEnd());
        }
        wrapper.likeIfPresent(HcPressSlotReportDO::getModelCode, trimToNull(reqVO.getModelCode()));
        wrapper.likeIfPresent(HcPressSlotReportDO::getMaterialCode, trimToNull(reqVO.getMaterialCode()));
        wrapper.orderByAsc(HcPressSlotReportDO::getReportDate);
        wrapper.orderByAsc(HcPressSlotReportDO::getSourceProductionBatchNo);
        wrapper.orderByAsc(HcPressSlotReportDO::getProductionBatchNo);
        wrapper.orderByAsc(HcPressSlotReportDO::getId);
        String batchNo = trimToNull(reqVO.getBatchNo());
        if (batchNo != null) {
            wrapper.and(item -> item
                    .like(HcPressSlotReportDO::getParentProductionBatchNo, batchNo)
                    .or()
                    .like(HcPressSlotReportDO::getSourceProductionBatchNo, batchNo)
                    .or()
                    .like(HcPressSlotReportDO::getSourceBatchNo, batchNo)
                    .or()
                    .like(HcPressSlotReportDO::getProductionBatchNo, batchNo));
        }
        return hcPressSlotReportMapper.selectList(wrapper);
    }

    private List<RndManualProductionRecord> selectRndManualProductionRecords(
            HcSlittingPressProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcPressSlotSpareRecordDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HcPressSlotSpareRecordDO::getDeleted, false)
                .eq(HcPressSlotSpareRecordDO::getEventType, EVENT_RND_MANUAL_USE)
                .eq(HcPressSlotSpareRecordDO::getRecordSource, RECORD_SOURCE_RND_MANUAL)
                .likeIfPresent(HcPressSlotSpareRecordDO::getModelCode, trimToNull(reqVO.getModelCode()))
                .likeIfPresent(HcPressSlotSpareRecordDO::getProductMaterialCode, trimToNull(reqVO.getMaterialCode()))
                .likeIfPresent(HcPressSlotSpareRecordDO::getProductionBatchNo, trimToNull(reqVO.getBatchNo()))
                .orderByAsc(HcPressSlotSpareRecordDO::getEventTime)
                .orderByAsc(HcPressSlotSpareRecordDO::getId);
        if (reqVO.getReportDateStart() != null) {
            wrapper.ge(HcPressSlotSpareRecordDO::getEventTime, reqVO.getReportDateStart().atStartOfDay());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.lt(HcPressSlotSpareRecordDO::getEventTime, reqVO.getReportDateEnd().plusDays(1).atStartOfDay());
        }
        Map<String, RndManualProductionRecord> result = new LinkedHashMap<>();
        for (HcPressSlotSpareRecordDO record : hcPressSlotSpareRecordMapper.selectList(wrapper)) {
            if (record.getEventTime() == null || StrUtil.isBlank(record.getProductionBatchNo())) {
                continue;
            }
            String groupKey = record.getEventTime().toLocalDate() + "|"
                    + firstNotBlank(record.getRecordGroupNo(), "RND-LEGACY-" + record.getId());
            result.computeIfAbsent(groupKey, ignored -> new RndManualProductionRecord(record))
                    .accept(record);
        }
        return new ArrayList<>(result.values());
    }

    private Map<Long, HcSlittingSliceRecordDO> selectSlittingSliceMap(List<HcPressSlotReportDO> reports) {
        List<Long> sliceIds = reports.stream()
                .map(HcPressSlotReportDO::getSourceSlittingSliceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (sliceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return hcSlittingSliceRecordMapper.selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                        .in(HcSlittingSliceRecordDO::getId, sliceIds)
                        .eq(HcSlittingSliceRecordDO::getDeleted, false))
                .stream()
                .collect(Collectors.toMap(HcSlittingSliceRecordDO::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, HcPlanOrderOperationDO> selectOperationMap(List<HcPressSlotReportDO> reports) {
        List<Long> operationIds = reports.stream()
                .map(HcPressSlotReportDO::getPlanOperationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (operationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return hcPlanOrderOperationMapper.selectList(new LambdaQueryWrapperX<HcPlanOrderOperationDO>()
                        .in(HcPlanOrderOperationDO::getId, operationIds))
                .stream()
                .collect(Collectors.toMap(HcPlanOrderOperationDO::getId, Function.identity(), (left, right) -> left));
    }

    private Map<String, List<HcPressSlotSpareRecordDO>> selectSpareEventMap(List<HcPressSlotReportDO> reports,
            Map<Long, HcPlanOrderOperationDO> operationMap) {
        List<Long> equipmentIds = operationMap.values().stream()
                .map(HcPlanOrderOperationDO::getEquipmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (equipmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDateTime maxReportTime = reports.stream()
                .map(this::resolveReportTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return selectSpareEventMap(equipmentIds, maxReportTime);
    }

    private void fillRndManualUseDays(List<RndManualProductionRecord> records) {
        List<Long> equipmentIds = records.stream()
                .map(record -> record.equipmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        LocalDateTime maxEventTime = records.stream()
                .map(record -> record.eventTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        Map<String, List<HcPressSlotSpareRecordDO>> spareEventMap = selectSpareEventMap(equipmentIds, maxEventTime);
        records.forEach(record -> record.fillUseDays(spareEventMap));
    }

    private Map<String, List<HcPressSlotSpareRecordDO>> selectSpareEventMap(
            Collection<Long> equipmentIds, LocalDateTime maxEventTime) {
        if (equipmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapperX<HcPressSlotSpareRecordDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HcPressSlotSpareRecordDO::getDeleted, false);
        wrapper.in(HcPressSlotSpareRecordDO::getEquipmentId, equipmentIds);
        wrapper.in(HcPressSlotSpareRecordDO::getSpareType, SPARE_TYPE_PRESS_ROLLER, SPARE_TYPE_BEARING);
        wrapper.orderByAsc(HcPressSlotSpareRecordDO::getEventTime);
        wrapper.orderByAsc(HcPressSlotSpareRecordDO::getId);
        if (maxEventTime != null) {
            wrapper.le(HcPressSlotSpareRecordDO::getEventTime, maxEventTime.plusDays(1));
        }
        Map<String, List<HcPressSlotSpareRecordDO>> result = new LinkedHashMap<>();
        for (HcPressSlotSpareRecordDO record : hcPressSlotSpareRecordMapper.selectList(wrapper)) {
            addSpareEvent(result, record, record.getAfterBatchNo());
            addSpareEvent(result, record, record.getBeforeBatchNo());
        }
        return result;
    }

    /** 分切独立按日投影，不能依赖压槽报工，否则会遗漏未压槽片并重复整段投入。 */
    private void appendDailySlittingRecords(Map<SlittingPressRecordKey, SlittingPressRecordAccumulator> result,
                                           HcSlittingPressProductionRecordPageReqVO reqVO) {
        List<HcSlittingSliceRecordDO> slices = hcSlittingSliceRecordMapper.selectProductionRecordSlices(reqVO);
        Set<Long> planIds = slices.stream().map(HcSlittingSliceRecordDO::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> sourceIds = slices.stream().map(HcSlittingSliceRecordDO::getSourceAdhesiveReportId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HcPlanOrderDO> plans = planIds.isEmpty() ? Map.of() : hcPlanOrderMapper.selectBatchIds(planIds)
                .stream().collect(Collectors.toMap(HcPlanOrderDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, HcAdhesiveReportDO> sources = sourceIds.isEmpty() ? Map.of() : hcAdhesiveReportMapper.selectBatchIds(sourceIds)
                .stream().collect(Collectors.toMap(HcAdhesiveReportDO::getId, Function.identity(), (a, b) -> a));
        for (HcSlittingSliceRecordDO slice : slices) {
            if (!STATUS_CONFIRMED.equalsIgnoreCase(slice.getScanStatus()) || Boolean.TRUE.equals(slice.getDeleted())) {
                continue;
            }
            HcPlanOrderDO plan = slice.getPlanId() == null ? null : plans.get(slice.getPlanId());
            HcAdhesiveReportDO source = slice.getSourceAdhesiveReportId() == null ? null : sources.get(slice.getSourceAdhesiveReportId());
            String modelCode = firstNotBlank(plan == null ? null : plan.getModelCode(),
                    source == null ? null : source.getModelCode());
            String materialCode = firstNotBlank(plan == null ? null : plan.getMaterialCode(),
                    source == null ? null : source.getMaterialCode());
            String batchNo = resolveSlittingSourceBatchNo(slice);
            if (!matchesText(modelCode, reqVO.getModelCode()) || !matchesText(materialCode, reqVO.getMaterialCode())
                    || !(matchesText(batchNo, reqVO.getBatchNo())
                         || matchesText(slice.getSliceSerialNo(), reqVO.getBatchNo())
                         || matchesText(slice.getSourceProductionBatchNo(), reqVO.getBatchNo()))) {
                continue;
            }
            LocalDateTime time = firstNotNull(slice.getSourceConsumeTime(), slice.getScanTime());
            // 缺少有效历史日期时保留未归属行；不猜测为压槽日或当前日期。
            LocalDate date = time == null || time.getYear() < 2000 ? null : time.toLocalDate();
            if (reqVO.getReportDateStart() != null && (date == null || date.isBefore(reqVO.getReportDateStart()))
                    || reqVO.getReportDateEnd() != null && (date == null || date.isAfter(reqVO.getReportDateEnd()))) {
                continue;
            }
            SlittingPressRecordKey key = new SlittingPressRecordKey(date, modelCode, materialCode, batchNo);
            result.computeIfAbsent(key, SlittingPressRecordAccumulator::new).acceptSlitting(slice, date == null ? null : time);
        }
    }

    private boolean matchesText(String value, String query) {
        return StrUtil.isBlank(query) || StrUtil.containsIgnoreCase(value, query.trim());
    }

    private BigDecimal resolveSlittingInput(HcSlittingSliceRecordDO slice) {
        if (slice.getSourceConsumeQty() != null && slice.getSourceConsumeQty().signum() > 0) {
            return slice.getSourceConsumeQty();
        }
        if (slice.getSliceLength() != null && slice.getSliceLength().signum() > 0) {
            return slice.getSliceLength();
        }
        if (slice.getStartPosition() != null && slice.getEndPosition() != null
                && slice.getEndPosition().compareTo(slice.getStartPosition()) > 0) {
            return slice.getEndPosition().subtract(slice.getStartPosition());
        }
        return null; // sourceLength 是整卷长度，不能当作当日实际投入。
    }

    private void addSpareEvent(Map<String, List<HcPressSlotSpareRecordDO>> target,
            HcPressSlotSpareRecordDO record, String batchNo) {
        if (record.getEquipmentId() == null || StrUtil.isBlank(record.getSpareType()) || StrUtil.isBlank(batchNo)) {
            return;
        }
        target.computeIfAbsent(spareEventKey(record.getEquipmentId(), record.getSpareType(), batchNo), ignored -> new ArrayList<>())
                .add(record);
    }

    private Integer calculateSpareUseDays(HcPressSlotReportDO report, HcPlanOrderOperationDO operation,
            Map<String, List<HcPressSlotSpareRecordDO>> spareEventMap, String spareType) {
        if (operation == null || operation.getEquipmentId() == null) {
            return null;
        }
        String batchNo = SPARE_TYPE_PRESS_ROLLER.equals(spareType)
                ? report.getPressureRollerBatchNo()
                : report.getBearingBatchNo();
        if (StrUtil.isBlank(batchNo)) {
            return null;
        }
        LocalDateTime reportTime = resolveReportTime(report);
        if (reportTime == null) {
            return null;
        }
        return calculateSpareUseDays(operation.getEquipmentId(), spareType, batchNo, reportTime, spareEventMap);
    }

    private Integer calculateSpareUseDays(Long equipmentId, String spareType, String batchNo,
            LocalDateTime recordTime, Map<String, List<HcPressSlotSpareRecordDO>> spareEventMap) {
        if (equipmentId == null || StrUtil.isBlank(batchNo) || recordTime == null) {
            return null;
        }
        List<HcPressSlotSpareRecordDO> events = spareEventMap.get(spareEventKey(equipmentId, spareType, batchNo));
        if (events == null || events.isEmpty()) {
            return null;
        }
        // 使用天数优先以实际清洗/更换为基准；CONFIG 仅作为从未维护过时的兜底初始日期。
        // 否则后补的配置记录会覆盖真实维护记录，导致页面显示错误的使用天数。
        HcPressSlotSpareRecordDO configFallback = null;
        for (int i = events.size() - 1; i >= 0; i--) {
            HcPressSlotSpareRecordDO event = events.get(i);
            if (event.getEventTime() == null || event.getEventTime().isAfter(recordTime)) {
                continue;
            }
            if (EVENT_CONFIG.equalsIgnoreCase(StrUtil.trimToEmpty(event.getEventType()))) {
                configFallback = event;
                continue;
            }
            if (!isUseDaysMaintenanceEvent(spareType, event.getEventType())) {
                continue;
            }
            return (int) Math.max(ChronoUnit.DAYS.between(event.getEventTime().toLocalDate(), recordTime.toLocalDate()), 0);
        }
        if (configFallback == null) {
            return null;
        }
        return (int) Math.max(ChronoUnit.DAYS.between(configFallback.getEventTime().toLocalDate(), recordTime.toLocalDate()), 0);
    }

    private boolean isUseDaysMaintenanceEvent(String spareType, String eventType) {
        if (SPARE_TYPE_PRESS_ROLLER.equals(spareType)) {
            return EVENT_CLEAN_RESET.equalsIgnoreCase(StrUtil.trimToEmpty(eventType))
                    || EVENT_REPLACE.equalsIgnoreCase(StrUtil.trimToEmpty(eventType));
        }
        return EVENT_REPLACE.equalsIgnoreCase(StrUtil.trimToEmpty(eventType));
    }

    private String spareEventKey(Long equipmentId, String spareType, String batchNo) {
        return equipmentId + "|" + StrUtil.trimToEmpty(spareType).toUpperCase() + "|" + StrUtil.trimToEmpty(batchNo);
    }

    private LocalDate resolveReportDate(HcPressSlotReportDO report) {
        if (report.getReportDate() != null) {
            return report.getReportDate();
        }
        LocalDateTime time = resolveReportTime(report);
        return time == null ? null : time.toLocalDate();
    }

    private LocalDateTime resolveReportTime(HcPressSlotReportDO report) {
        // 业务日期不能补成虚假的午夜完工时间。
        return firstNotNull(report.getConfirmerTime(), report.getEndTime(), report.getRecorderTime());
    }

    private String resolveBatchNo(HcPressSlotReportDO report, HcSlittingSliceRecordDO slice) {
        return firstNotBlank(
                report.getParentProductionBatchNo(),
                normalizePressSlotPieceNo(report.getSourceProductionBatchNo()),
                normalizePressSlotPieceNo(slice == null ? null : slice.getSourceProductionBatchNo()),
                normalizePressSlotPieceNo(report.getProductionBatchNo()),
                report.getSourceBatchNo(),
                slice == null ? null : slice.getSourceBatchNo());
    }

    private String normalizePressSlotPieceNo(String value) {
        String text = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        text = text.replaceFirst("(?i)-J\\d+$", "");
        text = text.replaceFirst("(?i)-S\\d+$", "");
        return text.replaceFirst("(?i)^(.+[PQRS])\\d{3}[A-Z]?$", "$1");
    }

    private String resolveSlittingSourceBatchNo(HcSlittingSliceRecordDO slice) {
        if (slice == null) {
            return null;
        }
        return firstNotBlank(slice.getSourceBatchNo(),
                normalizePressSlotPieceNo(slice.getSourceProductionBatchNo()),
                normalizePressSlotPieceNo(slice.getSliceSerialNo()));
    }

    private boolean isNgSlittingSlice(HcSlittingSliceRecordDO slice) {
        return slice != null && (SELF_CHECK_NG.equalsIgnoreCase(StrUtil.trimToEmpty(slice.getSelfCheck()))
                || OUTPUT_STATUS_WAIT_NG_SHELF.equalsIgnoreCase(
                        StrUtil.trimToEmpty(slice.getOutputStockPostStatus()))
                || "NG_STORED".equalsIgnoreCase(StrUtil.trimToEmpty(slice.getOutputStockPostStatus())));
    }

    private boolean isEffectivePressSlotReport(HcPressSlotReportDO report, HcSlittingSliceRecordDO slice) {
        return report != null && !isPressSlotPreProcessSlittingNg(report) && !isNgSlittingSlice(slice);
    }

    private boolean isPressSlotPreProcessSlittingNg(HcPressSlotReportDO report) {
        return report != null && isPreProcessSelfCheckFeedback(
                report.getSelfCheck(), report.getExtraJson(), PROCESS_CODE_SLITTING);
    }

    private boolean isPreProcessSelfCheckFeedback(String selfCheck, String extraJson, String expectedProcessCode) {
        Map<String, Object> extra = parseExtraMap(extraJson);
        boolean attributed = PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE.equalsIgnoreCase(
                toStringValue(extra.get("ngAttributionType")))
                || isFlagEnabled(extra.get("preProcessSelfCheckAbnormal"));
        String processCode = firstNotBlank(toStringValue(extra.get("ngAttributionProcessCode")),
                toStringValue(extra.get("sourceNgProcessCode")));
        boolean abnormal = isNgResultValue(selfCheck)
                || isNgResultValue(extra.get("visualInspectionResult"))
                || isNgResultValue(extra.get("selfCheck"));
        return attributed && abnormal && expectedProcessCode.equalsIgnoreCase(processCode);
    }

    private Map<String, Object> parseExtraMap(String extraJson) {
        if (StrUtil.isBlank(extraJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> extra = JsonUtils.parseObjectQuietly(extraJson,
                new TypeReference<Map<String, Object>>() {
                });
        return extra == null ? Collections.emptyMap() : extra;
    }

    private boolean isFlagEnabled(Object value) {
        if (Boolean.TRUE.equals(value) || Integer.valueOf(1).equals(value)) {
            return true;
        }
        return Set.of("1", "TRUE", "Y", "YES").contains(StrUtil.trimToEmpty(toStringValue(value)).toUpperCase());
    }

    private boolean isNgPressSlotReport(HcPressSlotReportDO report) {
        return report != null && (SELF_CHECK_NG.equalsIgnoreCase(StrUtil.trimToEmpty(report.getSelfCheck()))
                || OUTPUT_STATUS_WAIT_NG_SHELF.equalsIgnoreCase(
                        StrUtil.trimToEmpty(report.getOutputStockPostStatus()))
                || "NG_STORED".equalsIgnoreCase(StrUtil.trimToEmpty(report.getOutputStockPostStatus())));
    }

    private boolean isNgResultValue(Object value) {
        return Set.of("NG", "N", "FALSE", "0", "ABNORMAL", "FAILED", "FAIL", "不合格", "异常")
                .contains(StrUtil.trimToEmpty(toStringValue(value)).toUpperCase());
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal safeQty(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private Integer maxInteger(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null ? candidate : Math.max(current, candidate);
    }

    private String trimToNull(String value) {
        String text = StrUtil.trimToEmpty(value);
        return StrUtil.isBlank(text) ? null : text;
    }

    private void addIfNotBlank(Set<String> target, String value) {
        String text = StrUtil.trimToEmpty(value);
        if (StrUtil.isNotBlank(text)) {
            target.add(text);
        }
    }

    private String joinDistinct(Set<String> values) {
        return values.isEmpty() ? "" : String.join("；", values);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record SlittingPressRecordKey(LocalDate reportDate, String modelCode, String materialCode, String batchNo, String rndPadType) {
        SlittingPressRecordKey(LocalDate date, String model, String material, String batch) {
            this(date, model, material, batch, null);
        }
        @Override
        public String toString() {
            // 保留既有量产汇总行的修订ID；研发按垫型独立生成ID。
            return "SlittingPressRecordKey[reportDate=" + reportDate + ", modelCode=" + modelCode
                    + ", materialCode=" + materialCode + ", batchNo=" + batchNo
                    + (rndPadType == null ? "" : ", rndPadType=" + rndPadType) + "]";
        }
    }

    private class SlittingPressRecordAccumulator {

        private final SlittingPressRecordKey key;
        private BigDecimal slittingInputM = BigDecimal.ZERO;
        private Integer slittingOutputPcs = 0;
        private Integer slittingNgPcs = 0;
        private BigDecimal pressSlotActualInputPcs = BigDecimal.ZERO;
        private BigDecimal pressSlotActualOutputPcs = BigDecimal.ZERO;
        private BigDecimal pressSlotOutputPcs = BigDecimal.ZERO;
        private Integer rollerCleanAccumulatedPcs;
        private Integer rollerCleanUseDays;
        private Integer bearingReplaceAccumulatedPcs;
        private Integer bearingReplaceUseDays;
        private LocalDateTime recordTime;
        private final Set<Long> slittingSliceIds = new LinkedHashSet<>();
        private boolean missingSlittingInput;
        private final Set<String> recorderNames = new LinkedHashSet<>();
        private final Set<String> remarks = new LinkedHashSet<>();
        private final Set<String> recordSources = new LinkedHashSet<>();

        SlittingPressRecordAccumulator(SlittingPressRecordKey key) {
            this.key = key;
        }

        void accept(HcPressSlotReportDO report, HcSlittingSliceRecordDO slice, HcPlanOrderOperationDO operation,
                Map<String, List<HcPressSlotSpareRecordDO>> spareEventMap) {
            recordSources.add(DISPLAY_SOURCE_REPORT);
            if (isEffectivePressSlotReport(report, slice)) {
                pressSlotActualInputPcs = pressSlotActualInputPcs.add(safeQty(report.getInputLength()));
                pressSlotActualOutputPcs = pressSlotActualOutputPcs.add(safeQty(report.getOutputLength()));
                if (!isNgPressSlotReport(report)) {
                    pressSlotOutputPcs = pressSlotOutputPcs.add(safeQty(report.getOutputLength()));
                }
                rollerCleanAccumulatedPcs = maxInteger(rollerCleanAccumulatedPcs, report.getRollerCleanAccumulatedPcs());
                bearingReplaceAccumulatedPcs = maxInteger(bearingReplaceAccumulatedPcs, report.getBearingReplaceAccumulatedPcs());
                rollerCleanUseDays = maxInteger(rollerCleanUseDays,
                        calculateSpareUseDays(report, operation, spareEventMap, SPARE_TYPE_PRESS_ROLLER));
                bearingReplaceUseDays = maxInteger(bearingReplaceUseDays,
                        calculateSpareUseDays(report, operation, spareEventMap, SPARE_TYPE_BEARING));
            }
            addIfNotBlank(recorderNames, report.getRecorderName());
            addIfNotBlank(remarks, report.getRemark());
            LocalDateTime currentRecordTime = resolveReportTime(report);
            if (currentRecordTime != null && (recordTime == null || currentRecordTime.isAfter(recordTime))) {
                recordTime = currentRecordTime;
            }
        }

        void acceptRndManual(RndManualProductionRecord record) {
            recordSources.add(DISPLAY_SOURCE_RND_MANUAL);
            pressSlotActualInputPcs = pressSlotActualInputPcs.add(record.pressSlotInputPcs);
            pressSlotActualOutputPcs = pressSlotActualOutputPcs.add(record.pressSlotOutputPcs);
            pressSlotOutputPcs = pressSlotOutputPcs.add(record.pressSlotOutputPcs);
            rollerCleanAccumulatedPcs = maxInteger(rollerCleanAccumulatedPcs, record.rollerUseCount);
            bearingReplaceAccumulatedPcs = maxInteger(bearingReplaceAccumulatedPcs, record.bearingUseCount);
            rollerCleanUseDays = maxInteger(rollerCleanUseDays, record.rollerUseDays);
            bearingReplaceUseDays = maxInteger(bearingReplaceUseDays, record.bearingUseDays);
            addIfNotBlank(recorderNames, record.operatorName);
            addIfNotBlank(remarks, record.remark == null ? null : DISPLAY_SOURCE_RND_MANUAL + "：" + record.remark);
            if (record.eventTime != null && (recordTime == null || record.eventTime.isAfter(recordTime))) {
                recordTime = record.eventTime;
            }
        }

        void acceptSlitting(HcSlittingSliceRecordDO slice, LocalDateTime time) {
            if (slice.getId() == null || !slittingSliceIds.add(slice.getId())) {
                return;
            }
            recordSources.add(DISPLAY_SOURCE_REPORT);
            slittingOutputPcs++;
            if (isNgSlittingSlice(slice)) {
                slittingNgPcs++;
            }
            BigDecimal input = resolveSlittingInput(slice);
            if (input == null) {
                missingSlittingInput = true;
                addIfNotBlank(remarks, "部分分切片未记录有效占用米数，日投入待核实");
            } else {
                slittingInputM = slittingInputM.add(input);
            }
            if (time == null) {
                addIfNotBlank(remarks, "分切片未记录有效生产时间，日期待核实");
            } else if (recordTime == null || time.isAfter(recordTime)) {
                recordTime = time;
            }
            addIfNotBlank(recorderNames, slice.getScannerName());
            addIfNotBlank(remarks, slice.getRemark());
        }

        HcSlittingPressProductionRecordRespVO toRespVO() {
            HcSlittingPressProductionRecordRespVO respVO = new HcSlittingPressProductionRecordRespVO();
            respVO.setId(HcProductionRecordRevisionKeyUtils.generateDisplayId(
                    HcProductionRecordRevisionServiceImpl.MODULE_SLITTING_PRESS + ":DAILY_V1", key));
            respVO.setReportDate(key.reportDate());
            respVO.setPadType(key.rndPadType());
            respVO.setModelCode(key.modelCode());
            respVO.setMaterialCode(key.materialCode());
            respVO.setBatchNo(key.batchNo());
            respVO.setSlittingInputM(missingSlittingInput ? null : slittingInputM);
            respVO.setSlittingOutputPcs(slittingOutputPcs);
            respVO.setSlittingNgPcs(slittingNgPcs);
            respVO.setPressSlotActualInputPcs(pressSlotActualInputPcs);
            respVO.setPressSlotActualOutputPcs(pressSlotActualOutputPcs);
            respVO.setPressSlotOutputPcs(pressSlotOutputPcs);
            respVO.setRollerCleanAccumulatedPcs(rollerCleanAccumulatedPcs);
            respVO.setRollerCleanUseDays(rollerCleanUseDays);
            respVO.setBearingReplaceAccumulatedPcs(bearingReplaceAccumulatedPcs);
            respVO.setBearingReplaceUseDays(bearingReplaceUseDays);
            respVO.setRecordSource(joinDistinct(recordSources));
            respVO.setRecorderName(joinDistinct(recorderNames));
            respVO.setRecordTime(recordTime);
            respVO.setRemark(joinDistinct(remarks));
            return respVO;
        }
    }

    private class RndManualProductionRecord {

        private final LocalDate reportDate;
        private final String modelCode;
        private final String materialCode;
        private final String productionBatchNo;
        private final Long equipmentId;
        private final String padType;
        private BigDecimal pressSlotInputPcs = BigDecimal.ZERO;
        private BigDecimal pressSlotOutputPcs = BigDecimal.ZERO;
        private Integer rollerUseCount;
        private Integer bearingUseCount;
        private String rollerBatchNo;
        private String bearingBatchNo;
        private Integer rollerUseDays;
        private Integer bearingUseDays;
        private LocalDateTime eventTime;
        private String operatorName;
        private String remark;

        RndManualProductionRecord(HcPressSlotSpareRecordDO record) {
            reportDate = record.getEventTime().toLocalDate();
            modelCode = record.getModelCode();
            materialCode = record.getProductMaterialCode();
            productionBatchNo = record.getProductionBatchNo();
            equipmentId = record.getEquipmentId();
            padType = HcPressSlotRndPadType.forHistory(record.getPadType(), record.getEquipmentName());
        }

        void accept(HcPressSlotSpareRecordDO record) {
            if (pressSlotInputPcs.compareTo(BigDecimal.ZERO) == 0 && record.getPressSlotInputPcs() != null) {
                pressSlotInputPcs = record.getPressSlotInputPcs();
            }
            if (pressSlotOutputPcs.compareTo(BigDecimal.ZERO) == 0 && record.getPressSlotOutputPcs() != null) {
                pressSlotOutputPcs = record.getPressSlotOutputPcs();
            }
            if (SPARE_TYPE_PRESS_ROLLER.equalsIgnoreCase(StrUtil.trimToEmpty(record.getSpareType()))) {
                rollerUseCount = maxInteger(rollerUseCount, record.getAfterUseCount());
                rollerBatchNo = firstNotBlank(rollerBatchNo, record.getAfterBatchNo(), record.getBeforeBatchNo());
            }
            if (SPARE_TYPE_BEARING.equalsIgnoreCase(StrUtil.trimToEmpty(record.getSpareType()))) {
                bearingUseCount = maxInteger(bearingUseCount, record.getAfterUseCount());
                bearingBatchNo = firstNotBlank(bearingBatchNo, record.getAfterBatchNo(), record.getBeforeBatchNo());
            }
            if (record.getEventTime() != null && (eventTime == null || record.getEventTime().isAfter(eventTime))) {
                eventTime = record.getEventTime();
            }
            operatorName = firstNotBlank(operatorName, record.getOperatorName());
            remark = firstNotBlank(remark, record.getRemark());
        }

        void fillUseDays(Map<String, List<HcPressSlotSpareRecordDO>> spareEventMap) {
            rollerUseDays = calculateSpareUseDays(
                    equipmentId, SPARE_TYPE_PRESS_ROLLER, rollerBatchNo, eventTime, spareEventMap);
            bearingUseDays = calculateSpareUseDays(
                    equipmentId, SPARE_TYPE_BEARING, bearingBatchNo, eventTime, spareEventMap);
        }
    }

}
