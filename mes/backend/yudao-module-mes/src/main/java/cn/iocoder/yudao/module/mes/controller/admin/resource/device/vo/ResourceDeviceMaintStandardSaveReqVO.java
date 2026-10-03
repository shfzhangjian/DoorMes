package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备保养标准新增/修改 Request VO")
@Data
public class ResourceDeviceMaintStandardSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "标准编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标准编号不能为空")
    private String code;

    @Schema(description = "标准名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标准名称不能为空")
    private String name;

    @Schema(description = "适用分类ID")
    private Long categoryId;

    @Schema(description = "适用设备类型")
    private String deviceType;

    @Schema(description = "执行频率")
    private String frequency;

    @Schema(description = "保养类型/等级")
    private String maintType;

    @Schema(description = "预计工时(分钟)")
    private Integer estimatedMinutes;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁")
    private Integer version;

    @Valid
    private List<StandardItem> items;

    @Data
    public static class StandardItem {
        private Long id;
        private Long standardId;
        private String itemGroup;
        private String itemName;
        private String method;
        private String requirement;
        private String frequency;
        private String tool;
        private String resultType;
        private Boolean requiredFlag;
        private Integer sort;
    }

}
