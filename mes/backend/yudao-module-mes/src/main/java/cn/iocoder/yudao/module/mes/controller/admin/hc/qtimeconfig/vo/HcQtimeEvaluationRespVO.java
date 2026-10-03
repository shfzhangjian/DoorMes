package cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC QTIME 判定 Response VO")
@Data
public class HcQtimeEvaluationRespVO {

    private String ruleType;
    private String modelPrefix;
    private Integer standardMinutes;
    private Integer elapsedMinutes;
    private Boolean targetStarted;
    private Boolean timeout;
    private String status;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime targetStartTime;

}
