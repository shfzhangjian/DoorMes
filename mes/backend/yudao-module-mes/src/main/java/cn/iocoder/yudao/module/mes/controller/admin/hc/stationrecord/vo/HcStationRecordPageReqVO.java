package cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工位记录查询分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcStationRecordPageReqVO extends PageParam {

    @Schema(description = "关键词，匹配计划号、工序、表单、设备")
    private String keyword;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "排除的表单编码")
    private String excludeFormCode;

    @Schema(description = "表单编码前缀")
    private String formCodePrefix;

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "设备名")
    private String equipmentName;

    @Schema(description = "记录作用域")
    private String recordScope;

    @Schema(description = "创建开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "创建结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;

    @Schema(description = "创建人/记录人")
    private String createUserName;

    @Schema(description = "确认人")
    private String confirmUserName;

    @Schema(description = "确认开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime confirmTimeStart;

    @Schema(description = "确认结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime confirmTimeEnd;
}
