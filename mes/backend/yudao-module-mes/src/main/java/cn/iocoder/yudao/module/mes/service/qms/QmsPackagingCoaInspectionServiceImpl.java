package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.FaiRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SampleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SubmitRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsPackagingCoaSampleClaimDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsPackagingCoaSampleClaimMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 从不合格待包装段抽取样片并创建 FAI 检验单。
 *
 * <p>该服务不向来源报工单回写“可用数量”；样片占用记录是待包装查询的唯一扣减依据，
 * 从而保证取消、驳回和 NG 后也不会把样片重新放回包装队列。</p>
 */
@Service
@Validated
public class QmsPackagingCoaInspectionServiceImpl implements QmsPackagingCoaInspectionService {

    private static final String SOURCE_MODULE_PACKAGING_COA = "PACKAGING_COA";
    private static final String SOURCE_MODULE_ADHESIVE2 = "ADHESIVE2_REPORT";
    private static final String SOURCE_CUT_ROUND_REPORT = "CUT_ROUND_REPORT";
    private static final String SOURCE_MANUAL_HISTORY = "MANUAL_HISTORY";
    private static final String INSPECTION_STATUS_COMPLETED = "COMPLETED";
    private static final String INSPECTION_RESULT_OK = "OK";
    private static final String INSPECTION_RESULT_NG = "NG";
    private static final String FAI_STATUS_COMPLETED = "COMPLETED";
    private static final String FAI_STATUS_PENDING = "PENDING";
    private static final String FAI_JUDGMENT_PENDING = "PENDING";
    private static final String FAI_STANDARD_MATCH_PRODUCT_MODEL_PROCESS = "PRODUCT_MODEL_PROCESS";
    private static final String FAI_SUBMISSION_TYPE_MASS_SHIPMENT = "MASS_SHIPMENT";
    private static final String FAI_TRIGGER_NEW_ORDER = "NEW_ORDER";
    private static final String PROCESS_CATEGORY_ADHESIVE2 = "ADHESIVE2";
    private static final String OPERATION_NAME_ADHESIVE2 = "粘胶2";
    private static final String MACHINE_CODE_COA_LAB = "COA_LAB";
    private static final String MACHINE_NAME_COA_LAB = "COA实验室";
    private static final DateTimeFormatter SOURCE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Resource
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Resource
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Resource
    private QmsPackagingCoaSampleClaimMapper qmsPackagingCoaSampleClaimMapper;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsFaiService qmsFaiService;

