package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IQC进料过期留样销毁 Request VO")
@Data
public class QmsIqcRetentionDestroyReqVO {

    @Schema(description = "IQC进料检验单ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择要销毁的留样记录")
    private List<Long> ids;

    @Schema(description = "销毁备注")
    private String remark;

}
