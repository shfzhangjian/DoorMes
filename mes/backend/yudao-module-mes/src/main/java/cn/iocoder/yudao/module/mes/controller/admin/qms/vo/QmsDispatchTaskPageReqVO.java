package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 质量任务中心分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDispatchTaskPageReqVO extends PageParam {

    private String taskNo;
    private String checkType;
    private String dispatchStatus;
    private String priority;

    @Schema(description = "任务号、检验单号、批次、物料关键词")
    private String keyword;

    @Schema(description = "范围：ALL/MY_ASSIGNED/MY_CREATED")
    private String scope;
}
