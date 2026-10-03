package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 产品型号适用物料 Request VO")
@Data
public class HcProductModelMaterialReqVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Boolean isDefault;
    private String remark;

}
