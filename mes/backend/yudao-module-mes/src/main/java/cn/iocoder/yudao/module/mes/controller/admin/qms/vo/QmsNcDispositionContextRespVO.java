package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR 终审处置范围上下文 Response VO")
@Data
public class QmsNcDispositionContextRespVO {

    private Long ncRecordId;
    private String ncNo;
    private String finalDisposition;
    private String finalDispositionName;
    private String ngProcessCode;
    private String ngProcessName;
    private String targetWorkstationCode;
    private String targetWorkstationName;
    private String defaultScopeLevel;
    private List<String> availableScopeLevels;
    private BigDecimal affectedQty;
    private String sourceLotNo;
    private String candidateSourceDescription;
    private String emptyReason;
    private Boolean pickQualification;
    private List<ScopeCandidate> candidates;
    private QmsNcDispositionExecutionRespVO existingExecution;

    @Data
    public static class ScopeCandidate {

        private String objectKey;
        private String scopeLevel;
        private String scopeLevelName;
        private String label;
        private String motherBatchNo;
        private String segmentBatchNo;
        private String pieceNo;
        private String sourceObjectType;
        private Long sourceObjectId;
        private String sourceObjectNo;
        private BigDecimal quantity;
        private String quantityUnit;
        private BigDecimal productionLength;
        private String currentStatus;
        private String currentStatusName;
        private Boolean selectable;
    }
}
