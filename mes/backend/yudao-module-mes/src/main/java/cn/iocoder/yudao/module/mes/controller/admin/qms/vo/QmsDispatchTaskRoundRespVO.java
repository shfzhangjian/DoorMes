package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务检验轮次 Response VO")
@Data
public class QmsDispatchTaskRoundRespVO {

    private Long id;
    private Long taskId;
    private Integer roundNo;
    private String checkType;
    private String inspectionScene;
    private Long recheckDetailId;
    private Long sourceExecutionId;
    private String sourceExecutionNo;
    private Long executionId;
    private String executionNo;
    private String roundStatus;
    private String inspectionStatus;
    private String judgment;
    private String inspectorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    private String returnReason;
    private Long returnedById;
    private String returnedByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime returnedTime;

    private Long confirmedById;
    private String confirmedByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
