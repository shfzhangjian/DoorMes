package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 生产指令保存/下达 Request VO")
@Data
public class HcProductionInstructionSaveReqVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "指令批次号")
    private String instructionBatchNo;

    @Schema(description = "父指令 ID")
    private Long parentInstructionId;

    @Schema(description = "计划 ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "计划工序 ID")
    private Long planOperationId;

    @Schema(description = "标准工序 ID")
    private Long processId;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @Schema(description = "计划工序编码")
    private String operationCode;

    @Schema(description = "计划工序名称")
    private String operationName;

    @NotBlank(message = "批次号不能为空")
    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String batchNo;

    @Schema(description = "母批/生产批号")
    private String productionBatchNo;

    @Schema(description = "分段批号")
    private String segmentBatchNo;

    @Schema(description = "指令类型：DAILY/PAUSE/RESUME/CANCEL/CHANGEOVER")
    private String instructionType;

    @Schema(description = "作用范围：PLAN/OPERATION/SEGMENT")
    private String scopeType;

    @Schema(description = "目标计划工序 ID 列表")
    private List<Long> operationIds;

    @Schema(description = "接收人 ID 列表")
    private List<Long> recipientIds;

    @NotBlank(message = "指令内容不能为空")
    @Schema(description = "指令内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instructionContent;

    @Schema(description = "换型前料号")
    private String beforeMaterialCode;

    @Schema(description = "换型目标料号")
    private String targetMaterialCode;

    @Schema(description = "换型前产品型号")
    private String beforeModelCode;

    @Schema(description = "换型目标产品型号")
    private String targetModelCode;

    @Schema(description = "换型目标片数")
    private Integer targetQty;

    @Schema(description = "下达人 ID")
    private Long issuerId;

    @Schema(description = "下达人")
    private String issuerName;

    @Schema(description = "下达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime issuedTime;

    @Schema(description = "备注")
    private String remark;

}
