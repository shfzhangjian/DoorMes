package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_inner_pack_unit_item")
@KeySequence("mes_sfc_inner_pack_unit_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcInnerPackUnitItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long innerUnitId;
    private String innerUnitNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String sourceType;
    private Long sourceCutRoundReportId;
    private Long sourceManualPieceId;
    private Long sourceStockId;
    private Long sourcePlanLockId;
    private BigDecimal sourceConsumeQty;
    private String sourceConsumeTxnNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceConsumeTime;
    private String sliceBatchNo;
    private String productionBatchNo;
    private String qualityStatus;
    private String scanUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;
    private Long tenantId;
}
