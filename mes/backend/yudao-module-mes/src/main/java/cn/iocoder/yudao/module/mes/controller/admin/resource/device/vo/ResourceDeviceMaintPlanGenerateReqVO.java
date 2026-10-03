package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备年度保养计划生成 Request VO")
@Data
public class ResourceDeviceMaintPlanGenerateReqVO {

    @Schema(description = "来源年份")
    private Integer sourceYear;

    @Schema(description = "目标年份")
    private Integer targetYear;

    @Schema(description = "是否覆盖目标年份")
    private Boolean overwrite;

    @Schema(description = "计划ID")
    @NotEmpty(message = "计划ID不能为空", groups = Publish.class)
    private List<Long> ids;

    public interface Publish {
    }

    @NotNull(message = "目标年份不能为空", groups = Copy.class)
    public Integer getTargetYear() {
        return targetYear;
    }

    public interface Copy {
    }

}
