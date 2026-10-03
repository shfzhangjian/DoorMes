// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - SOP动作定义分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteProcessActionPageReqVO extends PageParam {

    @Schema(description = "关联工艺路线工序ID")
    private Long routeProcessId;

    @Schema(description = "动作代码")
    private String actionCode;

    @Schema(description = "动作名称(模糊匹配)")
    private String actionName;

    @Schema(description = "状态")
    private Integer status;

}
