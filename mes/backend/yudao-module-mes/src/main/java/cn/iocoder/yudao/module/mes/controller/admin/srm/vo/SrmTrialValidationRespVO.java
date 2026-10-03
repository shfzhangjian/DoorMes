package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmTrialValidationRespVO {

    private Long id;
    private String trialNo;
    private Long sourceSampleEvaluationId;
    private String sourceSampleEvaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialModel;
    private String materialBatchNo;
    private BigDecimal quantity;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long initiatorUserId;
    private String initiatorUserName;
    private Long trialExecutionUserId;
    private String trialExecutionUserName;
    private String trialExecutionOpinion;
    private Long productionCompleteUserId;
    private String productionCompleteUserName;
    private String productionCompleteOpinion;
    private Long archiveUserId;
    private String archiveUserName;
    private String archiveOpinion;
    private String remark;
    private Integer version;
    private List<Log> logs;
    private Boolean canTrialExecute;
    private Boolean canCompleteProduction;
    private Boolean canArchiveConfirm;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime noticeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime trialExecutionTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime productionCompleteTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime archiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Log {
        private Long id;
        private String action;
        private String actionName;
        private String fromStatus;
        private String toStatus;
        private Long operatorId;
        private String operatorName;
        private String actionDescription;
        private String detailJson;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }

}
