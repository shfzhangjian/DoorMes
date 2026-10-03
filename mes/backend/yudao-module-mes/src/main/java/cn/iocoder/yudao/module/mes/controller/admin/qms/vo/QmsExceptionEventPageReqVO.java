package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - QMS异常事件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsExceptionEventPageReqVO extends PageParam {

    @Schema(description = "页签类型：todo/initiated/processed/monitor/containmentOverdue")
    private String tabType;

    @Schema(description = "是否仅查询待我处理的异常")
    private Boolean pendingMine;

    @Schema(description = "是否仅查询我发现的异常")
    private Boolean discoveredMine;

    @Schema(description = "是否仅查询我参与的异常")
    private Boolean participatedMine;

    @Schema(description = "异常单号")
    private String exceptionNo;

    @Schema(description = "异常类别")
    private String exceptionType;

    @Schema(description = "异常等级")
    private String exceptionLevel;

    @Schema(description = "异常状态")
    private String status;

    @Schema(description = "发现时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] discoverTime;
}
