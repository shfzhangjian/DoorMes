// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qms/QmsTaskDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量检验任务 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_qms_task")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsTaskDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 检验单号
     */
    private String taskNo;

    /**
     * 检验类型(IQC/IPQC/FQC)
     */
    private String checkType;

    /**
     * 来源类型(INBOUND/WORK_ORDER)
     */
    private String sourceType;

    /**
     * 来源单据ID
     */
    private Long sourceId;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 冗余:物料编码
     */
    private String materialCode;

    /**
     * 冗余:物料名称
     */
    private String materialName;

    /**
     * 冗余:物料规格
     */
    private String materialSpec;

    /**
     * 冗余:单位
     */
    private String unit;

    /**
     * 批次号
     */
    private String lotNo;

    /**
     * 报检数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal checkQty;

    /**
     * 抽样/损耗数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal sampleQty;

    /**
     * 检验结果 (PENDING, QUALIFIED, REJECTED 等)
     */
    private String result;

    /**
     * 检验员
     */
    private String inspector;

    /**
     * 检验时间
     */
    private LocalDateTime checkTime;

    /**
     * 备注
     */
    private String remark;

}
