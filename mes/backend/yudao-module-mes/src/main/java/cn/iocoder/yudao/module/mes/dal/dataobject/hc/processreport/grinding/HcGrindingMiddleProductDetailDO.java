package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_grinding_middle_product_detail")
@KeySequence("mes_sfc_grinding_middle_product_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingMiddleProductDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Integer seq;
    private BigDecimal lengthMeter;
    private String innerThickness;
    private String outerThickness;
    private LocalDate recordDate;
    private String modelCode;
    private String materialCode;
    private String batchNo;
    private BigDecimal inputLength;
    private BigDecimal outputLength;
    private String grindingPass;
    private BigDecimal sandpaperLife;
    private String sandpaperBatchNo;
    private Integer guideClothLife;
    private String guideClothBatchNo;
    private String replaceReason;
    private String recorderName;
    private String remark;
    private Long tenantId;
}
