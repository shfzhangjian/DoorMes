package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 量检具维护最新校准结果 Request VO")
@Data
public class QmsMeasureToolMaintainCalibrationReqVO {

    @NotNull(message = "量检具不能为空")
    private Long ledgerId;

    @NotNull(message = "校准日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate calibrationDate;

    @Pattern(regexp = "INTERNAL|EXTERNAL", message = "校准类型不合法")
    private String calibrationType;
    private String calibrationMethod;
    private String calibrationOrg;
    private String calibrator;

    @NotNull(message = "校准结果不能为空")
    @Pattern(regexp = "QUALIFIED|UNQUALIFIED|LIMITED", message = "校准结果不合法")
    private String calibrationResult;

    private String certificateNo;
    @Size(max = 1000, message = "校准报告不能超过1000个字符")
    private String calibrationReport;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
