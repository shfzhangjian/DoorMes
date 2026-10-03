package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 质量任务复检来源分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDispatchTaskSourcePageReqVO extends PageParam {

    @NotBlank(message = "检验类型不能为空")
    private String checkType;
    private String operationName;
    private String batchNo;
}
