package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 缺陷代码列表 Request VO")
@Data
public class QmsDefectCodeListReqVO {

    @Schema(description = "名称或代码")
    private String name;

    @Schema(description = "节点类型")
    private String type;

    @Schema(description = "状态")
    private Integer status;
}
