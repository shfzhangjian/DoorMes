package cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工位记录查询 Response VO")
@Data
public class HcStationRecordRespVO {

    private Long id;
    private Long planId;
    private String planNo;
    private String batchNo;
    private String modelCode;
    private String modelName;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long formId;
    private String formCode;
    private String formName;
    private String triggerTimingCode;
    private String triggerTimingName;
    private String docStatus;
    private String resultStatus;
    private String inspectionResult;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long mixerEquipmentId;
    private String mixerEquipmentCode;
    private String mixerEquipmentName;
    private Long foamingEquipmentId;
    private String foamingEquipmentCode;
    private String foamingEquipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String recordScope;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;
    private String bizType;
    private Long bizId;
    private String recordUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;
    private String createUserName;
    private String creator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String confirmUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;
    private String headerDataJson;
    private String formRemark;
    private String confirmRemark;
    private String updater;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
