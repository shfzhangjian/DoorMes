package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单 Response VO")
@Data
public class HcStationFormRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "触发时机编码")
    private String triggerTimingCode;

    @Schema(description = "触发时机名称")
    private String triggerTimingName;

    @Schema(description = "是否需要确认")
    private Boolean needConfirm;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "Schema JSON")
    private String schemaJson;

    @Schema(description = "预设头信息 JSON")
    private String presetHeaderDataJson;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
