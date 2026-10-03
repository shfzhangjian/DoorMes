package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QmsDispatchTaskLogRespVO {

    private Long id;
    private Long taskId;
    private String actionType;
    private String actionDesc;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime actionTime;

    private String details;
}
