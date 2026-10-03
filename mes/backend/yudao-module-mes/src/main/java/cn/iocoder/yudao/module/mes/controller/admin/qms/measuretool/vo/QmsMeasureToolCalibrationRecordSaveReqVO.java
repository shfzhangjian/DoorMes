package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 量检具校准记录新增/修改 Request VO")
@Data
public class QmsMeasureToolCalibrationRecordSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "关联校准任务ID")
    private Long taskId;

    @Schema(description = "量检具台账ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "量检具不能为空")
    private Long ledgerId;

    @Schema(description = "校准日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "校准日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate calibrationDate;

    @Schema(description = "校准方式")
    private String calibrationMethod;

    @Schema(description = "校准机构")
    private String calibrationOrg;

    @Schema(description = "校准人")
    private String calibrator;

    @Schema(description = "校准结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "校准结果不能为空")
    @Pattern(regexp = "QUALIFIED|UNQUALIFIED|LIMITED", message = "校准结果不合法")
    private String calibrationResult;

    @Schema(description = "证书编号")
    private String certificateNo;

    @Schema(description = "证书附件")
    @Size(max = 500, message = "证书附件不能超过500个字符")
    private String certificateAttachment;

    @Schema(description = "有效期至")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;

    @Schema(description = "下次校准日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextCalibrationDate;

    @Schema(description = "校准费用")
    private BigDecimal cost;

    @Schema(description = "来源类型")
    @Pattern(regexp = "TASK|MANUAL|IMPORT", message = "来源类型不合法")
    private String sourceType;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Schema(description = "乐观锁")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
