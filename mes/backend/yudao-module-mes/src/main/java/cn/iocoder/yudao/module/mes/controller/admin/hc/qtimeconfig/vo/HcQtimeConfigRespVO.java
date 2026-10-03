package cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 额定 QTIME 配置 Response VO")
@Data
public class HcQtimeConfigRespVO {

    private Long id;
    private String modelPrefix;
    private Integer formulaToWetMinutes;
    private Integer wetToGrindingMinutes;
    private Integer firstGrindingToSecondMinutes;
    private Integer grindingToAdhesiveMinutes;
    private Integer adhesiveToSlittingMinutes;
    private Integer slittingToPressSlotMinutes;
    private Integer pressSlotToAdhesive2Minutes;
    private Integer adhesive2ToCutRoundMinutes;
    private Integer cutRoundToFqcMinutes;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
