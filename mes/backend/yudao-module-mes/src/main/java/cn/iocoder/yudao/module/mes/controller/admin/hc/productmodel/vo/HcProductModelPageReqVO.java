package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 产品型号字典分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProductModelPageReqVO extends PageParam {

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "规则名称")
    private String modelRuleName;

    @Schema(description = "生产类型编码")
    private String prodType;

    @Schema(description = "物料类型编码")
    private String categoryCode;

    @Schema(description = "尺寸编码")
    private String sizeSpec;

    @Schema(description = "状态")
    private String status;

}
