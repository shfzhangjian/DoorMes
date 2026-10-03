package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - FAI复检申请 Response VO")
@Data
public class QmsFaiRecheckApplyRespVO {

    private Long id;

    private String applyNo;

    private Long sourceFaiId;

    private String sourceFaiNo;

    private String sourceInspectionType;

    private String applyReason;

    private Long applyUserId;

    private String applyUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    private String status;

    private Long auditUserId;

    private String auditUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditTime;

    private String auditOpinion;

    private Long generatedFaiId;

    private String generatedFaiNo;
}
