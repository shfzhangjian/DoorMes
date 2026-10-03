package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR评审会签配置 Response VO")
@Data
public class QmsNcReviewConfigRespVO {

    private Long id;
    private String unitCode;
    private String unitName;
    private Long deptId;
    private String deptName;
    private List<Long> handlerUserIds;
    private List<String> handlerUserNames;
    private Integer status;
    private Integer sort;
    private String remark;
    private String opinionTemplateJson;
    private LocalDateTime createTime;
}
