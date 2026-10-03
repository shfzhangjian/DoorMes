package cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_lot_instance")
@KeySequence("mes_lot_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcLotInstanceDO extends BaseDO {

    @TableId
    private Long id;

    private Long ruleId;

    private String ruleCode;

    /**
     * 生成时命中的规则版本。规则后续升级或停用后，仍以此字段解释历史批号。
     */
    private Integer ruleVersion;

    /** 生成时规则的业务对象快照。 */
    private String bizType;

    /** 生成时规则的产品分类快照。 */
    private String productCategoryCode;

    /** 生成时规则的生产类型快照。 */
    private String prodType;

    /** 生成时计划关联的产品型号快照。 */
    private String modelCode;

    private String lotNo;

    private String productionBatchNo;

    private String parentProductionBatchNo;

    private String batchLevel;

    private String parentLotNo;

    private Long planId;

    private String planNo;

    private Long planOperationId;

    private String operationName;

    private String operationCode;

    private Integer operationSeq;

    private Long workCenterId;

    private String workCenterCode;

    private String workCenterName;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private String lineCode;

    private String lineName;

    private String lineShortCode;

    private String batchLineCode;

    private String sampleCode;

    private String sizeCode;

    private String yearCode;

    private String monthCode;

    private Integer annualBatchSeq;

    private Integer lineSeq;

    private String contextJson;

    private String instanceStatus;

    private String generateSource;

    private String batchStage;

    private LocalDate bizDate;

    private String sourceTable;

    private Long sourceId;

    private String sourceDetailKey;

    private String generationTrigger;

    private String generationScope;

    private String idempotentKey;

    private Long operatorId;

    private String operatorName;

    private String attributeJson;

    /** 规则主数据和规则段的可追溯格式快照 JSON。 */
    private String ruleFormatSnapshotJson;

    /** 本次生成的各规则段实际取值 JSON。 */
    private String segmentValuesJson;

    private java.time.LocalDateTime generatedTime;

    private Long tenantId;
}
