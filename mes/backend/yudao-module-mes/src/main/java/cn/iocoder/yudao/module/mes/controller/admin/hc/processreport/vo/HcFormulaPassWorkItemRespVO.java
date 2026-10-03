package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 配料过站工作明细 Response VO")
@Data
public class HcFormulaPassWorkItemRespVO {

    private Boolean requiredFlag;
    private Boolean explicitValueMode;
    private Integer itemSeq;
    private String category;
    private String node;
    private String item;
    private String standard;
    private String valueMode;
    private String dualLabel1;
    private String dualLabel2;

    /** 多字段定义快照，按稳定 key 对应实际值。 */
    private String fieldDefinitionsJson;
    private String fieldValuesJson;
    private String actualValue;
    private String actualValue2;
    private String status;
    private String remark;
}
