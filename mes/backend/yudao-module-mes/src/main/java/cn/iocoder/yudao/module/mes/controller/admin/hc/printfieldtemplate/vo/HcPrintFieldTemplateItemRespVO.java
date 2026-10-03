package cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 打印字段模板明细 Response VO")
@Data
public class HcPrintFieldTemplateItemRespVO {

    private Long id;
    private Long templateId;
    private String fieldKey;
    private String fieldLabel;
    private String valueKey;
    private Integer sort;
    private Boolean visible;
    private String defaultValue;
    private String formatType;
    private String suffix;
    private String remark;

}
