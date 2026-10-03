package cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工艺路线 Detail Response VO")
@Data
public class HcRouteDetailRespVO extends HcRouteRespVO {

    @Schema(description = "路线工序列表")
    private List<HcRouteOperationDO> routeOperations;

}