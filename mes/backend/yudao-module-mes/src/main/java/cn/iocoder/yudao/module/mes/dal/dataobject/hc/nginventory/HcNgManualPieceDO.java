package cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory;

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

/** 分切压槽不合格品历史补录来源记录。 */
@TableName("mes_inv_ng_manual_piece")
@KeySequence("mes_inv_ng_manual_piece_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcNgManualPieceDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String segmentBatchNo;
    private String pieceNo;
    private String processType;
    private String processName;
    private String sourceBatchNo;
    private String materialCode;
    private String modelNo;
    private String padType;
    /** NORMAL：普通不合格品；FREEZE：冻结品。 */
    private String storageTarget;
    private String defectSummary;
    private String recordStatus;
    private Long ngPieceId;
    private String inputMode;
    private String backfillReason;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String deleteReason;
    private String deleteUserName;
    private LocalDateTime deleteTime;
    private String unfreezeReason;
    private Long unfrozenBy;
    private String unfrozenByName;
    private LocalDateTime unfrozenTime;
    private String remark;
}
