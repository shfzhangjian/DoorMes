package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划工序 Request VO")
@Data
public class HcPlanOrderOperationReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "工序顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序顺序不能为空")
    private Integer opSeq;

    @Schema(description = "工序编码快照")
    private String opCode;

    @Schema(description = "工序名称快照", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序名称不能为空")
    private String opName;

    @Schema(description = "来源路线工序ID")
    private Long routeOperationId;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "工作中心编码快照")
    private String workCenterCode;

    @Schema(description = "工作中心名称快照")
    private String workCenterName;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编码快照")
    private String equipmentCode;

    @Schema(description = "设备名称快照")
    private String equipmentName;

    @Schema(description = "转换率/收得率", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转换率不能为空")
    private BigDecimal yieldRate;

    @Schema(description = "推算目标量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "推算目标量不能为空")
    private BigDecimal requiredQty;

    @Schema(description = "已锁定量")
    private BigDecimal lockedQty;

    @Schema(description = "已下发量")
    private BigDecimal dispatchQty;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位符号")
    private String unitCode;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "单位不能为空")
    private String uom;

    @Schema(description = "工艺执行指示")
    private String instructionText;

    @Schema(description = "是否存在锁定明细")
    private Boolean hasLock;

    @Schema(description = "工序状态")
    private String operationStatus;

    @Schema(description = "手工完工时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime finishTime;

    @Schema(description = "完工备注")
    private String finishRemark;

    @Schema(description = "拆批标记：SPLIT_OUT/SPLIT_IN")
    private String splitMark;

    @Schema(description = "来源计划ID")
    private Long sourcePlanId;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "来源计划工序ID")
    private Long sourcePlanOperationId;

    @Schema(description = "来源工序编码")
    private String sourceOperationCode;

    @Schema(description = "来源工序名称")
    private String sourceOperationName;

    @Schema(description = "暂停范围：ALL/DATE_RANGE")
    private String pauseScope;

    @Schema(description = "暂停开始日期")
    private LocalDate pauseStartDate;

    @Schema(description = "暂停结束日期")
    private LocalDate pauseEndDate;

    @Schema(description = "暂停说明")
    private String pauseRemark;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "最近一次状态操作人ID")
    private Long statusOperatorId;

    @Schema(description = "最近一次状态操作人名称")
    private String statusOperatorName;

    @Schema(description = "最近一次状态操作时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime statusOperateTime;

    @Schema(description = "按日期记录工序暂停/复工角标 JSON")
    private String statusDateMarksJson;

    @Schema(description = "展示顺序")
    private Integer sort;

}
