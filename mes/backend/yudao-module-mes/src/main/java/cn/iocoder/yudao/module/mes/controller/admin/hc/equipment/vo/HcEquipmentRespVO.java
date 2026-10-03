package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 设备台账 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcEquipmentRespVO {

    @Schema(description = "设备编码")
    @ExcelProperty("设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
    @ExcelProperty("设备名称")
    private String equipmentName;

    @Schema(description = "所属工作中心ID")
    @ExcelProperty("所属工作中心ID")
    private Long workCenterId;

    @Schema(description = "所属工作中心编码")
    @ExcelProperty("所属工作中心编码")
    private String workCenterCode;

    @Schema(description = "所属工作中心名称")
    @ExcelProperty("所属工作中心名称")
    private String workCenterName;

    @Schema(description = "设备类型")
    @ExcelProperty("设备类型")
    private String equipmentType;

    @Schema(description = "适用垫型")
    @ExcelProperty("适用垫型")
    private String applicablePadTypeName;

    @Schema(description = "适用垫型编码")
    private String applicablePadType;

    @Schema(description = "资产编号")
    @ExcelProperty("资产编号")
    private String assetNo;

    @Schema(description = "是否启用点检")
    @ExcelProperty("是否启用点检")
    private Boolean enableQcChecklist;

    @Schema(description = "是否启用清洁点检")
    @ExcelProperty("是否启用清洁点检")
    private Boolean enableCleanChecklist;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "运行状态")
    @ExcelProperty("运行状态")
    private String workStatus;

    @Schema(description = "当前计划号")
    @ExcelProperty("当前计划号")
    private String currentPlanNo;

    @Schema(description = "当前工序编号")
    @ExcelProperty("当前工序编号")
    private String currentOperationCode;

    @Schema(description = "当前工序名称")
    @ExcelProperty("当前工序名称")
    private String currentOperationName;

    @Schema(description = "开工时间")
    @ExcelProperty("开工时间")
    private LocalDateTime currentStartTime;

    @Schema(description = "完工时间")
    @ExcelProperty("完工时间")
    private LocalDateTime currentEndTime;

    @Schema(description = "操作人")
    @ExcelProperty("操作人")
    private String currentOperatorName;

    @Schema(description = "回写时间")
    @ExcelProperty("回写时间")
    private LocalDateTime currentRecordTime;

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
