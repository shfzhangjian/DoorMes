package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验片级判定保存 Request VO")
@Data
public class QmsCutRoundFqcSubmissionDetailSaveReqVO {

    @Schema(description = "FQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FQC主单ID不能为空")
    private Long fqcId;

    @NotEmpty(message = "送检明细不能为空")
    private List<@Valid Detail> details;

    @Data
    public static class Detail {

        @NotNull(message = "送检明细ID不能为空")
        private Long id;

        private String rowJudgment;
        private String defectCode;
        private String defectName;
        private String ngReason;
        private String remark;
    }
}
