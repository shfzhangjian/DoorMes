package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 质量任务加检标准分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDispatchTaskStandardPageReqVO extends PageParam {

    @NotBlank(message = "检验类型不能为空")
    private String checkType;
    private String operationCode;
    private String operationName;
    private String productModel;
    private String standardName;
}
