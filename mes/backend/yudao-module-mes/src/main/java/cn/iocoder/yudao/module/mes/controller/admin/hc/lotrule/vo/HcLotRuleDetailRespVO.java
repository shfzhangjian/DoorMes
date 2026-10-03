package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则 Detail Response VO")
@Data
public class HcLotRuleDetailRespVO extends HcLotRuleRespVO {

    @Schema(description = "批号规则分段列表")
    private List<HcLotRuleSegmentDO> lotRuleSegments;

}