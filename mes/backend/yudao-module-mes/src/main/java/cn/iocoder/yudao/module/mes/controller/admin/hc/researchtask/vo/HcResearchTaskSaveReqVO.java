package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 研发管理新增/修改 Request VO")
@Data
public class HcResearchTaskSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "研发任务号")
    private String taskNo;

    @Schema(description = "研发型号编码；后端按规则生成")
    private String rdModelCode;

    @Schema(description = "研发状态")
    private String taskStatus;

    @Schema(description = "研发日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate researchDate;

    @Schema(description = "任务下达人ID")
    private Long issueUserId;

    @Schema(description = "任务下达人")
    private String issueUserName;

    @Schema(description = "下达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime issueTime;

    @Schema(description = "型号类型编码：RD黑垫、RA黑垫", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "型号类型不能为空")
    private String productClassCode;

    @Schema(description = "型号类型名称")
    private String productClassName;

    @Schema(description = "批次类型编码：C黑垫、W白垫")
    private String batchTypeCode;

    @Schema(description = "基准配方编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "基准配方编码不能为空")
    private String baseFormulaCode;

    @Schema(description = "基准配方名称")
    private String baseFormulaName;

    @Schema(description = "湿法工艺编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "湿法工艺编码不能为空")
    private String wetProcessCode;

    @Schema(description = "湿法工艺名称")
    private String wetProcessName;

    @Schema(description = "磨皮工艺编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "磨皮工艺编码不能为空")
    private String grindingProcessCode;

    @Schema(description = "磨皮工艺名称")
    private String grindingProcessName;

    @Schema(description = "后工艺编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "后工艺编码不能为空")
    private String postProcessCode;

    @Schema(description = "后工艺名称")
    private String postProcessName;

    @Schema(description = "重复配方/组合使用序号；后端按历史次数生成")
    private Integer reuseSeq;

    @Schema(description = "工艺路线ID")
    private Long routeId;

    @Schema(description = "工艺路线编码")
    private String routeCode;

    @Schema(description = "工艺路线名称")
    private String routeName;

    @Schema(description = "工艺路线版本")
    private String routeVersion;

    @Schema(description = "研发批次规则编码")
    private String batchRuleCode;

    @Schema(description = "目标数量")
    private BigDecimal targetQty;

    @Schema(description = "目标单位")
    private String targetUom;

    @Schema(description = "研发目的")
    private String taskPurpose;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "工艺路线工序快照")
    private List<HcResearchTaskRouteOperationReqVO> routeOperations;

}
