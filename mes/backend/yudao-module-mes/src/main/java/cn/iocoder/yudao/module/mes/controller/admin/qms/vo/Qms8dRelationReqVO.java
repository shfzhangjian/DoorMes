package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D来源关系 Request VO")
@Data
public class Qms8dRelationReqVO {

    @Schema(description = "来源类型")
    private String relationType;

    @Schema(description = "来源对象ID")
    private Long relatedObjectId;

    @Schema(description = "来源对象单号")
    private String relatedObjectNo;

    @Schema(description = "来源对象摘要")
    private String relatedObjectName;

    @Schema(description = "来源对象状态")
    private String relationStatus;

    @Schema(description = "是否主来源")
    private Boolean primaryFlag;

    @Schema(description = "备注")
    private String remark;
}
