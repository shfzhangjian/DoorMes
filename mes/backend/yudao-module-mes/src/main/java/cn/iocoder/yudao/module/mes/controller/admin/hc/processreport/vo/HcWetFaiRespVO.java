package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工首检摘要 Response VO")
@Data
public class HcWetFaiRespVO {

    @Schema(description = "FAI 主单 ID")
    private Long faiId;

    @Schema(description = "FAI 首检单号")
    private String faiNo;

    @Schema(description = "FAI 单据状态；字典:mes_fai_status")
    private String faiStatus;

    @Schema(description = "FAI 判定结果；字典:mes_qms_judgment_result")
    private String faiJudgment;

    @Schema(description = "FAI 触发原因；PROCESS_CHECK_NG_RESTART=过程加检NG后重新首检；ADDITIONAL_FIRST_INSPECTION=追加普通首检")
    private String triggerReason;

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

    @Schema(description = "检验场景；PROCESS_CHECK=过程加检")
    private String inspectionScene;

    @Schema(description = "来源报工ID")
    private Long sourceReportId;

    @Schema(description = "来源单号")
    private String sourceReportNo;

    @Schema(description = "产品批次号")
    private String productBatchNo;

    @Schema(description = "湿法 NAP层送检米数")
    private BigDecimal napSampleLength;

    @Schema(description = "最近首检驳回或 NG 说明")
    private String faiRejectReason;

    @Schema(description = "首检管控日期；压槽按当天+母卷批号判定")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate inspectionDate;

    @Schema(description = "首检管控母卷批号；压槽按当天+母卷批号判定")
    private String inspectionScopeBatchNo;

    @Schema(description = "当天当前母卷是否已有首检记录")
    private Boolean todayInspected;

    @Schema(description = "压槽当前首检管控范围内是否存在已完成且判定合格的首检单")
    private Boolean overallQualified;

    @Schema(description = "压槽NG后是否需要新片重新首检，送检成功即恢复报工")
    private Boolean restartRequired;

    @Schema(description = "湿法显示文案")
    private String displayText;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否允许湿法完工出站")
    private Boolean allowReportSubmit;

    @Schema(description = "是否因压槽当天非连续生产/换型需要重新首检")
    private Boolean requiresReinspection;

    @Schema(description = "重新首检原因")
    private String reinspectionReason;

    @Schema(description = "同一湿法报工来源下的首检单记录，按生成顺序升序返回")
    private List<HcWetFaiRecordRespVO> records;
}
