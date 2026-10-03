package cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 工作中心 DO
 */
@TableName("mes_md_work_center")
@KeySequence("mes_md_work_center_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcWorkCenterDO extends BaseDO {

    /** 工作中心编码 */
    private String wcCode;

    /** 工作中心名称 */
    private String wcName;

    /** 工序名称，兼容历史字段 process_stage */
    private String processStage;

    /** 标准工序ID */
    private Long processId;

    /** 标准工序编码 */
    private String processCode;

    /** 标准工序名称 */
    private String processName;

    /** 产线编码 */
    private String lineCode;

    /** 产线名称 */
    private String lineName;

    /** 产线短码 */
    private String lineShortCode;

    /** 批次号产线码 */
    private String batchLineCode;

    /** 产线排序 */
    private Integer lineSort;

    /** 绑定工位 IP，多个用逗号、分号或换行分隔 */
    private String terminalIps;

    /** 标准小时产能 */
    private BigDecimal capacityPerHour;

    /** 产能单位 */
    private String capacityUom;

    /** 默认班制 */
    private String defaultShiftMode;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
