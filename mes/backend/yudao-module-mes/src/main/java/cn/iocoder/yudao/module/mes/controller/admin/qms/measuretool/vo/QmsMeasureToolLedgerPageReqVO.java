package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 量检具台账分页 Request VO")
@Data
public class QmsMeasureToolLedgerPageReqVO extends PageParam {

    @Schema(description = "量检具编码")
    private String toolCode;

    @Schema(description = "机身号")
    private String bodyNo;

    @Schema(description = "量检具名称")
    private String toolName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类ID集合，选择分类树父节点时包含其子分类")
    private Long[] categoryIds;

    @Schema(description = "位置（一级分类名称反范式）")
    private String storageLocation;

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "保管人")
    private String keeperName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "设备状态集合（Excel 表头筛选）")
    private String[] statusValues;

    @Schema(description = "校准预警状态")
    private String calibrationStatus;

    @Schema(description = "校准预警状态集合（Excel 表头筛选）")
    private String[] calibrationStatusValues;

    @Schema(description = "MSA预警状态")
    private String msaStatus;

    @Schema(description = "MSA预警状态集合（Excel 表头筛选）")
    private String[] msaStatusValues;

    @Schema(description = "只看逾期（校准或已纳入 MSA 的分析逾期）")
    private Boolean onlyOverdue;

    @Schema(description = "只看本月提醒（校准或已纳入 MSA 的分析日期落在本月）")
    private Boolean onlyCurrentMonthReminder;

    @Schema(description = "是否纳入MSA分析")
    private Integer msaEnabled;

    @Schema(description = "是否纳入MSA分析集合（Excel 表头筛选）")
    private Integer[] msaEnabledValues;

    @Schema(description = "是否对外开放：0否、1是")
    private Integer externalOpen;

    @Schema(description = "仅校准逾期（根据下次校准日期实时计算）")
    private Boolean calibrationOverdue;

    @Schema(description = "责任人")
    private String responsiblePerson;

    @Schema(description = "下次校准日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] nextCalibrationDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "排序字段（后端白名单校验）")
    private String sortField;

    @Schema(description = "排序方向（asc/desc）")
    private String sortOrder;

}
