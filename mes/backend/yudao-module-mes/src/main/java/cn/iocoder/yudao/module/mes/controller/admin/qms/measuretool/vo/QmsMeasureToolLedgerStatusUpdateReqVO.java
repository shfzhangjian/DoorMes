package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 量检具台账状态调整 Request VO")
@Data
public class QmsMeasureToolLedgerStatusUpdateReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主键ID不能为空")
    private Long id;

    @Schema(description = "乐观锁版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "版本不能为空")
    private Integer version;

    @Schema(description = "量检具状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "量检具状态不能为空")
    @Pattern(regexp = "IN_USE|CALIBRATING|REPAIRING|STOPPED|SCRAPPED", message = "量检具状态不合法")
    private String status;

    @Schema(description = "处理人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理人不能为空")
    @Size(max = 64, message = "处理人不能超过64个字符")
    private String handler;

    @Schema(description = "处理时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "处理时间不能为空")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime handleTime;

    @Schema(description = "OA处理单号")
    @Size(max = 128, message = "OA处理单号不能超过128个字符")
    private String oaProcessNo;

    @Schema(description = "处理说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理说明不能为空")
    @Size(max = 500, message = "处理说明不能超过500个字符")
    private String handleRemark;

    @Schema(description = "附件，多个URL以英文逗号分隔")
    @Size(max = 1000, message = "附件地址不能超过1000个字符")
    private String attachments;

}
