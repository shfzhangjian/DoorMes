package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲片号统计保存 Request VO")
@Data
public class QmsCmpWarpageSliceStatSaveReqVO {

    @Schema(description = "主键；更新时必填")
    private Long id;

    @Schema(description = "统计日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    @Schema(description = "产品型号")
    @Size(max = 128, message = "产品型号长度不能超过128个字符")
    private String modelCode;

    @Schema(description = "母批号")
    @Size(max = 128, message = "母批号长度不能超过128个字符")
    private String parentBatchNo;

    @Schema(description = "分段片号/内部编号")
    @Size(max = 128, message = "分段片号长度不能超过128个字符")
    private String segmentSliceNo;

    @Schema(description = "生产片号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生产片号不能为空")
    @Size(max = 128, message = "生产片号长度不能超过128个字符")
    private String productionSliceNo;

    @Schema(description = "翘曲高度实际值(mm)")
    @DecimalMin(value = "0", message = "翘曲高度不能小于0")
    private BigDecimal warpageValueMm;

    @Schema(description = "是否人工覆盖实际值")
    private Boolean manualOverride;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
