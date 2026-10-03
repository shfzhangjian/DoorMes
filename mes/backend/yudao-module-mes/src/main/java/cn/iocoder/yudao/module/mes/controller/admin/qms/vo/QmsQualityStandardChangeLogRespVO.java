package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 检验标准修改内容日志 Response VO")
@Data
public class QmsQualityStandardChangeLogRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "检验标准ID")
    private Long standardId;

    @Schema(description = "适用环节")
    private String applyType;

    @Schema(description = "修改范围：MAIN主表/ITEM检验项明细")
    private String changeScope;

    @Schema(description = "检验项稳定匹配键")
    private String itemKey;

    @Schema(description = "检验项展示名称")
    private String itemLabel;

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "字段中文名")
    private String fieldLabel;

    @Schema(description = "修改前")
    private String beforeValue;

    @Schema(description = "修改后")
    private String afterValue;

    @Schema(description = "修改人ID")
    private Long operatorId;

    @Schema(description = "修改人")
    private String operatorName;

    @Schema(description = "修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime changeTime;
}
