package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 包装段 COA 送检接口 VO。 */
public final class QmsPackagingCoaInspectionVO {

    private QmsPackagingCoaInspectionVO() {
    }

    @Schema(description = "管理后台 - 包装段 COA 送检 Request VO")
    @Data
    public static class SubmitReqVO {

        @Schema(description = "段批次", requiredMode = Schema.RequiredMode.REQUIRED, example = "HC20260819P")
        @NotBlank(message = "段批次不能为空")
        private String segmentBatchNo;

        @Schema(description = "样片列表，允许同一段批次一次送多片", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "请至少选择一片 COA 样片")
        private List<@Valid SampleReqVO> samples;

        @Schema(description = "送检备注")
        private String remark;
    }

    @Schema(description = "管理后台 - 包装段 COA 样片 Request VO")
    @Data
    public static class SampleReqVO {

        @Schema(description = "来源类型：CUT_ROUND_REPORT / MANUAL_HISTORY", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "样片来源类型不能为空")
        private String sourceType;

        @Schema(description = "来源记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样片来源记录不能为空")
        private Long sourceRecordId;
    }

    @Schema(description = "管理后台 - 包装段 COA 送检 Response VO")
    @Data
    public static class SubmitRespVO {

        @Schema(description = "FAI 首件检验单 ID")
        private Long faiId;

        @Schema(description = "FAI 首件检验单号")
        private String faiNo;

        @Schema(description = "本次创建的 FAI 首件检验单 ID 列表，每片样品对应一张单")
        private List<Long> faiIds;

        @Schema(description = "本次创建的 FAI 首件检验单号列表，每片样品对应一张单")
        private List<String> faiNos;

        @Schema(description = "段批次")
        private String segmentBatchNo;

        @Schema(description = "已送检样片数量")
        private Integer sampleCount;

        @Schema(description = "FAI 当前状态")
        private String faiStatus;

        @Schema(description = "FAI 当前判定")
        private String faiJudgment;
    }

    @Schema(description = "管理后台 - 包装段 COA 送检记录 Response VO")
    @Data
    public static class FaiRecordRespVO extends QmsFaiRespVO {

        @Schema(description = "COA 送检具体样片片号")
        private String coaSampleBatchNo;
    }
}
