// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.job.vo.JobActionHistorySaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.job.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "移动端 - SOP作业执行历史保存 Request VO")
@Data
public class JobActionHistorySaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联派工细单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "派工细单ID不能为空")
    private Long subOrderId;

    @Schema(description = "关联SOP动作ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联SOP动作ID不能为空")
    private Long actionId;

    @Schema(description = "打卡执行人")
    private String operatorUser;

    @Schema(description = "实际执行时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime executeTime;

    @Schema(description = "实际采集结果(读数/扫码值)")
    private String collectValue;

    @Schema(description = "合规标识(true:合规, false:异常)")
    private Boolean compliant; // 架构师红线：杜绝 isCompliant

}
