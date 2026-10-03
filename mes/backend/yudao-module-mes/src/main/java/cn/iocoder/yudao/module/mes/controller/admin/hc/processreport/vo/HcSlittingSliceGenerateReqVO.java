package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 分切生成切片 Request VO")
@Data
public class HcSlittingSliceGenerateReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotNull(message = "来源粘胶报工ID不能为空")
    private Long sourceAdhesiveReportId;

    @NotNull(message = "本次切片数量不能为空")
    @Min(value = 1, message = "本次切片数量必须大于0")
    private Integer sliceCount;

    @Schema(description = "历史字段，分切生成不再按位置控制")
    private BigDecimal startPosition;

    @Schema(description = "单片占用长度(m)")
    private BigDecimal sliceLength;

    @Schema(description = "历史字段，分切生成不再按位置控制")
    private BigDecimal endPosition;

    @Schema(description = "起始流水号，未传时按母批已切片最大流水+1")
    @Min(value = 1, message = "起始流水号必须大于0")
    private Integer startSerialNo;

    @Schema(description = "切片方式：AUTO 自动切片，MANUAL 人工切片")
    private String cutMode;

    private String sizeCode;
    private String sizeName;
}
