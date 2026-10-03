package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 取消量检具校准预警任务 Request VO")
@Data
public class QmsMeasureToolCalibrationTaskCancelReqVO {

    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Size(max = 500, message = "取消原因不能超过500个字符")
    private String reason;

}
