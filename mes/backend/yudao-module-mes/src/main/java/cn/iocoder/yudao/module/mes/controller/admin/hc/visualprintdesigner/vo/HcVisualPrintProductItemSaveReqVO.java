package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HcVisualPrintProductItemSaveReqVO {

    private Long id;
    private Long customerInfoId;
    private Integer sourceRow;
    @NotBlank(message = "产品类型不能为空")
    private String productType;
    @NotBlank(message = "尺寸/mm不能为空")
    private String sizeMm;
    private String padBackLabelImageId;
    private String padBackLabelImageFile;
    private String cleanBagLabelImageId;
    private String cleanBagLabelImageFile;
    private String boxFrontLabelImageId;
    private String boxFrontLabelImageFile;
    private String customerSideLabelImageId;
    private String customerSideLabelImageFile;
    private Integer status;

}
