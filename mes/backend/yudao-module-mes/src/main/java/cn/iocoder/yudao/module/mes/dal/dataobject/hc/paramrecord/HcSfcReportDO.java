package cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord;

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

/**
 * 报工主表 DO
 */
@TableName("mes_sfc_report")
@KeySequence("mes_sfc_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcSfcReportDO extends BaseDO {

    private String reportNo;

    private Long taskId;

    private String taskNo;

    private Long workOrderId;

    private String workOrderNo;

    private String operationCode;

    private String reportType;

    private BigDecimal goodQty;

    private BigDecimal scrapQty;

    private BigDecimal sampleQty;

    private LocalDateTime reportTime;

    private Long teamId;

    private Long equipmentId;

    @TableId
    private Long id;

    private Long tenantId;
}
