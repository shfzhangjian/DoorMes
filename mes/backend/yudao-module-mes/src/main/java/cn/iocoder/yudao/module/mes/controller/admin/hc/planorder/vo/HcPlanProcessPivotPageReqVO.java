package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - HC 计划工序透视报表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcPlanProcessPivotPageReqVO extends HcPlanOrderPageReqVO {

    @Schema(description = "分段批次号，匹配工序追踪分段批号")
    private String motherSegmentBatchNo;

    @Schema(description = "生产进度导出基础列")
    private List<String> exportBaseColumns;

    @Schema(description = "生产进度导出工序列，格式：工序编码.子列编码")
    private List<String> exportStageColumns;

}
