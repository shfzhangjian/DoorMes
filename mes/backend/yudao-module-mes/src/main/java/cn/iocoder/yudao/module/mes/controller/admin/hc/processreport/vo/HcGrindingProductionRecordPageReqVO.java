package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 磨皮生产记录表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcGrindingProductionRecordPageReqVO extends PageParam {

    @Schema(description = "完工日期开始，精确到秒")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTimeStart;

    @Schema(description = "完工日期结束，精确到秒")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTimeEnd;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "垫型：BLACK_PAD/WHITE_PAD；UNCLASSIFIED 仅用于查询未归类历史记录")
    private String padType;

    @Schema(description = "料号")
    private String materialCode;

    @Schema(description = "加工母批号")
    private String motherBatchNo;

    @Schema(description = "批号，支持母批、分段批号、来源生产批号")
    private String batchNo;

    @Schema(description = "生产记录角色：FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND")
    private String recordRole;

    @Schema(description = "来源业务类型：FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND")
    private String sourceBizType;

    @Schema(description = "加工单元：P/Q/R/S/NONE")
    private String segmentMark;

    @Schema(description = "磨皮次数，FIRST 一次，SECOND 二次，THIRD 三次，FOURTH 四次")
    private String grindingPass;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "确认状态，WAIT_CONFIRM/CONFIRMED")
    private String status;

}
