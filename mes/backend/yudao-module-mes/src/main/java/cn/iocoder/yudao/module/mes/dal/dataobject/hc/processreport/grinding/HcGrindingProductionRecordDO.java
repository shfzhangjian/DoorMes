package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_hc_grinding_production_record")
@KeySequence("mes_hc_grinding_production_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingProductionRecordDO extends BaseDO {
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingConsumptionVO consumption;

    @TableId
    private Long id;
    private LocalDate reportDate;
    /** 设备ID。研发手工记录选择，量产记录从来源报工明细带入。 */
    private Long equipmentId;
    /** 设备编码快照。 */
    private String equipmentCode;
    /** 设备名称快照。 */
    private String equipmentName;
    /** 垫型快照：BLACK_PAD/WHITE_PAD；历史无法识别记录保持为空。 */
    private String padType;
    private String modelCode;
    private String materialCode;
    /** 生产记录角色：FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND。 */
    private String recordRole;
    /** 来源业务类型：FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND。 */
    private String sourceBizType;
    /** 加工母批号快照。 */
    private String motherBatchNo;
    private String batchNo;
    /** 加工单元：P/Q/R/S/NONE；原有一磨可为空。 */
    private String segmentMark;
    /** 当前加工单元物理起米位置。 */
    private BigDecimal startPosition;
    /** 一磨前置分配记录ID；二磨用于精确对应一磨加工单元。 */
    private Long firstAllocationId;
    private BigDecimal inputLength;
    private BigDecimal outputLength;
    private String passType;
    private String passName;
    private BigDecimal sandpaperLife;
    private Integer sandpaperLifeDays;
    private String sandpaperBatchNo;
    /** 研发手工新增时的砂纸物理更换标识；同批号换卷也为 true。 */
    private Boolean sandpaperChanged;
    private String sandpaperReplaceReason;
    private Integer guideClothLife;
    private String guideClothBatchNo;
    /** 研发手工新增时的导布物理更换标识；同批号换卷也为 true。 */
    private Boolean guideClothChanged;
    private String guideClothReplaceReason;
    private String replaceReason;
    private String recorderName;
    private LocalDateTime recordTime;
    private String confirmerName;
    private LocalDateTime confirmTime;
    private String status;
    private String sourceType;
    private Long sourceDetailId;
    /** 同一来源报工的砂纸记录拆分序号：1=更换前/唯一记录，2=更换后新砂纸 */
    private Integer sourceSegmentNo;
    /** 手工研发记录发生砂纸更换时的双记录归属键；为空表示非手工拆分记录。 */
    private String manualSplitGroupNo;
    private String remark;
    private Long tenantId;
}
