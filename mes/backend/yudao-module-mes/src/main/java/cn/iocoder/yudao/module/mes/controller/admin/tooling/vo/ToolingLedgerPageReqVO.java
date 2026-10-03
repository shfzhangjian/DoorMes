// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.tooling.vo.ToolingLedgerPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.tooling.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 工装模具台账分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolingLedgerPageReqVO extends PageParam {

    @Schema(description = "工装治具编号")
    private String toolingCode;

    @Schema(description = "治具名称")
    private String toolingName;

    @Schema(description = "状态")
    private String status;

}
