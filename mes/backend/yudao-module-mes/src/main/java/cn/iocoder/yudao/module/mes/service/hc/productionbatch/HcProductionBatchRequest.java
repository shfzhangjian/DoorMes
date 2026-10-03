package cn.iocoder.yudao.module.mes.service.hc.productionbatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 生产批号记录/生成请求。
 *
 * <p>先作为跨工序批号服务的统一入参，后续配方开工、湿法开工、磨皮分段、切片明细都走这里。</p>
 */
@Data
public class HcProductionBatchRequest {

    private Long planId;

    private Long planOperationId;

    private String productionBatchNo;

    private String parentProductionBatchNo;

    private Long ruleId;

    private String ruleCode;

    private String sampleCode;

    private String generationTrigger;

    private String generationScope;

    private String generateSource;

    private String batchStage;

    private LocalDate bizDate;

    private LocalDateTime generatedTime;

    private String sourceTable;

    private Long sourceId;

    private String sourceDetailKey;

    private String idempotentKey;

    private Long operatorId;

    private String operatorName;

    private Map<String, Object> attributes;

    private Boolean updatePlanSnapshot;
}
