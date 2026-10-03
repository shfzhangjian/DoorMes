package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - BOM Detail Response VO")
@Data
public class HcBomDetailRespVO extends HcBomRespVO {

    @Schema(description = "BOM明细列表")
    private List<HcBomItemDO> bomItems;

}