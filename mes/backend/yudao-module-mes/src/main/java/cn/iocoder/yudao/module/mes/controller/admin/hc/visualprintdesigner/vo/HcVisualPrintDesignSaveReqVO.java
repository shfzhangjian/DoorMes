package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HcVisualPrintDesignSaveReqVO {

    private Long id;
    private Boolean forceNew;
    @NotNull(message = "客户打印信息ID不能为空")
    private Long customerInfoId;
    private Long productItemId;
    @NotBlank(message = "标签类型不能为空")
    private String labelKind;
    private String labelName;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private Integer dpi;
    private String imageId;
    private String imageFile;
    private Integer imageWidthPx;
    private Integer imageHeightPx;
    private String btwTemplateRootDir;
    private String btwCallFile;
    @NotBlank(message = "设计JSON不能为空")
    private String designJson;
    private Integer status;
    private String remark;

}
