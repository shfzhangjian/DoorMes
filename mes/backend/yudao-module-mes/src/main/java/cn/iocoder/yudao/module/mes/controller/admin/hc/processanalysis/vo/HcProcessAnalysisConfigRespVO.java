package cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 计划排程统计分析方案 Response VO")
@Data
public class HcProcessAnalysisConfigRespVO {

    @Schema(description = "方案ID")
    private Long id;

    @Schema(description = "方案名称")
    private String configName;

    @Schema(description = "范围：PRIVATE个人、SHARED租户共享")
    private String scopeType;

    @Schema(description = "方案所有人")
    private Long ownerUserId;

    @Schema(description = "当前用户是否可编辑")
    private Boolean editable;

    @Schema(description = "是否默认方案")
    private Boolean defaultFlag;

    @Schema(description = "配置JSON版本")
    private Integer configVersion;

    @Schema(description = "分析配置JSON")
    private String configJson;

    @Schema(description = "方案说明")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
