// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - SOP动作定义保存 Request VO")
@Data
public class RouteProcessActionSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联工艺路线工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联工艺路线工序ID不能为空")
    private Long routeProcessId;

    @Schema(description = "动作代码 (PRINT, SCAN, IOT)")
    private String actionCode;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "触发时机: PRE_CHECK, DURING_PROCESS, POST_CHECK")
    private String triggerMoment;

    @Schema(description = "是否强制执行(true:必填/必做, false:选填)")
    private Boolean mandatory; // 架构师红线：无 is_ 前缀

    @Schema(description = "异常防呆策略: BLOCK(阻断), WARN(警告), RECORD(仅记录)")
    private String errorStrategy;

    @Schema(description = "执行顺序")
    private Integer sort;

    @Schema(description = "动作参数配置 (JSON)")
    private Map<String, Object> actionConfig; // 架构师红线：强类型 Map 接 JSON

    @Schema(description = "数据回写映射 (JSON)")
    private Map<String, Object> dataMapping; // 架构师红线：强类型 Map 接 JSON

    @Schema(description = "是否阻塞流程")
    private Boolean blocking; // 架构师红线：无 is_ 前缀

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态 (0:启用, 1:禁用)")
    private Integer status;

}
