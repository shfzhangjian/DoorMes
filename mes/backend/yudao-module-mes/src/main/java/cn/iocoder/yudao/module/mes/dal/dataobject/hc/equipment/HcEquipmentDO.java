package cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 设备台账 DO
 */
@TableName("mes_md_equipment")
@KeySequence("mes_md_equipment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcEquipmentDO extends BaseDO {

    /** 设备编码 */
    private String equipmentCode;

    /** 设备名称 */
    private String equipmentName;

    /** 所属工作中心ID */
    private Long workCenterId;

    /** 所属工作中心编码 */
    private String workCenterCode;

    /** 所属工作中心名称 */
    private String workCenterName;

    /** 设备类型 */
    private String equipmentType;

    /** 适用垫型 */
    private String applicablePadType;

    /** 适用垫型名称 */
    private String applicablePadTypeName;

    /** 资产编号 */
    private String assetNo;

    /** 是否启用点检 */
    private Boolean enableQcChecklist;

    /** 是否启用清洁点检 */
    private Boolean enableCleanChecklist;

    /** 状态 */
    private Integer status;

    /** 运行状态 */
    private String workStatus;

    /** 当前计划号 */
    private String currentPlanNo;

    /** 当前工序编号 */
    private String currentOperationCode;

    /** 当前工序名称 */
    private String currentOperationName;

    /** 开工时间 */
    private LocalDateTime currentStartTime;

    /** 完工时间 */
    private LocalDateTime currentEndTime;

    /** 操作人 */
    private String currentOperatorName;

    /** 回写时间 */
    private LocalDateTime currentRecordTime;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
