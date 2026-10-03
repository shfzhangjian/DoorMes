package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS 8D CAPA行动项 Request VO")
@Data
public class Qms8dActionItemReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "行动项类型")
    private String actionType;

    @Schema(description = "行动内容")
    private String actionDesc;

    @Schema(description = "根因分类")
    private String rootCauseCategory;

    @Schema(description = "责任人ID")
    private Long ownerUserId;

    @Schema(description = "责任人名称")
    private String ownerUserName;

    @Schema(description = "责任部门ID")
    private Long ownerDeptId;

    @Schema(description = "责任部门名称")
    private String ownerDeptName;

    @Schema(description = "计划完成日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planFinishDate;

    @Schema(description = "实际完成日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualFinishDate;

    @Schema(description = "行动项状态")
    private String itemStatus;

    @Schema(description = "完成说明")
    private String finishDesc;

    @Schema(description = "验证结果")
    private String verificationResult;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
