package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceActualReportRespVO {

    private Long id;
    private String reportNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private String periodType;
    private Integer evalYear;
    private Integer evalQuarter;
    private Integer evalMonth;
    private String status;
    private Long reporterUserId;
    private String reporterUserName;
    private Long confirmUserId;
    private String confirmUserName;
    private String confirmOpinion;
    private String remark;
    private Integer version;
    private Boolean canEdit;
    private Boolean canSubmit;
    private Boolean canConfirm;
    private List<Value> values;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Value {
        private Long id;
        private Long reportId;
        private String metricCode;
        private String metricName;
        private String valueType;
        private BigDecimal numericValue;
        private String textValue;
        private String unit;
        private String sourceNodeKey;
        private String sourceIndicatorCode;
        private String sourceIndicatorName;
        private String calcDescription;
        private String formulaExpr;
        private String scoreFormulaExpr;
        private Boolean evidenceRequired;
        private String valueStatus;
        private String remark;
    }

}
