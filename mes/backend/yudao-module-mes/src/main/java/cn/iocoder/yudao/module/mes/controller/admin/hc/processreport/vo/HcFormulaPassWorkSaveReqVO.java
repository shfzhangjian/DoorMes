package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 配料过站工作填写/确认 Request VO")
@Data
public class HcFormulaPassWorkSaveReqVO {

    @Schema(description = "无需确认的表单提交完成；false或空仅保存草稿")
    private Boolean submit;

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @Schema(description = "表单编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "表单编码不能为空")
    private String formCode;

    @Schema(description = "执行结果")
    private String result;

    @Schema(description = "检验结果")
    private String inspectionResult;

    @Schema(description = "执行备注")
    private String formRemark;

    @Schema(description = "记录人")
    private String recorder;

    @Schema(description = "记录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;

    @Schema(description = "确认人")
    private String confirmer;

    @Schema(description = "确认时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;

    @Schema(description = "确认备注")
    private String confirmRemark;

    @Schema(description = "最后一次Excel导入原始附件")
    private Map<String, Object> importAttachment;

    @Schema(description = "搅拌机台号")
    private Long mixerEquipmentId;
    private String mixerEquipmentCode;
    private String mixerEquipmentName;

    @Schema(description = "泡发机台号")
    private Long foamingEquipmentId;
    private String foamingEquipmentCode;
    private String foamingEquipmentName;

    @Schema(description = "明细")
    @Valid
    private List<HcFormulaPassWorkItemReqVO> details;
}
