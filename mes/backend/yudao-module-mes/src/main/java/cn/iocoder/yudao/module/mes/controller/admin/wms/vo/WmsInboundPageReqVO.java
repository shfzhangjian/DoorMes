// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.wms.vo.WmsInboundPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.wms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - WMS入库单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsInboundPageReqVO extends PageParam {

    @Schema(description = "入库单号")
    private String inboundNo;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "状态")
    private String status;

}
