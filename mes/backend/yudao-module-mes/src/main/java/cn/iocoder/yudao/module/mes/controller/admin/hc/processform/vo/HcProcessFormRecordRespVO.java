package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HcProcessFormRecordRespVO {

    private Long id;
    private String recordNo;
    private Long templateId;
    private Long versionId;
    private String templateCode;
    private String templateName;
    private String processCode;
    private String processName;
    private String modelCode;
    private String modelName;
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
    private String recordStatus;
    private String resultStatus;
    private Long fillUserId;
    private String fillUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime fillTime;
    private Long confirmUserId;
    private String confirmUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;
    private String headerDataJson;
    private String contextJson;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
