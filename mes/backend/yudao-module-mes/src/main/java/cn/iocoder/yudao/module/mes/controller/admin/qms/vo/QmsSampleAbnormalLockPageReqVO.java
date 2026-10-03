package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 留样异常复检锁定分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsSampleAbnormalLockPageReqVO extends PageParam {

    @Schema(description = "锁定单号")
    private String lockNo;

    @Schema(description = "锁定状态")
    private String lockStatus;

    @Schema(description = "对象类型")
    private String objectType;

    @Schema(description = "母卷/分段号")
    private String objectNo;

    @Schema(description = "异常来源工序编码")
    private String sourceProcessCode;

    @Schema(description = "工单/计划号")
    private String planNo;

    @Schema(description = "异常检验单号")
    private String abnormalInspectionNo;

    @Schema(description = "异常结果")
    private String abnormalResult;

    @Schema(description = "异常反馈时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] abnormalFeedbackTime;

    @Schema(description = "复检检验单号")
    private String recheckInspectionNo;

    @Schema(description = "复检结果")
    private String recheckResult;

    @Schema(description = "复检反馈时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] recheckFeedbackTime;
}
