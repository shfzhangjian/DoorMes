package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 计划工序透视片级明细 Response VO")
@Data
public class HcPlanProcessPivotPieceRespVO {

    @Schema(description = "工序阶段编码")
    private String stageCode;

    @Schema(description = "片号")
    private String pieceNo;

    @Schema(description = "来源批号")
    private String sourceBatchNo;

    @Schema(description = "产出批号")
    private String outputBatchNo;

    @Schema(description = "实际/显示型号")
    private String actualModelCode;

    @Schema(description = "实际/显示尺寸")
    private String actualSizeSpec;

    @Schema(description = "裁切输出实际尺寸")
    private String outputActualSizeSpec;

    @Schema(description = "状态：DONE/PENDING/DEFECT")
    private String status;

    @Schema(description = "本工序是否确认")
    private Boolean reportConfirmed;

    @Schema(description = "是否NG/损耗")
    private Boolean defectFlag;

    @Schema(description = "是否COA片")
    private Boolean coaFlag;

    @Schema(description = "最后报工/检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReportTime;

    @Schema(description = "备注")
    private String remark;

}
