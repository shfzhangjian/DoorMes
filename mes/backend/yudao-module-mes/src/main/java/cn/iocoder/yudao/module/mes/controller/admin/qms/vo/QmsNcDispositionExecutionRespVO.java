package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR 处置执行单 Response VO")
@Data
public class QmsNcDispositionExecutionRespVO {

    private Long id;
    private String executionNo;
    private Long ncRecordId;
    private String ncNo;
    private String dispositionType;
    private String ngProcessCode;
    private String ngProcessName;
    private String targetWorkstationCode;
    private String targetWorkstationName;
    private String scopeLevel;
    private BigDecimal affectedQty;
    private BigDecimal selectedQty;
    private BigDecimal derivedScrapQty;
    private String executionStatus;
    private Long confirmUserId;
    private String confirmUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private Long executionUserId;
    private String executionUserName;
    private String remark;
    private String commandNo;
    private String commandStatus;
    private List<ScopeItem> scopes;

    @Data
    public static class ScopeItem {

        private String objectKey;
        private String scopeLevel;
        private String dispositionType;
        private String motherBatchNo;
        private String segmentBatchNo;
        private String pieceNo;
        private String scopeRole;
        private BigDecimal quantity;
        private String executionResult;
        private String remark;
    }
}
