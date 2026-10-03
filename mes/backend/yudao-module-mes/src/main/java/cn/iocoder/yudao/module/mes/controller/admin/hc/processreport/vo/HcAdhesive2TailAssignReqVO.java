package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2报工片号尾号批量选择 Request VO")
@Data
public class HcAdhesive2TailAssignReqVO {

    @Schema(description = "待选择尾号的粘胶2报工记录", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择至少一条粘胶2报工记录")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "粘胶2报工记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "粘胶2报工记录不能为空")
        private Long id;

        @Schema(description = "实际尺寸，仅允许 775mm 或 740mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "775mm")
        @NotBlank(message = "请选择实际尺寸")
        private String actualSizeRule;
    }
}
