package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件关联对象 Request VO")
@Data
public class QmsExceptionRelationReqVO {

    private Long id;
    private String relationType;
    private Long relatedObjectId;
    private String relatedObjectNo;
    private String relatedObjectName;
    private String relationStatus;
    private Boolean primaryFlag;
    private String remark;
}
