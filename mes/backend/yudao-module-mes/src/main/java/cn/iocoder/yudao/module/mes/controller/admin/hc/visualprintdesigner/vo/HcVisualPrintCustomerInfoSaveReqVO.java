package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 可视化打印客户信息保存 Request VO")
@Data
public class HcVisualPrintCustomerInfoSaveReqVO {

    private Long id;
    private Long productItemId;
    private Integer sourceRow;
    private String serialNo;
    @NotBlank(message = "客户不能为空")
    private String customer;
    private String productType;
    private String sizeMm;
    private String padBackLabelImageId;
    private String padBackLabelImageFile;
    private String cleanBagLabelImageId;
    private String cleanBagLabelImageFile;
    private String boxFrontLabelImageId;
    private String boxFrontLabelImageFile;
    private String customerSideLabelImageId;
    private String customerSideLabelImageFile;
    private String customerSideSize;
    private String customerSideMethod;
    private String shippingMethod;
    private String needPaperCoa;
    private String needEcoa;
    private String hasMark;
    private String deliveryNote;
    private String shipmentFilePackageMethod;
    private String customerSideTemplate;
    private String specialRemark;
    private Integer status;
    @Valid
    private List<HcVisualPrintProductItemSaveReqVO> productItems;

}
