package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 粘双面胶点检明细 Request VO")
@Data
public class HcAdhesiveCheckItemReqVO {

    private String itemCategory;
    private String itemName;
    private String standardValue;
    private String actualValue;
    private String checkResult;
    private String abnormalRemark;
    private Integer sortNo;
}
