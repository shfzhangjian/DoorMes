package cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HC 额定 QTIME 配置新增/修改 Request VO")
@Data
public class HcQtimeConfigSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "型号/批号前缀，例如 W26、W33")
    @NotBlank(message = "型号前缀不能为空")
    private String modelPrefix;

    @Schema(description = "配料完工到湿法开工额定间隔，单位分钟")
    @NotNull(message = "配料到湿法 QTIME 不能为空")
    @Min(value = 1, message = "配料到湿法 QTIME 必须大于 0 分钟")
    private Integer formulaToWetMinutes;

    @Schema(description = "湿法完工到磨皮开工额定间隔，单位分钟")
    @NotNull(message = "湿法到磨皮 QTIME 不能为空")
    @Min(value = 1, message = "湿法到磨皮 QTIME 必须大于 0 分钟")
    private Integer wetToGrindingMinutes;

    @Schema(description = "一次磨皮完工到二次磨皮开工额定间隔，单位分钟")
    @NotNull(message = "一次磨皮到二次磨皮 QTIME 不能为空")
    @Min(value = 1, message = "一次磨皮到二次磨皮 QTIME 必须大于 0 分钟")
    private Integer firstGrindingToSecondMinutes;

    @Schema(description = "磨皮完工到粘胶1开工额定间隔，单位分钟")
    @NotNull(message = "磨皮到粘胶1 QTIME 不能为空")
    @Min(value = 1, message = "磨皮到粘胶1 QTIME 必须大于 0 分钟")
    private Integer grindingToAdhesiveMinutes;

    @Schema(description = "粘胶1完工到分切开工额定间隔，单位分钟")
    @NotNull(message = "粘胶1到分切 QTIME 不能为空")
    @Min(value = 1, message = "粘胶1到分切 QTIME 必须大于 0 分钟")
    private Integer adhesiveToSlittingMinutes;

    @Schema(description = "分切完工到压槽开工额定间隔，单位分钟")
    @NotNull(message = "分切到压槽 QTIME 不能为空")
    @Min(value = 1, message = "分切到压槽 QTIME 必须大于 0 分钟")
    private Integer slittingToPressSlotMinutes;

    @Schema(description = "压槽完工到粘胶2开工额定间隔，单位分钟")
    @NotNull(message = "压槽到粘胶2 QTIME 不能为空")
    @Min(value = 1, message = "压槽到粘胶2 QTIME 必须大于 0 分钟")
    private Integer pressSlotToAdhesive2Minutes;

    @Schema(description = "粘胶2完工到裁切开工额定间隔，单位分钟")
    @NotNull(message = "粘胶2到裁切 QTIME 不能为空")
    @Min(value = 1, message = "粘胶2到裁切 QTIME 必须大于 0 分钟")
    private Integer adhesive2ToCutRoundMinutes;

    @Schema(description = "裁切完工到成品检验开工额定间隔，单位分钟")
    @NotNull(message = "裁切到成品检验 QTIME 不能为空")
    @Min(value = 1, message = "裁切到成品检验 QTIME 必须大于 0 分钟")
    private Integer cutRoundToFqcMinutes;

    @Schema(description = "状态：ENABLED/DISABLED")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "备注")
    private String remark;

}
