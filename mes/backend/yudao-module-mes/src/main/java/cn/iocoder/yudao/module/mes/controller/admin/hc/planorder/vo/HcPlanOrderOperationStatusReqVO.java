package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 生产计划工序状态操作 Request VO")
@Data
public class HcPlanOrderOperationStatusReqVO {

    @Schema(description = "计划工序ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "计划工序不能为空")
    private List<Long> operationIds;

    @Schema(description = "状态动作：FINISH/PAUSE/RESUME/CANCEL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态动作不能为空")
    private String actionType;

    @Schema(description = "暂停范围：ALL/DATE_RANGE")
    private String pauseScope;

    @Schema(description = "暂停开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate pauseStartDate;

    @Schema(description = "暂停结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate pauseEndDate;

    @Schema(description = "离散状态影响日期列表")
    private List<LocalDate> statusDates;

    @Schema(description = "指定复工日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate resumeDate;

    @Schema(description = "完工时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime finishTime;

    @Schema(description = "原因/备注说明")
    private String reasonRemark;

}
