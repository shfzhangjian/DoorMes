package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 各工序表单填写记录保存 Request VO")
@Data
public class HcProcessFormRecordSaveReqVO {

    private Long id;
    private Long templateId;
    private Long versionId;

    @NotBlank(message = "工序不能为空")
    private String processCode;
    private String processName;
    private String modelCode;
    private String modelName;

    @NotBlank(message = "单据类型不能为空")
    private String formType;
    private String formTypeName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String batchNo;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String headerDataJson;
    private String contextJson;
    private String remark;
    private List<HcProcessFormRecordItemReqVO> items;
}
