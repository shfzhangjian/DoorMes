package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 客户打印模板新增/修改 Request VO")
@Data
public class HcCustomerPrintTemplateSaveReqVO {

    private Long id;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "客户编号不能为空")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @NotBlank(message = "模板格式不能为空")
    private String templateFormat;

    private String fileName;
    private String fileUrl;
    private Long fileSize;

    private String templateContent;

    private Integer status;
    private String remark;

    @Valid
    private List<HcCustomerPrintTemplateVarSaveReqVO> vars;

}
