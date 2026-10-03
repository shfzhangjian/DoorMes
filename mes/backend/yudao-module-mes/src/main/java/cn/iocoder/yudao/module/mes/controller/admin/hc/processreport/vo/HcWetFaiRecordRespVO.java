package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工首检记录 Response VO")
@Data
public class HcWetFaiRecordRespVO {

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

    @Schema(description = "产品批次号")
    private String productBatchNo;

    @Schema(description = "湿法 NAP层送检米数")
    private BigDecimal napSampleLength;

    @Schema(description = "最近首检驳回或 NG 说明")
    private String faiRejectReason;

    @Schema(description = "湿法显示文案")
    private String displayText;

    @Schema(description = "是否允许湿法完工出站")
    private Boolean allowReportSubmit;

    @Schema(description = "是否当前用于湿法放行判断的最新单")
    @JsonProperty("isCurrent")
    private Boolean current;
}
