package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialExtAttrDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 物料主数据 Detail Response VO")
@Data
public class HcMaterialDetailRespVO extends HcMaterialRespVO {

    @Schema(description = "物料扩展属性列表")
    private List<HcMaterialExtAttrDO> materialExtAttrs;

}