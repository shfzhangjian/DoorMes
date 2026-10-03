package cn.iocoder.yudao.module.mes.dal.dataobject.hc.wetproductionrecord;

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

@TableName("mes_hc_wet_production_record")
@KeySequence("mes_hc_wet_production_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcWetProductionRecordDO extends BaseDO {

    @TableId
    private Long id;

    private LocalDate recordDate;

    private String modelCode;

    private String materialCode;

    private String batchNo;

    private BigDecimal inputKg;

    private BigDecimal outputMeter;

    private String petModel;

    private String petBatchNo;

    private String guideClothBatchNo;

    private Integer guideClothUseCount;

    private String guideClothChanged;

    private String changeDesc;

    private String recorderName;

    private LocalDateTime recordTime;

    private String confirmerName;

    private LocalDateTime confirmTime;

    private String status;

    private String remark;

    private Long tenantId;
}
