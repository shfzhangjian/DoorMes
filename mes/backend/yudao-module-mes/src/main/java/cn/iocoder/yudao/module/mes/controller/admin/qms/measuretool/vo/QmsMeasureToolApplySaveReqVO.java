package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 量检具新增申请新增/修改 Request VO")
@Data
public class QmsMeasureToolApplySaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "量检具名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "量检具名称不能为空")
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

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "保管人")
    private String keeperName;

    @Schema(description = "存放位置")
    private String storageLocation;

    @Schema(description = "申请部门")
    private String applyDepartment;

    @Schema(description = "申请人")
    private String applicantName;

    @Schema(description = "申请原因")
    @Size(max = 500, message = "申请原因不能超过500个字符")
    private String applyReason;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Schema(description = "乐观锁")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
