package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验扫码解析 Response VO")
@Data
public class QmsCutRoundFqcScanRespVO {

    private String scanCode;
    private String scanTargetType;
    private String matchResult;
    private String message;
    private String openTarget;
    /** 候选数量；扫描父批次、计划号或报检任务时可能大于 1。 */
    private Integer candidateCount;
    private QmsCutRoundFqcRespVO record;
    private QmsCutRoundFqcRespVO.SubmissionDetail detail;
    private List<ScanCandidate> candidates;

    @Data
    public static class ScanCandidate {

        private Long fqcId;
        private String fqcNo;
        private String fqcStatus;
        private Long submissionDetailId;
        private String cutRoundInspectionTaskNo;
        private String planNo;
        private String productionBatchNo;
        private String parentProductionBatchNo;
        private String materialCode;
        private String modelCode;
    }
}
