package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 湿法过站工作 Response VO")
@Data
public class HcWetPassWorkRespVO {

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
    private String recordDate;
    private Boolean canFill;
    private Boolean canConfirm;
    private Boolean canView;
    private String recorder;
    private String recorderTime;
    private String confirmer;
    private String confirmerTime;
    private String formRemark;
    private String confirmRemark;
    private String presetHeaderDataJson;
    private String headerDataJson;
    private String schemaJson;
    private List<HcWetPassWorkItemRespVO> presetDetails;
    private List<HcWetPassWorkItemRespVO> details;
}
