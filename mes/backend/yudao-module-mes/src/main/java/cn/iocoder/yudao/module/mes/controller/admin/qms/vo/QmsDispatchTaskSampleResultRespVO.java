package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务自有样本结果 Response VO")
@Data
public class QmsDispatchTaskSampleResultRespVO {

    private Long id;
    private Long taskItemId;
    private Integer roundNo;
    private Integer sampleSeq;
    private String pieceNo;
    private String inspectionItem;
    private String standardDesc;
    private String unit;
    private String itemType;
    private String inspectionMethod;
    private String testTool;
    private BigDecimal measuredValue;
    private String qualitativeValue;
    private String result;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectionTime;
}
