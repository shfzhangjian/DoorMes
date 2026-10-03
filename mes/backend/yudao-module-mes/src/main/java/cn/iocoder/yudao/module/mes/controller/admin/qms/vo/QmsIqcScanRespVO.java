package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IQC扫码解析 Response VO")
@Data
public class QmsIqcScanRespVO {

    @Schema(description = "扫码内容")
    private String scanCode;

    @Schema(description = "扫码对象类型")
    private String scanTargetType;

    @Schema(description = "扫码场景")
    private String scanScene;

    @Schema(description = "匹配结果")
    private String matchResult;

    @Schema(description = "打开目标：WORKBENCH/REPORT/CANDIDATE_MODAL")
    private String openTarget;

    @Schema(description = "阻断原因或提示")
    private String message;

    @Schema(description = "命中 IQC 主单 ID")
    private Long matchedIqcId;

    @Schema(description = "命中 IQC 单号")
    private String matchedIqcNo;

    @Schema(description = "候选数量")
    private Integer candidateCount;

    @Schema(description = "命中单据详情")
    private QmsIqcRespVO record;

    @Schema(description = "候选单列表")
    private List<Candidate> candidates;

    @Schema(description = "扫码时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;

    @Schema(description = "管理后台 - IQC扫码候选单")
    @Data
    public static class Candidate {
        private Long id;
        private String iqcNo;
        private String receiptNo;
        private String supplierName;
        private String materialCode;
        private String materialName;
        private String batchNo;
        private String status;
        private String judgment;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }
}
