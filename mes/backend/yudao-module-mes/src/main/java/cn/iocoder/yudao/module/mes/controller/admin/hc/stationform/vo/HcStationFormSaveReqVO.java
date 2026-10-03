package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单新增/修改 Request VO")
@Data
public class HcStationFormSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "表单编码")
    @NotBlank(message = "表单编码不能为空")
    private String formCode;

    @Schema(description = "表单名称")
    @NotBlank(message = "表单名称不能为空")
    private String formName;

    @Schema(description = "工序编码")
    @NotBlank(message = "工序编码不能为空")
    private String processCode;

    @Schema(description = "工序名称")
    @NotBlank(message = "工序名称不能为空")
    private String processName;

    @Schema(description = "触发时机编码")
    @NotBlank(message = "触发时机编码不能为空")
    private String triggerTimingCode;

    @Schema(description = "触发时机名称")
    @NotBlank(message = "触发时机名称不能为空")
    private String triggerTimingName;

    @Schema(description = "是否需要确认")
    @NotNull(message = "是否需要确认不能为空")
    private Boolean needConfirm;

    @Schema(description = "排序号")
    @NotNull(message = "排序号不能为空")
    private Integer sortNo;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "Schema JSON")
    private String schemaJson;

    @Schema(description = "预设头信息 JSON")
    private String presetHeaderDataJson;

    @Schema(description = "预设明细")
    private List<HcStationFormItemSaveReqVO> presetItems;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "模板明细")
    private List<HcStationFormItemSaveReqVO> items;
}
