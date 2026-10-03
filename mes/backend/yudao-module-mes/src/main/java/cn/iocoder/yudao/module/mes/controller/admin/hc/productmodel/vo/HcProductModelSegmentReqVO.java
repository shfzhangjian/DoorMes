package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 产品型号规则段 Request VO")
@Data
public class HcProductModelSegmentReqVO {

    private Long id;
    private Long ruleItemId;
    private String itemCode;
    private String itemName;
    private String segmentValue;
    private String segmentText;
    private Long dictId;
    private Integer sort;

}
