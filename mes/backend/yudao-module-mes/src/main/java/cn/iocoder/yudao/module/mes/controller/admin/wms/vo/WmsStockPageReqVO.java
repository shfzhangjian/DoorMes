// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsStockPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.wms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - WMS实时库存分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsStockPageReqVO extends PageParam {

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "质量状态")
    private String qualityStatus;

}
