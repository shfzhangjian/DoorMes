// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.andon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 安灯呼叫保存 Request VO")
@Data
public class AndonRecordSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "安灯呼叫单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "呼叫单号不能为空")
    private String andonNo;

    @Schema(description = "类别(MATERIAL, QUALITY, EQUIPMENT, QTIME)")
    private String exceptionType;

    @Schema(description = "等级(LOW, MEDIUM, HIGH, CRITICAL)")
    private String severityLevel;

    @Schema(description = "故障机台ID")
    private Long equipmentId;

    @Schema(description = "关联派工细单ID")
    private Long subOrderId;

    @Schema(description = "状态(UNPROCESSED, PROCESSING, RECOVERED)")
    private String status;

    @Schema(description = "响应处理人")
    private String handlerUser;

    @Schema(description = "AI智能辅助归因与排故建议预留 (JSON)")
    private Map<String, Object> aiAnalysisResult; // 🚨 架构师红线：强约束 Map 承接 JSON

}
