// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.route.vo.QtimeRulePageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 工艺Q-Time约束规则分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QtimeRulePageReqVO extends PageParam {

    @Schema(description = "关联工艺路线ID")
    private Long routeId;

    @Schema(description = "起始工序ID")
    private Long fromProcessId;

    @Schema(description = "目标工序ID")
    private Long toProcessId;

    @Schema(description = "约束(MAX_STAY最大停滞, MIN_WAIT最小静置)")
    private String constraintType;

}
