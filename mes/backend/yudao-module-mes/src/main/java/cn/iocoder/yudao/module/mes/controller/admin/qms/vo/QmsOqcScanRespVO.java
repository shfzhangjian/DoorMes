package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - OQC扫码解析 Response VO")
@Data
public class QmsOqcScanRespVO {

    @Schema(description = "扫码内容")
    private String scanCode;

    @Schema(description = "扫码对象类型")
    private String scanTargetType;

    @Schema(description = "扫码场景")
    private String scanScene;

    @Schema(description = "匹配结果")
    private String matchResult;

    @Schema(description = "打开目标：ENTRY/REPORT/CANDIDATE_MODAL")
    private String openTarget;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "命中 OQC 主单 ID")
    private Long matchedOqcId;

    @Schema(description = "命中 OQC 单号")
    private String matchedOqcNo;

    @Schema(description = "候选数量")
    private Integer candidateCount;

    @Schema(description = "命中单据详情")
    private QmsOqcRespVO record;

    @Schema(description = "候选列表")
    private List<Candidate> candidates;

    @Schema(description = "扫码时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;

    @Schema(description = "管理后台 - OQC扫码候选项")
    @Data
    public static class Candidate {
        private Long id;
        private String oqcNo;
        private Long shippingNoticeId;
        private Long shippingNoticeItemId;
        private String shippingNo;
        private String noticeNo;
        private String customerName;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String batchNo;
        private String customerBatchNo;
        private BigDecimal shippingQty;
        private String status;
        private String judgment;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastSaveTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }
}
