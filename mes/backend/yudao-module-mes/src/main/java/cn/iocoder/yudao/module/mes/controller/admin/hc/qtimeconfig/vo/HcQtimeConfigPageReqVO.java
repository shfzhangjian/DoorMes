package cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HC 额定 QTIME 配置分页 Request VO")
@Data
public class HcQtimeConfigPageReqVO extends PageParam {

    @Schema(description = "型号/批号前缀")
    private String modelPrefix;

    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;

}
