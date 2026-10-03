package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS异常事件关闭 Request VO")
@Data
public class QmsExceptionEventCloseReqVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long id;

    @Schema(description = "效果确认", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "效果确认不能为空")
    private String effectConfirm;

    @Schema(description = "品质确认人ID")
    private Long qaConfirmerId;

    @Schema(description = "品质确认人")
    private String qaConfirmerName;

    @Schema(description = "品质闭环确认是否有效")
    private Boolean qaConfirmValid;

    @Schema(description = "完成/关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime finishTime;

    @Schema(description = "关闭意见")
    private String opinion;

    @Schema(description = "关联对象/附件")
    private List<QmsExceptionRelationReqVO> relations;
}
