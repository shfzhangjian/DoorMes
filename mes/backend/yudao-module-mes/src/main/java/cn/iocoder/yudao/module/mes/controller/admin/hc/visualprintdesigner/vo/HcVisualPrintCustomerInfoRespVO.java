package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class HcVisualPrintCustomerInfoRespVO {

    private Long id;
    private Long productItemId;
    private Integer sourceRow;
    private Integer masterSourceRow;
    private String serialNo;
    private String customer;
    private String productType;
    private String sizeMm;
    private String padBackLabelImageId;
    private String padBackLabelImageFile;
    private Long padBackDesignId;
    private Integer padBackSharedCount;
    private String cleanBagLabelImageId;
    private String cleanBagLabelImageFile;
    private Long cleanBagDesignId;
    private Integer cleanBagSharedCount;
    private String boxFrontLabelImageId;
    private String boxFrontLabelImageFile;
    private Long boxFrontDesignId;
    private Integer boxFrontSharedCount;
    private String customerSideLabelImageId;
    private String customerSideLabelImageFile;
    private Long customerSideDesignId;
    private Integer customerSideSharedCount;
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
    private String importBatchNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<HcVisualPrintProductItemRespVO> productItems;
    private List<HcVisualPrintDesignRespVO> designs;

}
