package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 不合格品处理单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsNcRecordPageReqVO extends PageParam {

    @Schema(description = "页签类型：todo/initiated/processed/monitor")
    private String tabType;

    @Schema(description = "仅查询流转中（排除草稿、关闭、取消）")
    private Boolean inProgressOnly;

    @Schema(description = "包装流转查询的完整片号，精确匹配")
    private String packagingPieceNo;


    @Schema(description = "是否只查询待我处理的处置单")
    private Boolean pendingMine;

    @Schema(description = "是否只查询我发现的处置单")
    private Boolean discoveredMine;

    @Schema(description = "是否只查询我参与的处置单")
    private Boolean participatedMine;

    @Schema(description = "NCR单号")
    private String ncNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(hidden = true)
    private Boolean excludeRawMaterial;

    @Schema(description = "关联来源单据类型")
    private String sourceBizType;

    @Schema(description = "发生部门")
    private String happenDeptName;

    @Schema(description = "原物料异常类别")
    private String rawMaterialAbnormalCategory;

    @Schema(description = "关联派工细单ID")
    private Long subOrderId;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "缺陷代码")
    private String defectCode;

    @Schema(description = "不合格等级")
    private String ncLevel;

    @Schema(description = "NCR状态")
    private String status;

    @Schema(description = "兼容旧MRB决策")
    private String mrbDecision;

    @Schema(description = "终审处置结论")
    private String finalDisposition;

    @Schema(description = "仅查询异常事件可挂接处置单：未挂接异常")
    private Boolean linkableExceptionOnly;

    @Schema(description = "发生时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] happenTime;
}
