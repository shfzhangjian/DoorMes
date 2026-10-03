package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务加检标准 Response VO")
@Data
public class QmsDispatchTaskStandardRespVO {

    private Long id;
    private String standardNo;
    private String standardName;
    private String version;
    private String operationCode;
    private String operationName;
    private Long productModelId;
    private String productModelCode;
    private String productModelName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String glueBoardModel;
    private String auditorName;
    private LocalDateTime auditTime;
    private Integer itemCount;
}
