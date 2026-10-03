package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 保存质量任务检验结果 Request VO")
@Data
public class QmsDispatchTaskResultSaveReqVO {

    @NotNull(message = "质量任务不能为空")
    private Long id;

    @Valid
    @NotEmpty(message = "检验结果不能为空")
    private List<QmsDispatchTaskResultItemReqVO> items;
}
