package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderPageReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - QMS 母卷批次良品统计分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsMotherRollGoodStatisticsPageReqVO extends HcPlanOrderPageReqVO {

    @Schema(description = "分段批次号，匹配工序追踪分段批号")
    private String motherSegmentBatchNo;

    @Schema(description = "母卷批次良品统计导出基础列")
    private List<String> exportBaseColumns;

    @Schema(description = "母卷批次良品统计导出工序列，格式：工序编码.子列编码")
    private List<String> exportStageColumns;

}
