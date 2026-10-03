package cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工作中心 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcWorkCenterRespVO {

    @Schema(description = "工作中心编码")
    @ExcelProperty("工作中心编码")
    private String wcCode;

    @Schema(description = "工作中心名称")
    @ExcelProperty("工作中心名称")
    private String wcName;

    @Schema(description = "工序名称，兼容历史字段 process_stage")
    @ExcelProperty("工序")
    private String processStage;

    @Schema(description = "标准工序ID")
    @ExcelProperty("标准工序ID")
    private Long processId;

    @Schema(description = "标准工序编码")
    @ExcelProperty("标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    @ExcelProperty("标准工序名称")
    private String processName;

    @Schema(description = "产线编码")
    @ExcelProperty("产线编码")
    private String lineCode;

    @Schema(description = "产线名称")
    @ExcelProperty("产线名称")
    private String lineName;

    @Schema(description = "产线短码")
    @ExcelProperty("产线短码")
    private String lineShortCode;

    @Schema(description = "批次号产线码")
    @ExcelProperty("批次号产线码")
    private String batchLineCode;

    @Schema(description = "产线排序")
    @ExcelProperty("产线排序")
    private Integer lineSort;

    @Schema(description = "绑定工位 IP")
    @ExcelProperty("绑定工位 IP")
    private String terminalIps;

    @Schema(description = "标准小时产能")
    @ExcelProperty("标准小时产能")
    private BigDecimal capacityPerHour;

    @Schema(description = "产能单位")
    @ExcelProperty("产能单位")
    private String capacityUom;

    @Schema(description = "默认班制")
    @ExcelProperty("默认班制")
    private String defaultShiftMode;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
