package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

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

@TableName("mes_inv_package_aux_consume_record")
@KeySequence("mes_inv_package_aux_consume_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackageAuxConsumeRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long stockId;
    private String auxCategory;
    private String auxCategoryName;
    private String materialCode;
    private String materialName;
    private String auxSpec;
    private String batchNo;
    private String bizType;
    private String bizNo;
    private BigDecimal consumeQty;
    private BigDecimal beforeAvailableQty;
    private BigDecimal afterAvailableQty;
    private String consumeStatus;
    private LocalDate recordDate;
    private Long recorderId;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
