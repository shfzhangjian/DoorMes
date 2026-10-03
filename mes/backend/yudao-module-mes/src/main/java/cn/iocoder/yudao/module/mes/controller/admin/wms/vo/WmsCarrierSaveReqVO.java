// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.wms.vo.WmsCarrierSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 载具与周转箱台账保存 Request VO")
@Data
public class WmsCarrierSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "载具/托盘条码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "载具条码不能为空")
    private String carrierCode;

    @Schema(description = "载具类型(PALLET托盘, BOX周转箱)")
    private String carrierType;

    @Schema(description = "状态(EMPTY空闲, OCCUPIED被占用, MAINTENANCE维修中)")
    private String status;

    @Schema(description = "当前物理位置(工位/库区)")
    private String currentLocation;

}
