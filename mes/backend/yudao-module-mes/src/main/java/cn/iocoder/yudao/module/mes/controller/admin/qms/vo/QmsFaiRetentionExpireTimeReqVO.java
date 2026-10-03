package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - FAI首件留样过期时间设置 Request VO")
@Data
public class QmsFaiRetentionExpireTimeReqVO {

    @Schema(description = "FAI首件检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "留样记录ID不能为空")
    private Long id;

    @Schema(description = "留样过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "留样过期时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime retentionExpireTime;

}
