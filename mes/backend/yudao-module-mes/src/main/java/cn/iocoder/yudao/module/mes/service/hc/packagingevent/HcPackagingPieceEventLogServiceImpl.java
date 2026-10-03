package cn.iocoder.yudao.module.mes.service.hc.packagingevent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingPieceEventLogDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingPieceEventLogMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class HcPackagingPieceEventLogServiceImpl implements HcPackagingPieceEventLogService {

    private static final String SOURCE_CUT_ROUND_REPORT = "CUT_ROUND_REPORT";
    private static final String SOURCE_MANUAL_HISTORY = "MANUAL_HISTORY";
    private static final String EVENT_NG_ENTER = "PACKAGING_NG_ENTER";
    private static final String EVENT_QUALITY_CHANGED = "PACKAGING_QUALITY_CHANGED";
    private static final DateTimeFormatter EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private HcPackagingPieceEventLogMapper hcPackagingPieceEventLogMapper;

    @Override
    public void recordCutRoundFqcResult(HcCutRoundReportDO report, String fqcResult, String operatorName,
                                        LocalDateTime eventTime, String refDocNo, String remark) {
        if (report == null || report.getId() == null || eventTime == null) {
            return;
        }
        String normalizedResult = normalize(fqcResult);
        boolean ngRelated = "NG".equals(normalizedResult)
                || hcPackagingPieceEventLogMapper.hasNgLifecycle(SOURCE_CUT_ROUND_REPORT, report.getId());
        if (!ngRelated) {
            return;
        }
        String eventType = "NG".equals(normalizedResult) ? EVENT_NG_ENTER : EVENT_QUALITY_CHANGED;
        insertIfAbsent(HcPackagingPieceEventLogDO.builder()
                .tenantId(report.getTenantId())
                .eventType(eventType)
                .eventTime(eventTime)
                .sourceType(SOURCE_CUT_ROUND_REPORT)
                .sourceRecordId(report.getId())
                .sliceBatchNo(report.getProductionBatchNo())
                .segmentBatchNo(firstNotBlank(report.getParentProductionBatchNo(), report.getSourceProductionBatchNo()))
                .planNo(report.getPlanNo())
                .materialCode(report.getMaterialCode())
                .materialName(report.getMaterialName())
                .modelCode(report.getModelCode())
                .fqcResult(normalizedResult)
                .qualityStatus("NG".equals(normalizedResult) ? "NG" : "OK")
                .afterStatus("WAIT_PACKAGING")
                .ngRelated(true)
                .operatorName(operatorName)
                .refDocNo(refDocNo)
                .remark(remark)
                .build());
    }

    @Override
    public void recordCutRoundEvent(HcCutRoundReportDO report, String eventType, String beforeStatus,
                                    String afterStatus, LocalDateTime eventTime, String operatorName,
                                    String refDocNo, String remark) {
        if (report == null || report.getId() == null || eventTime == null) {
            return;
        }
        String fqcResult = normalize(report.getInspectionResult());
        boolean ngRelated = "NG".equals(fqcResult)
                || hcPackagingPieceEventLogMapper.hasNgLifecycle(SOURCE_CUT_ROUND_REPORT, report.getId());
        if (!ngRelated) {
            return;
        }
        insertIfAbsent(HcPackagingPieceEventLogDO.builder()
                .tenantId(report.getTenantId())
                .eventType(eventType)
                .eventTime(eventTime)
                .sourceType(SOURCE_CUT_ROUND_REPORT)
                .sourceRecordId(report.getId())
                .sliceBatchNo(report.getProductionBatchNo())
                .segmentBatchNo(firstNotBlank(report.getParentProductionBatchNo(), report.getSourceProductionBatchNo()))
                .planNo(report.getPlanNo())
                .materialCode(report.getMaterialCode())
                .materialName(report.getMaterialName())
                .modelCode(report.getModelCode())
                .fqcResult(fqcResult)
                .qualityStatus("NG".equals(fqcResult) ? "NG" : "OK")
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .ngRelated(true)
                .operatorName(operatorName)
                .refDocNo(refDocNo)
                .remark(remark)
                .build());
    }

    @Override
    public void recordManualPieceEvent(HcPackagingManualPieceDO piece, String eventType, String beforeStatus,
                                       String afterStatus, LocalDateTime eventTime, String operatorName,
                                       String refDocNo, String remark) {
        if (piece == null || piece.getId() == null || eventTime == null) {
            return;
        }
        String qualityStatus = resolveQualityStatus(piece.getInspectionResult(), piece.getCoaInspectionResult());
        boolean ngRelated = "NG".equals(qualityStatus)
                || hcPackagingPieceEventLogMapper.hasNgLifecycle(SOURCE_MANUAL_HISTORY, piece.getId());
        if (!ngRelated) {
            return;
        }
        insertIfAbsent(HcPackagingPieceEventLogDO.builder()
                .tenantId(piece.getTenantId())
                .eventType(eventType)
                .eventTime(eventTime)
                .sourceType(SOURCE_MANUAL_HISTORY)
                .sourceRecordId(piece.getId())
                .sliceBatchNo(piece.getSliceBatchNo())
                .segmentBatchNo(piece.getSegmentBatchNo())
                .materialCode(piece.getMaterialCode())
                .materialName(piece.getMaterialName())
                .modelCode(piece.getModelCode())
                .fqcResult(normalize(piece.getInspectionResult()))
                .coaResult(normalize(piece.getCoaInspectionResult()))
                .qualityStatus(qualityStatus)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .ngRelated(true)
                .innerUnitNo(piece.getInnerUnitNo())
                .operatorName(operatorName)
                .refDocNo(refDocNo)
                .remark(remark)
                .build());
    }

    private void insertIfAbsent(HcPackagingPieceEventLogDO event) {
        String eventNo = "PKG-NG-" + event.getSourceType() + '-' + event.getSourceRecordId() + '-'
                + event.getEventType() + '-' + event.getEventTime().format(EVENT_TIME_FORMATTER) + '-'
                + normalize(event.getQualityStatus()) + '-' + normalize(event.getAfterStatus());
        if (hcPackagingPieceEventLogMapper.existsByEventNo(eventNo)) {
            return;
        }
        event.setEventNo(eventNo);
        hcPackagingPieceEventLogMapper.insert(event);
    }

    private String resolveQualityStatus(String fqcResult, String coaResult) {
        return "NG".equals(normalize(fqcResult)) || "NG".equals(normalize(coaResult)) ? "NG" : "OK";
    }

    private String normalize(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }
}
