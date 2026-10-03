package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slittingpress;

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

@TableName("mes_hc_slitting_press_production_record")
@KeySequence("mes_hc_slitting_press_production_record_seq")
@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
@Builder @NoArgsConstructor @AllArgsConstructor
public class HcSlittingPressProductionRecordDO extends BaseDO {
    @TableId private Long id;
    private LocalDate reportDate;
    private String modelCode;
    private String materialCode;
    private String batchNo;
    private BigDecimal slittingInputM;
    private Integer slittingOutputPcs;
    private BigDecimal pressSlotInputPcs;
    private BigDecimal pressSlotOutputPcs;
    private Integer rollerCleanAccumulatedPcs;
    private Integer rollerCleanUseDays;
    private Integer bearingReplaceAccumulatedPcs;
    private Integer bearingReplaceUseDays;
    private String recorderName;
    private LocalDateTime recordTime;
    private String confirmerName;
    private LocalDateTime confirmTime;
    private String status;
    private String sourceType;
    private String remark;
    private Long tenantId;
}
