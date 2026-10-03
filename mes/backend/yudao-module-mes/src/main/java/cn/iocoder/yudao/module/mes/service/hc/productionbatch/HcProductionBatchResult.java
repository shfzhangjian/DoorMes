package cn.iocoder.yudao.module.mes.service.hc.productionbatch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HcProductionBatchResult {

    private Long instanceId;

    private String productionBatchNo;

    private String parentProductionBatchNo;

    private Long ruleId;

    private String ruleCode;

    private String sampleCode;

    private String contextJson;

    private String idempotentKey;

    private Boolean newlyCreated;
}
