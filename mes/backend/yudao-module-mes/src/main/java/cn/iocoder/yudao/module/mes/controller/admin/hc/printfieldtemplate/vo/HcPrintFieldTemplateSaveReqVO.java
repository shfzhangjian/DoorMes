package cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 打印字段模板新增/修改 Request VO")
@Data
public class HcPrintFieldTemplateSaveReqVO {

    private Long id;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "工序编码不能为空")
    private String processCode;

    @NotBlank(message = "工序名称不能为空")
    private String processName;

    @NotBlank(message = "单据类型不能为空")
    private String documentType;

    @NotBlank(message = "单据名称不能为空")
    private String documentName;

    private String usageScene;
    private Integer status;
    private String remark;

    @Valid
    private List<HcPrintFieldTemplateItemSaveReqVO> items;

}
