package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 配料报工任务列表 Response VO")
@Data
public class HcFormulaReportTaskRespVO {

    @Schema(description = "任务展示编号")
    private String id;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "最近一次报工记录ID")
    private Long operationReportId;

    @Schema(description = "计划单号")
    private String planNo;

    @Schema(description = "ERP订单号")
    private String erpOrderNo;

    @Schema(description = "计划类型")
    private String planType;

    @Schema(description = "加工产品")
    private String product;

    @Schema(description = "产品料号")
    private String materialCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "母料料号")
    private String motherMaterialCode;

    @Schema(description = "母料名称")
    private String motherMaterialName;

    @Schema(description = "产品规格")
    private String spec;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "母料型号")
    private String motherModelCode;

    @Schema(description = "计划开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionStartDate;

    @Schema(description = "交货日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionEndDate;

    @Schema(description = "实际生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionDate;

    @Schema(description = "生产批次号")
    private String batchNo;

    @Schema(description = "母批批号是否为规则预览值；配料开工后会替换为正式占号结果")
    private Boolean batchNoPreview;

    @Schema(description = "当前工序生产批号")
    private String productionBatchNo;

    @Schema(description = "上游生产批号")
    private String parentProductionBatchNo;

    @Schema(description = "工序名称")
    private String process;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "计划量")
    private BigDecimal planQty;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "累计良品量")
    private BigDecimal goodQty;

    @Schema(description = "累计不良量")
    private BigDecimal scrapQty;

    @Schema(description = "前端状态：PENDING/IN_PROGRESS/COMPLETED")
    private String status;

    @Schema(description = "首次报工开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "执行要求")
    private String requirements;

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "配方名称")
    private String recipeName;

    @Schema(description = "最近一次配料单号")
    private String batchingNo;

    @Schema(description = "最近一次投料批次")
    private String feedBatchNo;

    @Schema(description = "最近一次实投数量")
    private BigDecimal feedQty;

    @Schema(description = "最近一次搅拌开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime stirStartTime;

    @Schema(description = "最近一次搅拌结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime stirEndTime;

    @Schema(description = "最近一次结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    @Schema(description = "确认人")
    private String confirmerName;

    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;

    @Schema(description = "搅拌机台")
    private Long mixerEquipmentId;
    private String mixerEquipmentCode;
    private String mixerEquipmentName;

    @Schema(description = "泡发机台")
    private Long foamingEquipmentId;
    private String foamingEquipmentCode;
    private String foamingEquipmentName;

    @Schema(description = "粘度(mPa.s)")
    private BigDecimal viscosity;

    @Schema(description = "浆料温度(℃)")
    private BigDecimal slurryTemperature;

    @Schema(description = "滤网批号")
    private String filterBatchNo;

    @Schema(description = "投料重量(kg)")
    private BigDecimal inputWeight;

    @Schema(description = "配料罐罐号")
    private String batchingTankNo;

    @Schema(description = "脱泡罐罐号")
    private String defoamingTankNo;

    @Schema(description = "最近一次报工备注")
    private String reportRemark;
}
