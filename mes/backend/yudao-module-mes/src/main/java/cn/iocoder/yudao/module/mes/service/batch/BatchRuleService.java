package cn.iocoder.yudao.module.mes.service.batch;

/**
 * MES 批次号生成规则服务
 * 负责解析规则配置，生成全局唯一的业务流水号
 */
public interface BatchRuleService {

    /**
     * [独立生成] 根据产品ID生成新批次号
     * 适用场景：原材料入库、压铸首工序、MTS生产
     *
     * @param productId 产品ID
     * @return 格式化后的批次号 (e.g., KNK-20260217-001)
     */
    String generateBatchNo(Long productId);

    /**
     * [继承生成] 根据父批次号生成子批次号
     * 适用场景：分切、分箱、返工拆单
     *
     * @param productId 产品ID (用于查找规则配置)
     * @param parentBatchNo 父批次号 (e.g., COAT-20260217-088)
     * @return 继承后的批次号 (e.g., COAT-20260217-088-01)
     */
    String generateChildBatchNo(Long productId, String parentBatchNo);
}
