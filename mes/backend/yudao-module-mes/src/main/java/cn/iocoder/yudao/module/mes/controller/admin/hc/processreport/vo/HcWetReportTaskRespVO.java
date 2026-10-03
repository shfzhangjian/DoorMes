package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工任务列表 Response VO")
@Data
public class HcWetReportTaskRespVO {

    @Schema(description = "任务展示编号")
    private String id;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "最近一次湿法END报工记录ID")
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

    @Schema(description = "生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionStartDate;

    @Schema(description = "生产结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionEndDate;

    @Schema(description = "实际生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionDate;

    @Schema(description = "生产批次号")
    private String batchNo;

    @Schema(description = "当前工序生产批号")
    private String productionBatchNo;

    @Schema(description = "上游生产批号")
    private String parentProductionBatchNo;

    @Schema(description = "工序名称")
    private String process;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "计划量")
    private BigDecimal planQty;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "累计良品量")
    private BigDecimal goodQty;

    @Schema(description = "累计不良量")
    private BigDecimal scrapQty;

    @Schema(description = "当前湿法首检 NAP层送检米数")
    private BigDecimal napSampleLength;

    @Schema(description = "前端状态：PENDING/IN_PROGRESS/COMPLETED/RELEASED")
    private String status;

    @Schema(description = "首次报工开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "最近一次结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @Schema(description = "执行要求")
    private String requirements;

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

    @Schema(description = "最近一次报工备注")
    private String reportRemark;

    @Schema(description = "前序工序名称")
    private String previousOperationName;

    @Schema(description = "前序工序状态")
    private String previousOperationStatus;

    @Schema(description = "前序工序生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate previousProductionDate;

    @Schema(description = "前序工序开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime previousStartTime;

    @Schema(description = "前序工序结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime previousEndTime;

    @Schema(description = "前序工序记录人")
    private String previousRecorderName;

    @Schema(description = "前序工序良品量")
    private BigDecimal previousGoodQty;

    @Schema(description = "配料完工到湿法开工的额定 QTIME 评估结果")
    private HcQtimeEvaluationRespVO qtime;

    @Schema(description = "当前关联 FAI 主单 ID")
    private Long faiId;

    @Schema(description = "当前关联 FAI 首检单号")
    private String faiNo;

    @Schema(description = "FAI 单据状态；字典:mes_fai_status")
    private String faiStatus;

    @Schema(description = "FAI 判定结果；字典:mes_qms_judgment_result")
    private String faiJudgment;

    @Schema(description = "命中的 FAI 检验标准 ID")
    private Long faiStandardId;

    @Schema(description = "命中的 FAI 检验标准编号")
    private String faiStandardNo;

    @Schema(description = "提交首检申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiApplyTime;

    @Schema(description = "FAI 最近状态回写时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiReturnTime;

    @Schema(description = "最近首检驳回或 NG 说明")
    private String faiRejectReason;

    @Schema(description = "扩展业务数据JSON")
    private String extraJson;
}
