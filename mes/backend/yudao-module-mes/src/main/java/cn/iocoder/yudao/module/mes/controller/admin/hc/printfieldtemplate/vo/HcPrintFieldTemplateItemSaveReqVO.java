package cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 打印字段模板明细新增/修改 Request VO")
@Data
public class HcPrintFieldTemplateItemSaveReqVO {

    private Long id;

    @NotBlank(message = "字段编码不能为空")
    private String fieldKey;

    @NotBlank(message = "显示名称不能为空")
    private String fieldLabel;

    @NotBlank(message = "取值字段不能为空")
    private String valueKey;

    private Integer sort;
    private Boolean visible;
    private String defaultValue;
    private String formatType;
    private String suffix;
    private String remark;

}
