package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 研发管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcResearchTaskRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "研发任务号")
    @ExcelProperty("研发任务号")
    private String taskNo;

    @Schema(description = "研发型号编码")
    @ExcelProperty("研发型号")
    private String rdModelCode;

    @Schema(description = "展示型号编码")
    private String displayModelCode;

    @Schema(description = "研发状态")
    @ExcelProperty("状态")
    private String taskStatus;

    @Schema(description = "研发日期")
    @ExcelProperty("研发日期")
    private LocalDate researchDate;

    @Schema(description = "任务下达人ID")
    private Long issueUserId;

    @Schema(description = "任务下达人")
    @ExcelProperty("任务下达人")
    private String issueUserName;

    @Schema(description = "下达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("下达时间")
    private LocalDateTime issueTime;

    @Schema(description = "型号类型编码")
    private String productClassCode;

    @Schema(description = "型号类型名称")
    @ExcelProperty("型号类型")
    private String productClassName;

    @Schema(description = "批次类型编码")
    private String batchTypeCode;

    @Schema(description = "基准配方编码")
    @ExcelProperty("基准配方")
    private String baseFormulaCode;

    @Schema(description = "基准配方名称")
    private String baseFormulaName;

    @Schema(description = "湿法工艺编码")
    @ExcelProperty("湿法工艺")
    private String wetProcessCode;

    @Schema(description = "湿法工艺名称")
    private String wetProcessName;

    @Schema(description = "磨皮工艺编码")
    @ExcelProperty("磨皮工艺")
    private String grindingProcessCode;

    @Schema(description = "磨皮工艺名称")
    private String grindingProcessName;

    @Schema(description = "后工艺编码")
    @ExcelProperty("后工艺")
    private String postProcessCode;

    @Schema(description = "后工艺名称")
    private String postProcessName;

    @Schema(description = "重复配方/组合使用序号")
    @ExcelProperty("重复序号")
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

    @Schema(description = "配方使用次数")
    private Integer formulaUsageCount;

    @Schema(description = "湿法使用次数")
    private Integer wetUsageCount;

    @Schema(description = "磨皮使用次数")
    private Integer grindingUsageCount;

    @Schema(description = "后工艺使用次数")
    private Integer postUsageCount;

    @Schema(description = "组合使用次数")
    private Integer combinationUsageCount;

    @Schema(description = "归档型号ID")
    private Long archivedModelId;

    @Schema(description = "归档型号编码")
    private String archivedModelCode;

    @Schema(description = "归档时间")
    private LocalDateTime archivedTime;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "快照 JSON")
    private String snapshotJson;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
