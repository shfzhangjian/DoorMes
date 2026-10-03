package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮操作看板首检摘要 Response VO")
@Data
public class HcRoughFaiRespVO {

    @Schema(description = "FAI 主单 ID")
    private Long faiId;

    @Schema(description = "FAI 首检单号")
    private String faiNo;

    @Schema(description = "FAI 单据状态；字典:mes_fai_status")
    private String faiStatus;

    @Schema(description = "FAI 判定结果；字典:mes_qms_judgment_result")
    private String faiJudgment;

    @Schema(description = "命中的检验标准 ID")
    private Long faiStandardId;

    @Schema(description = "命中的检验标准编号")
    private String faiStandardNo;

    @Schema(description = "命中的检验标准版本")
    private String faiStandardVersion;

    @Schema(description = "提交首检申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiApplyTime;

    @Schema(description = "FAI 最近状态回写时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiReturnTime;

    @Schema(description = "最近首检驳回或 NG 说明")
    private String faiRejectReason;

    @Schema(description = "是否允许提交磨皮完工")
    private Boolean allowReportSubmit;

    @Schema(description = "磨皮显示文案")
    private String displayText;
}
