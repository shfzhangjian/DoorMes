package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务检验结果项目 Request VO")
@Data
public class QmsDispatchTaskResultItemReqVO {

    @NotNull(message = "任务检验项目不能为空")
    private Long taskItemId;
    private Long sampleResultId;
    private Integer sampleSeq;
    /** Actual piece number entered during task-owned inspection. */
    private String pieceNo;

    private BigDecimal measuredValue;

    private String qualitativeValue;

    @NotBlank(message = "项目判定不能为空")
    private String result;
}
