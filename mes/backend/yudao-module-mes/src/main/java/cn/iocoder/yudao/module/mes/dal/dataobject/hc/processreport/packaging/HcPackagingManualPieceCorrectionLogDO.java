package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

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
 * 历史导入成品片数据更正审计日志。
 *
 * <p>更正只更新实时来源、包装和库存快照；该表保存更正前后值，不能用来覆盖库存流水事实。</p>
 */
@TableName("mes_sfc_packaging_manual_piece_correction_log")
@KeySequence("mes_sfc_packaging_manual_piece_correction_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackagingManualPieceCorrectionLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long manualPieceId;
    private Long finishedStockId;
    private Long innerUnitId;
    private String innerUnitNo;
    private String beforeDataJson;
    private String afterDataJson;
    private String correctionReason;
    private String operatorName;
    private LocalDateTime correctionTime;
    private Boolean labelReprintRequired;
    private Long tenantId;
}
