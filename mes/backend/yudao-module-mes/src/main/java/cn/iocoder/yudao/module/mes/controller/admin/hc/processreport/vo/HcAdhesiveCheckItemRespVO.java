package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 粘双面胶点检明细 Response VO")
@Data
public class HcAdhesiveCheckItemRespVO {

    private Long id;
    private String itemCategory;
    private String itemName;
    private String standardValue;
    private String actualValue;
    private String checkResult;
    private String abnormalRemark;
    private String valueMode;
    private String dualLabel1;
    private String dualLabel2;
    private Boolean requiredFlag;
    private Integer sortNo;
}
