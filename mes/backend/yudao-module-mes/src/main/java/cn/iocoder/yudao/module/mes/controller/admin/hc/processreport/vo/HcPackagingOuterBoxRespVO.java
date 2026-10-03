package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 外包装箱/板 Response VO")
@Data
public class HcPackagingOuterBoxRespVO {

    private Long id;
    private String outerBoxNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String packMethod;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private String productSize;
    private Integer standardQty;
    private Integer currentQty;
    private Boolean tailBoxFlag;
    private String outerLabelNo;
    private String boxStatus;
    private Integer printCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;

    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;

    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    private String remark;
    private List<HcPackagingOuterBoxItemRespVO> items;
}
