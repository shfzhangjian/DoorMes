package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 包装点检/清洁 Response VO")
@Data
public class HcPackagingPassWorkRespVO {

    private Long recordId;
    private Long formId;
    private String formCode;
    private String id;
    private String name;
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
    private List<HcPackagingPassWorkItemRespVO> presetDetails;
    private List<HcPackagingPassWorkItemRespVO> details;
}
