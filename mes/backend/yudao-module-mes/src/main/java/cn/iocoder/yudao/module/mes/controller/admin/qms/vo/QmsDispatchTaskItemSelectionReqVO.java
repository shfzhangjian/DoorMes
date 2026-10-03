package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务检验项目选择范围 Request VO")
@Data
public class QmsDispatchTaskItemSelectionReqVO {

    @NotNull(message = "检验项目不能为空")
    private Long itemId;

    /** ITEM 表示整项；POSITION 表示位置；PIECE 表示具体片号/样本，值复用 positions。 */
    private String scopeType;

    private List<String> positions;
}
