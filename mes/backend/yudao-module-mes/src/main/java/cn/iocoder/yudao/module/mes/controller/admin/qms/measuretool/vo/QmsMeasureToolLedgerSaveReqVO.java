package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 量检具台账新增/修改 Request VO")
@Data
public class QmsMeasureToolLedgerSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "量检具编码")
    @Size(max = 64, message = "量检具编码不能超过64个字符")
    private String toolCode;

    @Schema(description = "机身号")
    @Size(max = 64, message = "机身号不能超过64个字符")
    private String bodyNo;

    @Schema(description = "量检具名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "量检具名称不能为空")
    @Size(max = 128, message = "量检具名称不能超过128个字符")
    private String toolName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "精度")
    private String accuracy;

    @Schema(description = "量程")
    private String measureRange;

    @Schema(description = "制造商/品牌")
    private String manufacturer;

    @Schema(description = "采购日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @Schema(description = "校准周期(月)")
    @Min(value = 1, message = "校准周期必须大于0")
    private Integer calibrationCycleMonths;

    @Schema(description = "提前预警天数")
    @Min(value = 0, message = "提前预警天数不能小于0")
    private Integer warningDays;

    @Schema(description = "校准类型")
    @Pattern(regexp = "INTERNAL|EXTERNAL", message = "校准类型不合法")
    private String calibrationType;

    @Schema(description = "最新校准方式")
    private String calibrationMethod;

    @Schema(description = "最新校准机构")
    private String calibrationOrg;

    @Schema(description = "最新校准人")
    private String calibrator;

    @Schema(description = "最新校准结果")
    @Pattern(regexp = "QUALIFIED|UNQUALIFIED|LIMITED", message = "校准结果不合法")
    private String calibrationResult;

    @Schema(description = "最新校准证书号")
    private String certificateNo;

    @Schema(description = "最新校准报告")
    @Size(max = 1000, message = "校准报告不能超过1000个字符")
    private String calibrationReport;

    @Schema(description = "上次校准日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastCalibrationDate;

    @Schema(description = "下次校准日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextCalibrationDate;

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "保养人")
    private String maintainerName;

    @Schema(description = "存放位置")
    private String storageLocation;

    @Schema(description = "量检具状态")
    @Pattern(regexp = "IN_USE|IDLE|CALIBRATING|REPAIRING|STOPPED|SCRAPPED", message = "量检具状态不合法")
    private String status;

    @Schema(description = "是否纳入MSA分析")
    private Integer msaEnabled;

    @Schema(description = "MSA周期(月)")
    @Min(value = 1, message = "MSA周期必须大于0")
    private Integer msaCycleMonths;

    @Schema(description = "MSA提前预警天数")
    @Min(value = 0, message = "MSA提前预警天数不能小于0")
    private Integer msaWarningDays;

    @Schema(description = "上次MSA分析日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMsaDate;

    @Schema(description = "下次MSA分析日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextMsaDate;

    @Schema(description = "最新MSA分析结果")
    @Pattern(regexp = "QUALIFIED|UNQUALIFIED", message = "MSA分析结果不合法")
    private String msaResult;

    @Schema(description = "最新MSA分析报告")
    @Size(max = 1000, message = "MSA分析报告不能超过1000个字符")
    private String msaReport;

    @Schema(description = "最新MSA分析人")
    private String msaAnalyst;

    @Schema(description = "责任人")
    private String responsiblePerson;

    @Schema(description = "是否对外开放：0否、1是")
    @Min(value = 0, message = "对外开放值不合法")
    @Max(value = 1, message = "对外开放值不合法")
    private Integer externalOpen;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Schema(description = "乐观锁")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
