package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_md_press_slot_spare")
@KeySequence("mes_md_press_slot_spare_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPressSlotSpareDO extends BaseDO {

    @TableId
    private Long id;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String spareType;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private BigDecimal onlineQuantity;
    private BigDecimal availableQuantity;
    private LocalDateTime lastReplaceTime;
    private String lastReplacePlanNo;
    private String lastReplaceReason;
    private LocalDateTime lastCleanTime;
    private String lastCleanRemark;
    private Integer useCount;
    private BigDecimal usedLength;
    private Integer limitCount;
    private Integer limitDays;
    private Integer warningFlag;
    private String status;
    private Long lastOperatorId;
    private String lastOperatorName;
    private LocalDateTime lastEventTime;
    private String remark;
    private Long tenantId;
}
