package cn.iocoder.yudao.module.mes.service.hc.productionbatch;

public interface HcProductionBatchService {

    /** 下发配料起始计划时占号，仅保存计划预约，不创建开工记录或批次实例。 */
    void reserveRootBatchOnPlanRelease(Long planId);

    /**
     * 配方开工时生成计划根生产批号。
     */
    HcProductionBatchResult generateRootBatchOnFormulaStart(HcProductionBatchRequest request);

    /**
     * 记录一个已生成或外部传入的生产批号实例，并可按需回写计划/工序快照。
     */
    HcProductionBatchResult recordKnownBatch(HcProductionBatchRequest request);
}
