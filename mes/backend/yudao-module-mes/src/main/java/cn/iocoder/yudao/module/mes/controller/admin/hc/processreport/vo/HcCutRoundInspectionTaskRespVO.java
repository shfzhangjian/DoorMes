package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切产品报检单 Response VO")
@Data
public class HcCutRoundInspectionTaskRespVO {

    private Long id;
    private String taskNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String reportProcess;
    private String receiveLocation;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;
    private String reporterName;
    private String receiverName;
    private String priorityLevel;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate expectedFinishDate;
    private String taskStatus;
    private Long fqcOrderId;
    private String fqcNo;
    private String fqcStatus;
    private String fqcJudgment;
    private Integer detailCount;
    private String remark;
    private List<Detail> details;

    @Data
    public static class Detail {

        private Long id;
        private Long taskId;
        private Long cutRoundReportId;
        private Integer seqNo;
        private String parentProductionBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String sizeRule;
        private String productionBatchNo;
        private String qualityRiskFlag;
        private String qualityRiskSnapshotJson;
        private Long fqcOrderId;
        private String fqcNo;
        private String fqcStatus;
        private String fqcJudgment;
        private String inspectionResult;
        private String inspectorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
        private String remark;
    }
}
