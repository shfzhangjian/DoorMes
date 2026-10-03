package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class HcVisualPrintDesignRespVO {

    private Long id;
    private Long customerInfoId;
    private Long productItemId;
    private Long bindingId;
    private Integer sharedCount;
    private List<HcVisualPrintProductItemRespVO> linkedProductItems;
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
    private String designJson;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
