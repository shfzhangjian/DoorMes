package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工序合格目标配置分页 Request VO")
@Data
public class QmsYieldTargetConfigPageReqVO extends PageParam {

    @Schema(description = "产品型号前缀")
    private String modelCode;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "母卷分段数")
    private Integer segmentCount;

    @Schema(description = "状态：0 启用，1 禁用")
    private Integer status;
}
