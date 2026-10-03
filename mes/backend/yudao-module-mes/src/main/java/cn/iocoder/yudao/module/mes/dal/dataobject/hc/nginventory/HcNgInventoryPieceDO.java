package cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory;

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

/**
 * 分切、压槽不合格品逐片台账。
 *
 * <p>该表记录每一片的来源、NG 原因和当前精确库位；库位主数据使用独立的
 * {@code mes_inv_ng_location}，不复用成品包装库位表。</p>
 */
@TableName("mes_inv_ng_piece")
@KeySequence("mes_inv_ng_piece_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcNgInventoryPieceDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String sourceType;
    private String sourceTable;
    private Long sourceId;
    private Long sourceReportId;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourcePlanOperationId;
    private String processType;
    private String processName;
    private String pieceNo;
    private String sourceBatchNo;
    private String sourceParentBatchNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String modelNo;
    /** 计划分类快照：BLACK_PAD/WHITE_PAD。 */
    private String padType;
    private BigDecimal pieceQty;
    /** 入库原因：NG_REPORT/FREEZE_INSTRUCTION。 */
    private String entryReason;
    /** 来源报工质量判定：OK/NG。 */
    private String qualityResult;
    private String defectSummary;
    private String defectDetailJson;
    private String status;
    private Long stockId;
    private String currentWarehouseCode;
    private String currentWarehouseName;
    private String currentLocationCode;
    private String currentLocationName;
    private String originalWarehouseCode;
    private String originalWarehouseName;
    private String originalLocationCode;
    private String originalLocationName;
    private Long freezeInstructionId;
    private String freezeInstructionNo;
    private LocalDateTime freezeEffectiveTime;
    private Long unfreezeInstructionId;
    private Long shelvedBy;
    private String shelvedByName;
    private LocalDateTime shelvedTime;
    private Long scrappedBy;
    private String scrappedByName;
    private LocalDateTime scrappedTime;
    private String scrapReason;
    /** 工艺流转单累计打印次数。 */
    private Integer printCount;
    /** 最近一次由本机打印服务确认成功的时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;
}
