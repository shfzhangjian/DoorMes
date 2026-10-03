package cn.iocoder.yudao.module.mes.controller.admin.process.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - MES标准工序新增/修改 Request VO")
@Data
public class ProcessSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "23639")
    private Long id;

    @Schema(description = "工序编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工序编码不能为空")
    private String code;

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "工序名称不能为空")
    private String name;

    @Schema(description = "默认车间ID", example = "10939")
    private Long workshopId;

    @Schema(description = "车间编码")
    private String workshopCode;

    @Schema(description = "车间名称", example = "李四")
    private String workshopName;

    @Schema(description = "工序类型", example = "2")
    private String processType;

    // ========== 🚨 补全字段 START ==========
    @Schema(description = "是否绑定工位", example = "true")
    private Boolean bindStation;
    // ========== 🚨 补全字段 END ============

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "状态", example = "2")
    private Integer status;

    @Schema(description = "可用工位/设备列表")
    private List<ProcessStation> stations;

    @Data
    @Schema(description = "管理后台 - 工序可用工位 Request VO")
    public static class ProcessStation {
        @Schema(description = "子表ID (更新时必填)")
        private Long id;

        @Schema(description = "工位/设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "工位ID不能为空")
        private Long stationId;

        @Schema(description = "工位编码")
        private String stationCode;
        @Schema(description = "工位名称")
        private String stationName;

        @Schema(description = "是否默认工位")
        private Boolean defaultStatus;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "备注")
        private String remark;
    }
}
