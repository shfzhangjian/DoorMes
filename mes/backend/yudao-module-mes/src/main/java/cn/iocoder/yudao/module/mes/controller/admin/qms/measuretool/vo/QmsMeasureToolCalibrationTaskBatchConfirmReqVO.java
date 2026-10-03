package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 批量确认量检具校准任务 Request VO")
@Data
public class QmsMeasureToolCalibrationTaskBatchConfirmReqVO {

    @Schema(description = "待确认校准任务ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择待确认校准任务")
    private List<Long> ids;

}
