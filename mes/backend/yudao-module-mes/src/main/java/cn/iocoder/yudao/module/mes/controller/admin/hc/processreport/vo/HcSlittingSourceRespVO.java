package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 分切来源粘胶分段 Response VO")
@Data
@Builder
public class HcSlittingSourceRespVO {

    private Long adhesiveReportId;
    private String adhesivePlanNo;
    private Long adhesivePlanOperationId;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String segmentBatchNo;
    private String segmentMark;
    private String productionBatchNo;
    private String adhesiveProductionBatchNo;
    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String status;
    private BigDecimal sourceStartPosition;
    private BigDecimal sourceEndPosition;
    private BigDecimal outputLength;
    private BigDecimal motherWetOutputLength;
    private BigDecimal availableSourceLength;
    private Integer sliceCount;
    private Integer confirmedSliceCount;
    private HcQtimeEvaluationRespVO qtime;
}
