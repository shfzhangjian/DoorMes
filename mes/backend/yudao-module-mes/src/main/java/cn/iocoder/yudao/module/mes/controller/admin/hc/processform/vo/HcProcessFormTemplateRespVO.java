package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 各工序表单模板 Response VO")
@Data
public class HcProcessFormTemplateRespVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String processCode;
    private String processName;
    private String modelScope;
    private String modelCode;
    private String modelName;
    private String formType;
    private String formTypeName;
    private Long currentVersionId;
    private String status;
    private Boolean needConfirm;
    private Integer sortNo;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