    @Override
    public PageResult<FaiRecordRespVO> getFaiPage(QmsFaiPageReqVO pageReqVO) {
        pageReqVO.setSourceModule(SOURCE_MODULE_PACKAGING_COA);
        PageResult<QmsFaiOrderDO> pageResult = qmsFaiService.getFaiPage(pageReqVO);
        List<FaiRecordRespVO> records = BeanUtils.toBean(pageResult.getList(), FaiRecordRespVO.class);
        if (records.isEmpty()) {
            return new PageResult<>(records, pageResult.getTotal());
        }

        Map<Long, String> sampleBatchNoByFaiId = new HashMap<>();
        qmsPackagingCoaSampleClaimMapper.selectListByFaiIds(records.stream()
                        .map(FaiRecordRespVO::getId)
                        .filter(Objects::nonNull)
                        .toList()).forEach(claim -> {
                    if (claim.getFaiId() != null && StrUtil.isNotBlank(claim.getSampleBatchNo())) {
                        sampleBatchNoByFaiId.putIfAbsent(claim.getFaiId(), claim.getSampleBatchNo());
                    }
                });
        records.forEach(record -> record.setCoaSampleBatchNo(sampleBatchNoByFaiId.get(record.getId())));
        return new PageResult<>(records, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitRespVO submit(SubmitReqVO reqVO) {
        String segmentBatchNo = normalizeSegmentBatchNo(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            throw invalidParamException("段批次不能为空");
        }
        List<SourceSample> samples = resolveAndValidateSamples(segmentBatchNo, reqVO.getSamples());
        List<QmsPackagingCoaSampleClaimDO> claims = reserveSamples(segmentBatchNo, samples);

        LocalDateTime submissionTime = LocalDateTime.now();
        List<QmsFaiOrderDO> faiOrders = new ArrayList<>();
        for (int index = 0; index < samples.size(); index++) {
            SourceSample sample = samples.get(index);
            QmsFaiSaveReqVO faiReqVO = buildFaiSaveReq(segmentBatchNo, sample, submissionTime, reqVO.getRemark());
            Long faiId = qmsFaiService.createFaiForInspectionPush(faiReqVO);
            QmsFaiOrderDO faiOrder = qmsFaiOrderMapper.selectById(faiId);
            QmsPackagingCoaSampleClaimDO claim = claims.get(index);
            claim.setFaiId(faiId);
            claim.setFaiNo(faiOrder == null ? null : faiOrder.getFaiNo());
            qmsPackagingCoaSampleClaimMapper.updateById(claim);
            faiOrders.add(faiOrder);
        }

        QmsFaiOrderDO latestFaiOrder = faiOrders.get(faiOrders.size() - 1);
        SubmitRespVO respVO = new SubmitRespVO();
        respVO.setFaiId(latestFaiOrder == null ? null : latestFaiOrder.getId());
        respVO.setFaiNo(latestFaiOrder == null ? null : latestFaiOrder.getFaiNo());
        respVO.setFaiIds(faiOrders.stream().filter(Objects::nonNull).map(QmsFaiOrderDO::getId).toList());
        respVO.setFaiNos(faiOrders.stream().filter(Objects::nonNull).map(QmsFaiOrderDO::getFaiNo).toList());
        respVO.setSegmentBatchNo(segmentBatchNo);
        respVO.setSampleCount(samples.size());
        respVO.setFaiStatus(latestFaiOrder == null ? FAI_STATUS_PENDING : latestFaiOrder.getStatus());
        respVO.setFaiJudgment(latestFaiOrder == null ? FAI_JUDGMENT_PENDING : latestFaiOrder.getJudgment());
        return respVO;
    }

    private List<SourceSample> resolveAndValidateSamples(String segmentBatchNo, List<SampleReqVO> sampleReqVOs) {
        Set<String> sourceKeys = new HashSet<>();
        List<SourceSample> samples = new ArrayList<>();
        for (SampleReqVO sampleReqVO : sampleReqVOs) {
            String sourceType = normalizeSourceType(sampleReqVO.getSourceType());
            Long sourceRecordId = sampleReqVO.getSourceRecordId();
            String sourceKey = sourceType + ":" + sourceRecordId;
            if (!sourceKeys.add(sourceKey)) {
                throw invalidParamException("同一片样品不能重复选择");
            }
            SourceSample sample = SOURCE_CUT_ROUND_REPORT.equals(sourceType)
                    ? resolveCutRoundSample(segmentBatchNo, sourceRecordId)
                    : resolveManualSample(segmentBatchNo, sourceRecordId);
            if (!isCurrentlyInUnqualifiedPackaging(sample)) {
                throw invalidParamException("片号 " + sample.sampleBatchNo() + " 当前不在不合格待包装段，请刷新后重新选择");
            }
            samples.add(sample);
        }
        if (samples.isEmpty()) {
            throw invalidParamException("请至少选择一片 COA 样片");
        }
        return samples;
    }

    private SourceSample resolveCutRoundSample(String segmentBatchNo, Long sourceRecordId) {
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectByIdForUpdate(sourceRecordId);
        if (report == null) {
            throw invalidParamException("待包装片不存在，请刷新后重试");
        }
        String actualSegment = resolveCutRoundSegmentBatchNo(report);
        if (!segmentBatchNo.equals(actualSegment)) {
            throw invalidParamException("所选片号不属于当前段批次，请重新选择");
        }
        if (!INSPECTION_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.trimToEmpty(report.getInspectionStatus()))
                || StrUtil.isBlank(report.getInspectionResult())) {
            throw invalidParamException("片号 " + firstNotBlank(report.getProductionBatchNo(), String.valueOf(report.getId()))
                    + " 尚未完成 FQC，不能作为 COA 样片");
        }
        if (hcInnerPackUnitItemMapper.selectByCutRoundReportId(report.getId()) != null
                || hcFinishedStockMapper.selectBySliceBatchNo(report.getProductionBatchNo()) != null) {
            throw invalidParamException("片号 " + report.getProductionBatchNo() + " 已包装或入库，不能送检");
        }
        return new SourceSample(SOURCE_CUT_ROUND_REPORT, report.getId(), actualSegment,
                firstNotBlank(report.getProductionBatchNo(), String.valueOf(report.getId())), report.getMaterialCode(),
                report.getMaterialName(), report.getModelCode(), normalizeResult(report.getInspectionResult()),
                null, report.getTenantId());
    }

    private SourceSample resolveManualSample(String segmentBatchNo, Long sourceRecordId) {
        HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectByIdForUpdate(sourceRecordId);
        if (piece == null) {
            throw invalidParamException("待包装历史片不存在，请刷新后重试");
        }
        String actualSegment = normalizeSegmentBatchNo(piece.getSegmentBatchNo());
        if (!segmentBatchNo.equals(actualSegment)) {
            throw invalidParamException("所选片号不属于当前段批次，请重新选择");
        }
        if (!"WAIT_PACKAGING".equalsIgnoreCase(StrUtil.trimToEmpty(piece.getRecordStatus()))
                || StrUtil.isBlank(piece.getInspectionResult())) {
            throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 当前不可作为 COA 样片");
        }
        return new SourceSample(SOURCE_MANUAL_HISTORY, piece.getId(), actualSegment, piece.getSliceBatchNo(),
                piece.getMaterialCode(), piece.getMaterialName(), piece.getModelCode(),
                normalizeResult(piece.getInspectionResult()), normalizeResult(piece.getCoaInspectionResult()), piece.getTenantId());
    }

    private boolean isCurrentlyInUnqualifiedPackaging(SourceSample sample) {
        if (INSPECTION_RESULT_NG.equals(sample.fqcResult())) {
            return true;
        }
        if (!INSPECTION_RESULT_OK.equals(sample.fqcResult())) {
            return false;
        }
        QmsFaiOrderDO latestCoa = selectLatestCoa(sample.segmentBatchNo());
        if (latestCoa != null) {
            return !isCoaReleased(latestCoa);
        }
        return INSPECTION_RESULT_NG.equals(sample.initialCoaResult());
    }

    /**
     * 新旧 COA 都参与当前判定；最新判定严格按 submission_time、id 排序，不能用 update_time。
     */
    private QmsFaiOrderDO selectLatestCoa(String segmentBatchNo) {
        Collection<String> segmentBatchNos = List.of(segmentBatchNo);
        List<QmsFaiOrderDO> orders = new ArrayList<>(qmsFaiOrderMapper.selectPackagingCoaListBySegmentBatchNos(segmentBatchNos));
        orders.addAll(qmsFaiOrderMapper.selectCoaListBySegmentBatchNos(SOURCE_MODULE_ADHESIVE2, segmentBatchNos));
        return orders.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(QmsFaiOrderDO::getSubmissionTime,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(QmsFaiOrderDO::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean isCoaReleased(QmsFaiOrderDO faiOrder) {
        return faiOrder != null
                && FAI_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.trimToEmpty(faiOrder.getStatus()))
                && INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(faiOrder.getJudgment()));
    }

    private List<QmsPackagingCoaSampleClaimDO> reserveSamples(String segmentBatchNo, List<SourceSample> samples) {
        List<QmsPackagingCoaSampleClaimDO> claims = new ArrayList<>();
        LocalDateTime claimTime = LocalDateTime.now();
        for (SourceSample sample : samples) {
            QmsPackagingCoaSampleClaimDO claim = QmsPackagingCoaSampleClaimDO.builder()
                    .segmentBatchNo(segmentBatchNo)
                    .sourceType(sample.sourceType())
                    .sourceRecordId(sample.sourceRecordId())
                    .sampleBatchNo(sample.sampleBatchNo())
                    .materialCode(sample.materialCode())
                    .materialName(sample.materialName())
                    .modelCode(sample.modelCode())
                    .fqcResult(sample.fqcResult())
                    .claimTime(claimTime)
                    .tenantId(sample.tenantId() == null ? currentTenantId() : sample.tenantId())
                    .build();
            try {
                qmsPackagingCoaSampleClaimMapper.insert(claim);
            } catch (DuplicateKeyException exception) {
                throw invalidParamException("片号 " + sample.sampleBatchNo() + " 已作为 COA 样片送检，请更换未送检的新片号");
            }
            claims.add(claim);
        }
        return claims;
    }

    private QmsFaiSaveReqVO buildFaiSaveReq(String segmentBatchNo, SourceSample sample,
                                             LocalDateTime submissionTime, String remark) {
        QmsFaiSaveReqVO reqVO = new QmsFaiSaveReqVO();
        reqVO.setWorkOrderNo(segmentBatchNo);
        reqVO.setSourceReportId(sample.sourceRecordId());
        reqVO.setSourceReportNo("PACKAGING-COA-" + segmentBatchNo + "-" + sample.sampleBatchNo() + "-"
                + SOURCE_NO_TIME_FORMATTER.format(submissionTime));
        reqVO.setSourceModule(SOURCE_MODULE_PACKAGING_COA);
        reqVO.setSourceOperationCode(PROCESS_CATEGORY_ADHESIVE2);
        reqVO.setSourceOperationName(OPERATION_NAME_ADHESIVE2);
        reqVO.setStandardMatchMode(FAI_STANDARD_MATCH_PRODUCT_MODEL_PROCESS);
        reqVO.setOperationCode(PROCESS_CATEGORY_ADHESIVE2);
        reqVO.setOperationName(OPERATION_NAME_ADHESIVE2);
        reqVO.setMachineCode(MACHINE_CODE_COA_LAB);
        reqVO.setMachineName(MACHINE_NAME_COA_LAB);
        reqVO.setMaterialCode(sample.materialCode());
        reqVO.setMaterialName(sample.materialName());
        reqVO.setProductModel(sample.modelCode());
        reqVO.setProductBatchNo(segmentBatchNo);
        reqVO.setInspectionQty(BigDecimal.ONE);
        reqVO.setProcessCategory(PROCESS_CATEGORY_ADHESIVE2);
        reqVO.setSubmissionType(FAI_SUBMISSION_TYPE_MASS_SHIPMENT);
        reqVO.setTriggerReason(FAI_TRIGGER_NEW_ORDER);
        reqVO.setStatus(FAI_STATUS_PENDING);
        reqVO.setJudgment(FAI_JUDGMENT_PENDING);
        reqVO.setSubmissionTime(submissionTime);
        reqVO.setSubmitterName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        reqVO.setRemark(firstNotBlank(StrUtil.trimToNull(remark), "包装段 COA 送检，样片：" + sample.sampleBatchNo()));
        return reqVO;
    }

    private String resolveCutRoundSegmentBatchNo(HcCutRoundReportDO report) {
        return chooseSegmentBatchNo(report.getParentProductionBatchNo(), report.getSourceProductionBatchNo(),
                report.getProductionBatchNo());
    }

    private String chooseSegmentBatchNo(String... batchNos) {
        String fallback = "";
        if (batchNos == null) {
            return fallback;
        }
        for (String batchNo : batchNos) {
            String normalized = normalizeSegmentBatchNo(batchNo);
            if (StrUtil.isBlank(normalized)) {
                continue;
            }
            if (StrUtil.isBlank(fallback)) {
                fallback = normalized;
            }
            if (normalized.matches("^.+[PQRS]$")) {
                return normalized;
            }
        }
        return fallback;
    }

    private String normalizeSegmentBatchNo(String batchNo) {
        String value = StrUtil.trimToEmpty(batchNo).toUpperCase(Locale.ROOT);
        value = value.replaceFirst("-J\\d+$", "");
        value = value.replaceFirst("-S\\d+$", "");
        return value.replaceFirst("^(.+[PQRS])\\d{3}[A-Z]?$", "$1");
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = StrUtil.trimToEmpty(sourceType).toUpperCase(Locale.ROOT);
        if (!SOURCE_CUT_ROUND_REPORT.equals(normalized) && !SOURCE_MANUAL_HISTORY.equals(normalized)) {
            throw invalidParamException("样片来源类型不支持");
        }
        return normalized;
    }

    private String normalizeResult(String result) {
        return StrUtil.trimToEmpty(result).toUpperCase(Locale.ROOT);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private record SourceSample(String sourceType, Long sourceRecordId, String segmentBatchNo, String sampleBatchNo,
                                String materialCode, String materialName, String modelCode, String fqcResult,
                                String initialCoaResult, Long tenantId) {
    }
}
