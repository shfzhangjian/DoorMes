package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HcVisualPrintProductItemRespVO {

    private Long id;
    private Long customerInfoId;
    private Integer sourceRow;
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
    private Integer status;
    private String importBatchNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
