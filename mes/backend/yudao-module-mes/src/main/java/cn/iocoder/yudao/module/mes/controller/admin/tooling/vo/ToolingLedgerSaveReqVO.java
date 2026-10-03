// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.tooling.vo.ToolingLedgerSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.tooling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 工装模具台账保存 Request VO")
@Data
public class ToolingLedgerSaveReqVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "工装治具编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工装治具编号不能为空")
    private String toolingCode;

    @Schema(description = "治具名称")
    private String toolingName;

    @Schema(description = "最大使用寿命(次/小时)")
    private Integer maxLifeTimes;

    @Schema(description = "已使用寿命")
    private Integer usedLifeTimes;

    @Schema(description = "状态(IDLE空闲, IN_USE使用中, REPAIR修磨中, SCRAP报废)")
    private String status;

}
