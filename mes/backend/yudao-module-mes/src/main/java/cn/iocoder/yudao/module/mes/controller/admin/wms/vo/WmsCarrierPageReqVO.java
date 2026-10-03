// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.wms.vo.WmsCarrierPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.wms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 载具与周转箱台账分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsCarrierPageReqVO extends PageParam {

    @Schema(description = "载具/托盘条码")
    private String carrierCode;

    @Schema(description = "载具类型")
    private String carrierType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "当前物理位置")
    private String currentLocation;

}
