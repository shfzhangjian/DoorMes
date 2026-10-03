package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - QMS异常锁定分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsAbnormalLockPageReqVO extends PageParam {

    @Schema(description = "锁定单号")
    private String lockNo;

    @Schema(description = "检验单类型")
    private String inspectionOrderType;

    @Schema(description = "检验单号")
    private String inspectionOrderNo;

    @Schema(description = "锁定来源")
    private String lockSourceType;

    @Schema(description = "锁定范围")
    private String lockScope;

    @Schema(description = "锁定状态")
    private String lockStatus;

    @Schema(description = "受影响批次")
    private String affectedBatchNo;

    @Schema(description = "原料/来源批次")
    private String sourceBatchNo;

    @Schema(description = "生产批次")
    private String productionBatchNo;

    @Schema(description = "父级生产批次")
    private String parentProductionBatchNo;

    @Schema(description = "胶板批次")
    private String glueBoardBatchNo;

    @Schema(description = "工单/计划号")
    private String planNo;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "锁定时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] lockTime;
}
