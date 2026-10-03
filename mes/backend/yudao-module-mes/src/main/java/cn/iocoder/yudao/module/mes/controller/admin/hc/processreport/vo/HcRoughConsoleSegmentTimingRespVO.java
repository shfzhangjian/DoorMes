package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板母批/分段开工完工时间 Response VO")
@Data
public class HcRoughConsoleSegmentTimingRespVO {

    private Long id;
    private String passType;
    private String segmentMark;
    private String segmentBatchNo;
    private Long firstDetailId;
    private Long secondDetailId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private String startOperatorName;
    private String endOperatorName;

    @Schema(description = "当前分段适用的额定 QTIME 评估结果")
    private HcQtimeEvaluationRespVO qtime;

}
