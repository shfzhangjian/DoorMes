package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - IPQC过程抽检分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsIpqcPageReqVO extends PageParam {

    @Schema(description = "IPQC过程巡检单号")
    private String ipqcNo;

    @Schema(description = "生产机台")
    private String machineCode;

    @Schema(description = "生产工单号")
    private String workOrderNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "巡检类型")
    private String inspectionType;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "巡检时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectionTime;
}
