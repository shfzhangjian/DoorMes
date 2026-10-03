package cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 工序耗材使用更换流水保存 Request VO")
@Data
public class HcProcessMaterialLifeEventSaveReqVO {

    @Schema(description = "状态ID")
    private Long stateId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "工序编码")
    @NotBlank(message = "工序不能为空")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "耗材类型")
    @NotBlank(message = "耗材类型不能为空")
    private String consumableType;

    @Schema(description = "耗材批号")
    private String batchNo;

    @Schema(description = "事件类型，USE=使用，REPLACE=更换")
    @NotBlank(message = "流水类型不能为空")
    private String eventType;

    @Schema(description = "计划编号")
    private String planNo;

    @Schema(description = "计划对应母批号")
    private String motherBatchNo;

    @Schema(description = "使用次数")
    private Integer changeUseCount;

    @Schema(description = "使用米数")
    private BigDecimal changeLength;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "更换原因")
    private String replaceReason;

    @Schema(description = "流水时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime eventTime;

    @Schema(description = "备注")
    private String remark;
}
