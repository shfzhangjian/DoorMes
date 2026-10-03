package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FAI扫码解析 Response VO")
@Data
public class QmsFaiScanRespVO {

    @Schema(description = "扫码记录 ID")
    private Long scanRecordId;

    @Schema(description = "扫码内容")
    private String scanCode;

    @Schema(description = "扫码对象类型")
    private String scanTargetType;

    @Schema(description = "扫码场景")
    private String scanScene;

    @Schema(description = "匹配结果")
    private String matchResult;

    @Schema(description = "打开目标：WORKBENCH/ITEM_MODAL/REPORT/CANDIDATE_MODAL")
    private String openTarget;

    @Schema(description = "阻断原因或提示")
    private String message;

    @Schema(description = "命中 FAI 主单 ID")
    private Long matchedFaiId;

    @Schema(description = "命中 FAI 单号")
    private String matchedFaiNo;

    @Schema(description = "命中 FAI 检验项 ID")
    private Long matchedFaiItemId;

    @Schema(description = "命中步骤编码")
    private String matchedStepCode;

    @Schema(description = "候选数量")
    private Integer candidateCount;

    @Schema(description = "命中单据详情")
    private QmsFaiRespVO record;

    @Schema(description = "候选单列表")
    private List<Candidate> candidates;

    @Schema(description = "扫码时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;

    @Schema(description = "管理后台 - FAI扫码候选单")
    @Data
    public static class Candidate {
        private Long id;
        private String faiNo;
        private String workOrderNo;
        private String productModel;
        private String productBatchNo;
        private String machineCode;
        private String status;
        private String judgment;
        private String currentStepCode;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastSaveTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }
}
