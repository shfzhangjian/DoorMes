package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 录入规则模板 Response VO")
@Data
public class QmsEntryRuleTemplateRespVO {

    private Long id;

    private String templateName;

    private String itemType;

    private String testFrequencyJudgement;

    private String templateParams;

    private String ruleDescription;

    private Integer sampleSize;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
