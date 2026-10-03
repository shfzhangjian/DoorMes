package cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 量产备货库存出货相关明细 Response VO")
@Data
public class HcMassStockShippingDetailRespVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "需求单ID")
    private Long noticeId;

    @Schema(description = "需求单号")
    private String noticeNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "客户")
    private String customerName;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "母卷批号")
    private String motherBatchNo;

    @Schema(description = "母卷段号")
    private String motherSegmentBatchNo;

    @Schema(description = "需求批号")
    private String requiredBatchNo;

    @Schema(description = "需求片号范围")
    private String requiredSliceRange;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "需求片号")
    private String sliceBatchNo;

    @Schema(description = "实际片号")
    private String actualSliceBatchNo;

    @Schema(description = "客户产品批号")
    private String customerProductBatchNo;

    @Schema(description = "内部项目号")
    private String internalItemCode;

    @Schema(description = "数量")
    private BigDecimal qty;

    @Schema(description = "锁定状态")
    private String lockStatus;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "发货检验结果")
    private String shippingInspectionResult;

    @Schema(description = "发货检验人")
    private String shippingInspectorName;

    @Schema(description = "发货检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippingInspectionTime;

    @Schema(description = "货架")
    private String warehouseName;

    @Schema(description = "货位编码")
    private String locationCode;

    @Schema(description = "货位")
    private String locationName;

    @Schema(description = "库存编号")
    private String stockNo;

    @Schema(description = "备注")
    private String remark;

}
