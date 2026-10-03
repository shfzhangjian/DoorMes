package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 配料过站工作 Response VO")
@Data
public class HcFormulaPassWorkRespVO {

    private Boolean needConfirm;
    private Boolean complete;
    private String completionMessage;
    private String formulaCategory;
    private String matchReason;
    private Boolean frozen;
    private Long recordId;
    private Long formId;
    private String formCode;
    private String id;
    private String name;
    private String displayName;
    private String timing;
    private String status;
    private String result;
    private String inspectionResult;
    private String recorder;
    private String recorderTime;
    private String confirmer;
    private String confirmerTime;
    private String formRemark;
    private String confirmRemark;
    private Map<String, Object> importAttachment;
    private List<HcFormulaPassWorkItemRespVO> details;
}
