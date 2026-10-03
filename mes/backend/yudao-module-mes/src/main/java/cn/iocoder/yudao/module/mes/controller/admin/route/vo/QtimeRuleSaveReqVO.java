// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.route.vo.QtimeRuleSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 工艺Q-Time约束规则保存 Request VO")
@Data
public class QtimeRuleSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线ID不能为空")
    private Long routeId;

    @Schema(description = "起始工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "起始工序ID不能为空")
    private Long fromProcessId;

    @Schema(description = "目标工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标工序ID不能为空")
    private Long toProcessId;

    @Schema(description = "约束(MAX_STAY最大停滞, MIN_WAIT最小静置)")
    private String constraintType;

    @Schema(description = "时间阈值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时间阈值不能为空")
    private BigDecimal thresholdValue; // 🚨 架构师红线：强约束 BigDecimal

    @Schema(description = "时间单位(MINUTE, HOUR, DAY)")
    private String timeUnit;

    @Schema(description = "违规动作(BLOCK拦截, SCRAP报废, ALARM预警)")
    private String violationAction;

}
