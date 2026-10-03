package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS 8D报告保存 Request VO")
@Data
public class Qms8dReportSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "8D报告号")
    private String reportNo;

    @Schema(description = "来源类型")
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "来源对象ID")
    private Long sourceId;

    @Schema(description = "来源单号")
    @NotBlank(message = "来源单号不能为空")
    private String sourceNo;

    @Schema(description = "立案日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @Schema(description = "要求结案日")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @Schema(description = "当前阶段")
    private String currentStep;

    @Schema(description = "当前处理人ID")
    private Long currentHandlerUserId;

    @Schema(description = "当前处理人名称")
    private String currentHandlerUserName;

    @Schema(description = "问题描述")
    @NotBlank(message = "问题描述不能为空")
    private String problemDesc;

    private String containmentAction;
    private Long containmentOwnerId;
    private String containmentOwnerName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate containmentDate;

    private String rootCauseCategory;
    private String rootCauseAnalysis;
    private String correctiveAction;
    private Long actionOwnerId;
    private String actionOwnerName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actionPlanDate;

    private String validationResult;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validationDate;

    private Boolean updateSop;
    private Boolean updateFmea;
    private Boolean updateControlPlan;
    private String standardizeDesc;
    private List<Qms8dTeamMemberReqVO> teamMembers;
    private List<Qms8dActionItemReqVO> actionItems;

    @Schema(description = "关系清单（附件等）")
    private List<Qms8dRelationReqVO> relations;
}
