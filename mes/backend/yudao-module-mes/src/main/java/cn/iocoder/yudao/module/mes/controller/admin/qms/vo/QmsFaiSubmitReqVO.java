package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - FAI提交自检/复核 Request VO")
@Data
public class QmsFaiSubmitReqVO {

    @Schema(description = "FAI主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FAI主单ID不能为空")
    private Long id;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime inspectionTime;

    @Schema(description = "留样状态；字典:mes_fai_retention_status")
    private String retentionStatus;

    @Schema(description = "留样确认时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime retentionConfirmTime;

    @Schema(description = "留样确认人ID")
    private Long retentionConfirmUserId;

    @Schema(description = "留样确认人名称")
    private String retentionConfirmUserName;

    @Schema(description = "提交时覆盖的检验项明细")
    @Valid
    private List<QmsFaiSaveReqVO.FaiItem> items;

    @Schema(description = "提交时覆盖的异常/驳回记录")
    @Valid
    private List<QmsFaiSaveReqVO.FaiAbnormal> abnormals;
}
