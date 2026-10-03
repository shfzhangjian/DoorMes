package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionKeyUtils;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcAdhesiveProductionRecordServiceImpl implements HcAdhesiveProductionRecordService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String TYPE_GLUE_BOARD = "GLUE_BOARD";
    private static final String PROCESS_ADHESIVE = "ADHESIVE";
    private static final String PROCESS_ADHESIVE1 = "ADHESIVE1";
    private static final String PROCESS_ADHESIVE2 = "ADHESIVE2";
    private static final String CONSUME_TYPE_CORRECTION = "CORRECTION";
    private static final String RECORD_SOURCE_MASS_PRODUCTION = "量产报工";
    private static final String RECORD_SOURCE_RND_SAMPLE = "研发样品/手工消耗";

    @Resource
    private HcAdhesiveReportMapper adhesiveReportMapper;
    @Resource
    private HcAdhesive2ReportMapper adhesive2ReportMapper;
    @Resource
    private HcToolingConsumableConsumeMapper toolingConsumableConsumeMapper;
    @Resource
    private HcToolingConsumableLedgerMapper toolingConsumableLedgerMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private HcProductionRecordRevisionService productionRecordRevisionService;
    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Override
    public PageResult<HcAdhesiveProductionRecordRespVO> getAdhesive1Page(
            HcAdhesiveProductionRecordPageReqVO reqVO) {
        return page(getAdhesive1List(reqVO), reqVO);
    }

    @Override
    public List<HcAdhesiveProductionRecordRespVO> getAdhesive1List(
            HcAdhesiveProductionRecordPageReqVO reqVO) {
        Map<ProductionRecordKey, ProductionRecordAccumulator> result = new LinkedHashMap<>();
        for (HcAdhesiveReportDO report : selectAdhesive1Reports(reqVO)) {
            LocalDate reportDate = resolveDate(report.getReportDate(), report.getEndTime(), report.getRecorderTime());
            String batchNo = firstNotBlank(report.getSourceProductionBatchNo(), report.getProductionBatchNo(),
                    report.getParentProductionBatchNo(), report.getSourceBatchNo());
            ProductionRecordKey key = new ProductionRecordKey(reportDate, trimToNull(report.getModelCode()),
                    trimToNull(report.getMaterialCode()), trimToNull(batchNo),
                    trimToNull(report.getGlueBoardMaterialCode()), trimToNull(report.getGlueBoardBatchNo()),
                    RECORD_SOURCE_MASS_PRODUCTION);
            result.computeIfAbsent(key, ProductionRecordAccumulator::new)
                    .acceptMassProduction(report.getId(), reportDate, report.getInputLength(), report.getOutputLength(),
                            report.getGlueBoardUseLength(),
                            firstNotNull(report.getEndTime(), report.getRecorderTime()),
                            report.getRecorderName(), report.getRemark());
        }
        appendRAndDSampleConsumes(result, reqVO, PROCESS_ADHESIVE1);
        return applyPadTypeAndRevisions(toSortedList(result, HcProductionRecordRevisionServiceImpl.MODULE_ADHESIVE1),
                HcProductionRecordRevisionServiceImpl.MODULE_ADHESIVE1, reqVO);
    }

    @Override
    public PageResult<HcAdhesiveProductionRecordRespVO> getAdhesive2Page(
            HcAdhesiveProductionRecordPageReqVO reqVO) {
        return page(getAdhesive2List(reqVO), reqVO);
    }

    @Override
    public List<HcAdhesiveProductionRecordRespVO> getAdhesive2List(
            HcAdhesiveProductionRecordPageReqVO reqVO) {
        Map<ProductionRecordKey, ProductionRecordAccumulator> result = new LinkedHashMap<>();
        for (HcAdhesive2ReportDO report : selectAdhesive2Reports(reqVO)) {
            LocalDate reportDate = resolveDate(report.getReportDate(), report.getEndTime(), report.getRecorderTime());
            String batchNo = firstNotBlank(report.getParentProductionBatchNo(), report.getSourceBatchNo(),
                    normalizePieceBatchNo(report.getSourceProductionBatchNo()),
                    normalizePieceBatchNo(report.getProductionBatchNo()));
            // 同一 P/Q/R/S 分段按生产日期拆行，每行仅累计当日有效报工。
            ProductionRecordKey key = new ProductionRecordKey(reportDate, trimToNull(report.getModelCode()),
                    trimToNull(report.getMaterialCode()), trimToNull(batchNo),
                    trimToNull(report.getGlueBoardMaterialCode()), trimToNull(report.getGlueBoardBatchNo()),
                    RECORD_SOURCE_MASS_PRODUCTION);
            result.computeIfAbsent(key, ProductionRecordAccumulator::new)
                    .acceptMassProduction(report.getId(), reportDate, report.getInputLength(), report.getOutputLength(),
                            report.getGlueBoardUseLength(),
                            firstNotNull(report.getEndTime(), report.getRecorderTime()),
                            report.getRecorderName(), report.getRemark());
        }
        appendRAndDSampleConsumes(result, reqVO, PROCESS_ADHESIVE2);
        return applyPadTypeAndRevisions(toSortedList(result, HcProductionRecordRevisionServiceImpl.MODULE_ADHESIVE2),
                HcProductionRecordRevisionServiceImpl.MODULE_ADHESIVE2, reqVO);
    }

    private List<HcAdhesiveProductionRecordRespVO> applyPadTypeAndRevisions(
            List<HcAdhesiveProductionRecordRespVO> rows, String moduleCode,
            HcAdhesiveProductionRecordPageReqVO reqVO) {
        Map<String, String> padTypes = padTypeResolver.resolveByModelCodes(
                rows.stream().map(HcAdhesiveProductionRecordRespVO::getModelCode).toList());
        rows.forEach(row -> row.setPadType(row.getModelCode() == null ? null : padTypes.get(row.getModelCode())));
        return productionRecordRevisionService.applyRevisions(moduleCode, rows).stream()
                .filter(row -> padTypeResolver.matchesFilter(reqVO.getPadType(), row.getPadType()))
                .toList();
    }

    /**
     * 研发样品没有量产计划和报工事实，直接投影胶板消耗流水；不写入、也不伪造粘胶报工。
     * 粘胶1、粘胶2均按实际消耗日期拆行。
     */
    private void appendRAndDSampleConsumes(Map<ProductionRecordKey, ProductionRecordAccumulator> result,
                                            HcAdhesiveProductionRecordPageReqVO reqVO,
                                            String processCode) {
        List<HcToolingConsumableConsumeDO> consumes = selectRAndDSampleConsumes(reqVO, processCode);
        if (consumes.isEmpty()) {
            return;
        }
        Map<Long, HcToolingConsumableLedgerDO> ledgerMap = loadLedgerMap(consumes);
        Map<Long, String> creatorNameMap = loadCreatorNameMap(consumes);
        for (HcToolingConsumableConsumeDO consume : consumes) {
            LocalDate reportDate = consume.getConsumeTime() == null ? null : consume.getConsumeTime().toLocalDate();
            HcToolingConsumableLedgerDO ledger = ledgerMap.get(consume.getLedgerId());
            ProductionRecordKey key = new ProductionRecordKey(reportDate,
                    trimToNull(consume.getProductModelCode()), trimToNull(consume.getProductMaterialCode()),
                    trimToNull(consume.getProductBatchNo()),
                    ledger == null ? null : trimToNull(ledger.getErpMaterialCode()),
                    trimToNull(consume.getBatchNo()), RECORD_SOURCE_RND_SAMPLE);
            result.computeIfAbsent(key, ProductionRecordAccumulator::new)
                    .acceptRAndDSample(consume.getId(), reportDate, consume.getConsumeQty(),
                            signedRAndDSampleProductQty(consume, consume.getProductInputQty()),
                            signedRAndDSampleProductQty(consume, consume.getProductOutputQty()), consume.getConsumeTime(),
                            resolveCreatorName(consume.getCreator(), creatorNameMap), consume.getRemark());
        }
    }

    /**
     * BaseDO.creator 保存的是系统用户 ID；生产记录表展示时需要转换为人员昵称。
     * 历史数据可能存有非数字创建人，或对应用户已不存在，此时保留原值以保证可追溯。
     */
    private Map<Long, String> loadCreatorNameMap(List<HcToolingConsumableConsumeDO> consumes) {
        Set<Long> creatorIds = consumes.stream()
                .map(HcToolingConsumableConsumeDO::getCreator)
                .map(this::parseUserId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (creatorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(creatorIds);
        if (userMap == null || userMap.isEmpty()) {
            return Map.of();
        }
        return userMap.values().stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(java.util.stream.Collectors.toMap(AdminUserRespDTO::getId,
                        user -> firstNotBlank(user.getNickname(), String.valueOf(user.getId())),
                        (left, right) -> left));
    }

    private String resolveCreatorName(String creator, Map<Long, String> creatorNameMap) {
        Long creatorId = parseUserId(creator);
        return creatorId == null ? creator : firstNotBlank(creatorNameMap.get(creatorId), creator);
    }

    private Long parseUserId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal signedRAndDSampleProductQty(HcToolingConsumableConsumeDO consume, BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (CONSUME_TYPE_CORRECTION.equalsIgnoreCase(StrUtil.trimToEmpty(consume.getConsumeType()))) {
            return value.negate();
        }
        return value;
    }

    private List<HcToolingConsumableConsumeDO> selectRAndDSampleConsumes(
            HcAdhesiveProductionRecordPageReqVO reqVO, String processCode) {
        LambdaQueryWrapperX<HcToolingConsumableConsumeDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HcToolingConsumableConsumeDO::getConsumableType, TYPE_GLUE_BOARD);
        wrapper.eq(HcToolingConsumableConsumeDO::getDeleted, false);
        if (PROCESS_ADHESIVE1.equals(processCode)) {
            wrapper.in(HcToolingConsumableConsumeDO::getProcessCode, List.of(PROCESS_ADHESIVE, PROCESS_ADHESIVE1));
        } else {
            wrapper.eq(HcToolingConsumableConsumeDO::getProcessCode, processCode);
        }
        if (reqVO.getReportDateStart() != null) {
            wrapper.ge(HcToolingConsumableConsumeDO::getConsumeTime, reqVO.getReportDateStart().atStartOfDay());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.lt(HcToolingConsumableConsumeDO::getConsumeTime,
                    reqVO.getReportDateEnd().plusDays(1).atStartOfDay());
        }
        wrapper.likeIfPresent(HcToolingConsumableConsumeDO::getProductModelCode, trimToNull(reqVO.getModelCode()));
        wrapper.likeIfPresent(HcToolingConsumableConsumeDO::getProductMaterialCode, trimToNull(reqVO.getMaterialCode()));
        wrapper.likeIfPresent(HcToolingConsumableConsumeDO::getProductBatchNo, trimToNull(reqVO.getBatchNo()));
        wrapper.likeIfPresent(HcToolingConsumableConsumeDO::getCreator, trimToNull(reqVO.getRecorderName()));
        wrapper.orderByAsc(HcToolingConsumableConsumeDO::getConsumeTime);
        wrapper.orderByAsc(HcToolingConsumableConsumeDO::getId);
        List<HcToolingConsumableConsumeDO> rows = toolingConsumableConsumeMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().filter(this::hasRAndDSampleProductSnapshot).toList();
    }

    private boolean hasRAndDSampleProductSnapshot(HcToolingConsumableConsumeDO consume) {
        return consume != null
                && StrUtil.isNotBlank(consume.getProductModelCode())
                && StrUtil.isNotBlank(consume.getProductMaterialCode())
                && StrUtil.isNotBlank(consume.getProductBatchNo());
    }

    private Map<Long, HcToolingConsumableLedgerDO> loadLedgerMap(List<HcToolingConsumableConsumeDO> consumes) {
        Set<Long> ledgerIds = consumes.stream()
                .map(HcToolingConsumableConsumeDO::getLedgerId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (ledgerIds.isEmpty()) {
            return Map.of();
        }
        List<HcToolingConsumableLedgerDO> ledgers = toolingConsumableLedgerMapper.selectBatchIds(ledgerIds);
        if (ledgers == null || ledgers.isEmpty()) {
            return Map.of();
        }
        return ledgers.stream().collect(java.util.stream.Collectors.toMap(
                HcToolingConsumableLedgerDO::getId, item -> item, (left, right) -> left));
    }

    private List<HcAdhesiveReportDO> selectAdhesive1Reports(HcAdhesiveProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcAdhesiveReportDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(HcAdhesiveReportDO::getReportStatus, STATUS_CONFIRMED, STATUS_SUBMITTED);
        wrapper.eq(HcAdhesiveReportDO::getDeleted, false);
        if (reqVO.getReportDateStart() != null) {
            wrapper.apply("COALESCE(report_date, DATE(end_time), DATE(recorder_time)) >= {0}", reqVO.getReportDateStart());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.apply("COALESCE(report_date, DATE(end_time), DATE(recorder_time)) <= {0}", reqVO.getReportDateEnd());
        }
        wrapper.likeIfPresent(HcAdhesiveReportDO::getModelCode, trimToNull(reqVO.getModelCode()));
        wrapper.likeIfPresent(HcAdhesiveReportDO::getMaterialCode, trimToNull(reqVO.getMaterialCode()));
        wrapper.likeIfPresent(HcAdhesiveReportDO::getRecorderName, trimToNull(reqVO.getRecorderName()));
        addAdhesive1BatchFilter(wrapper, reqVO.getBatchNo());
        wrapper.orderByAsc(HcAdhesiveReportDO::getReportDate);
        wrapper.orderByAsc(HcAdhesiveReportDO::getSourceProductionBatchNo);
        wrapper.orderByAsc(HcAdhesiveReportDO::getId);
        return adhesiveReportMapper.selectList(wrapper).stream()
                .filter(report -> STATUS_SUBMITTED.equalsIgnoreCase(StrUtil.trimToEmpty(report.getReportStatus()))
                        || STATUS_CONFIRMED.equalsIgnoreCase(StrUtil.trimToEmpty(report.getReportStatus())))
                .toList();
    }

    private List<HcAdhesive2ReportDO> selectAdhesive2Reports(HcAdhesiveProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcAdhesive2ReportDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(HcAdhesive2ReportDO::getReportStatus, STATUS_CONFIRMED, STATUS_SUBMITTED);
        wrapper.eq(HcAdhesive2ReportDO::getDeleted, false);
        if (reqVO.getReportDateStart() != null) {
            wrapper.apply("COALESCE(report_date, DATE(end_time), DATE(recorder_time)) >= {0}", reqVO.getReportDateStart());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.apply("COALESCE(report_date, DATE(end_time), DATE(recorder_time)) <= {0}", reqVO.getReportDateEnd());
        }
        wrapper.likeIfPresent(HcAdhesive2ReportDO::getModelCode, trimToNull(reqVO.getModelCode()));
        wrapper.likeIfPresent(HcAdhesive2ReportDO::getMaterialCode, trimToNull(reqVO.getMaterialCode()));
        wrapper.likeIfPresent(HcAdhesive2ReportDO::getRecorderName, trimToNull(reqVO.getRecorderName()));
        addAdhesive2BatchFilter(wrapper, reqVO.getBatchNo());
        wrapper.orderByAsc(HcAdhesive2ReportDO::getReportDate);
        wrapper.orderByAsc(HcAdhesive2ReportDO::getParentProductionBatchNo);
        wrapper.orderByAsc(HcAdhesive2ReportDO::getId);
        return adhesive2ReportMapper.selectList(wrapper).stream()
                .filter(report -> STATUS_SUBMITTED.equalsIgnoreCase(StrUtil.trimToEmpty(report.getReportStatus()))
                        || STATUS_CONFIRMED.equalsIgnoreCase(StrUtil.trimToEmpty(report.getReportStatus())))
                .toList();
    }

    private void addAdhesive1BatchFilter(LambdaQueryWrapperX<HcAdhesiveReportDO> wrapper, String value) {
        String batchNo = trimToNull(value);
        if (batchNo == null) {
            return;
        }
        wrapper.and(item -> item.like(HcAdhesiveReportDO::getSourceProductionBatchNo, batchNo)
                .or().like(HcAdhesiveReportDO::getProductionBatchNo, batchNo)
                .or().like(HcAdhesiveReportDO::getParentProductionBatchNo, batchNo)
                .or().like(HcAdhesiveReportDO::getSourceBatchNo, batchNo));
    }

    private void addAdhesive2BatchFilter(LambdaQueryWrapperX<HcAdhesive2ReportDO> wrapper, String value) {
        String batchNo = trimToNull(value);
        if (batchNo == null) {
            return;
        }
        wrapper.and(item -> item.like(HcAdhesive2ReportDO::getParentProductionBatchNo, batchNo)
                .or().like(HcAdhesive2ReportDO::getSourceBatchNo, batchNo)
                .or().like(HcAdhesive2ReportDO::getSourceProductionBatchNo, batchNo)
                .or().like(HcAdhesive2ReportDO::getProductionBatchNo, batchNo));
    }

    private PageResult<HcAdhesiveProductionRecordRespVO> page(
            List<HcAdhesiveProductionRecordRespVO> rows, HcAdhesiveProductionRecordPageReqVO reqVO) {
        int pageSize = reqVO.getPageSize() == null ? 20 : reqVO.getPageSize();
        if (pageSize <= 0) {
            return new PageResult<>(rows, (long) rows.size());
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private List<HcAdhesiveProductionRecordRespVO> toSortedList(
            Map<ProductionRecordKey, ProductionRecordAccumulator> source, String moduleCode) {
        return source.values().stream()
                .map(item -> item.toRespVO(moduleCode))
                .sorted(Comparator
                        .comparing(HcAdhesiveProductionRecordRespVO::getReportDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcAdhesiveProductionRecordRespVO::getRecordTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcAdhesiveProductionRecordRespVO::getBatchNo,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private LocalDate resolveDate(LocalDate reportDate, LocalDateTime endTime, LocalDateTime recorderTime) {
        if (reportDate != null) {
            return reportDate;
        }
        LocalDateTime fallback = firstNotNull(endTime, recorderTime);
        return fallback == null ? null : fallback.toLocalDate();
    }

    private String normalizePieceBatchNo(String value) {
        String text = trimToNull(value);
        return text == null ? null : text.replaceFirst("\\d{3}$", "");
    }

    private BigDecimal safeQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        String text = StrUtil.trimToEmpty(value);
        return StrUtil.isBlank(text) ? null : text;
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

    private record ProductionRecordKey(LocalDate reportDate, String modelCode, String materialCode,
                                       String batchNo, String glueBoardMaterialCode, String glueBoardBatchNo,
                                       String recordSource) {
    }

    private class ProductionRecordAccumulator {

        private final ProductionRecordKey key;
        private final Set<String> recorderNames = new LinkedHashSet<>();
        private final Set<String> remarks = new LinkedHashSet<>();
        private LocalDate reportDate;
        private BigDecimal inputQty;
        private BigDecimal outputQty;
        private BigDecimal glueBoardConsumeQty;
        private LocalDateTime recordTime;

        private ProductionRecordAccumulator(ProductionRecordKey key) {
            this.key = key;
            this.reportDate = key.reportDate();
        }

        private void acceptMassProduction(Long sourceId, LocalDate sourceReportDate,
                                          BigDecimal input, BigDecimal output, BigDecimal glueBoardConsume,
                                          LocalDateTime time,
                                          String recorderName, String remark) {
            acceptCommon(sourceId, sourceReportDate, time, recorderName, remark);
            inputQty = safeQty(inputQty).add(safeQty(input));
            outputQty = safeQty(outputQty).add(safeQty(output));
            if (glueBoardConsume != null) {
                glueBoardConsumeQty = safeQty(glueBoardConsumeQty).add(glueBoardConsume);
            }
        }

        private void acceptRAndDSample(Long sourceId, LocalDate sourceReportDate, BigDecimal consumeQty,
                                       BigDecimal input, BigDecimal output, LocalDateTime time,
                                       String recorderName, String remark) {
            acceptCommon(sourceId, sourceReportDate, time, recorderName, remark);
            glueBoardConsumeQty = safeQty(glueBoardConsumeQty).add(safeQty(consumeQty));
            if (input != null) {
                inputQty = safeQty(inputQty).add(input);
            }
            if (output != null) {
                outputQty = safeQty(outputQty).add(output);
            }
        }

        private void acceptCommon(Long sourceId, LocalDate sourceReportDate, LocalDateTime time,
                                  String recorderName, String remark) {
            if (sourceReportDate != null && (reportDate == null || sourceReportDate.isAfter(reportDate))) {
                reportDate = sourceReportDate;
            }
            if (time != null && (recordTime == null || time.isAfter(recordTime))) {
                recordTime = time;
            }
            addIfNotBlank(recorderNames, recorderName);
            addIfNotBlank(remarks, remark);
        }

        private void addIfNotBlank(Set<String> target, String value) {
            String text = trimToNull(value);
            if (text != null) {
                target.add(text);
            }
        }

        private HcAdhesiveProductionRecordRespVO toRespVO(String moduleCode) {
            HcAdhesiveProductionRecordRespVO row = new HcAdhesiveProductionRecordRespVO();
            // DAILY_V1 隔离旧整段修订，原修订审计保留，禁止套用到某一天。
            row.setId(HcProductionRecordRevisionKeyUtils.generateDisplayId(moduleCode + ":DAILY_V1", key));
            row.setReportDate(reportDate);
            row.setModelCode(key.modelCode());
            row.setMaterialCode(key.materialCode());
            row.setBatchNo(key.batchNo());
            row.setInputQty(inputQty);
            row.setOutputQty(outputQty);
            row.setGlueBoardMaterialCode(key.glueBoardMaterialCode());
            row.setGlueBoardBatchNo(key.glueBoardBatchNo());
            row.setGlueBoardConsumeQty(glueBoardConsumeQty);
            row.setRecordSource(key.recordSource());
            row.setRecorderName(String.join("；", recorderNames));
            row.setRecordTime(recordTime);
            row.setRemark(String.join("；", remarks));
            return row;
        }
    }

}
