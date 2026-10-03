package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 计划换型日志 Response VO")
@Data
public class HcPlanChangeoverLogRespVO {

    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String beforeMaterialCode;
    private String afterMaterialCode;
    private String beforeModelCode;
    private String afterModelCode;
    private String beforeGlueBoardModel;
    private String afterGlueBoardModel;
    private Boolean changeoverFlag;
    private Long changeoverUserId;
    private String changeoverUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime changeoverTime;
    private String remark;
    private String extraJson;
}
