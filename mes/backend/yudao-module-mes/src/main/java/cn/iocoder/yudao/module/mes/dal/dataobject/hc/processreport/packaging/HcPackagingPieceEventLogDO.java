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

/** 不合格待包装片生命周期事件。 */
@TableName("mes_sfc_packaging_piece_event_log")
@KeySequence("mes_sfc_packaging_piece_event_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackagingPieceEventLogDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String eventNo;
    private String eventType;
    private LocalDateTime eventTime;
    private String sourceType;
    private Long sourceRecordId;
    private String sliceBatchNo;
    private String segmentBatchNo;
    private String planNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String fqcResult;
    private String coaResult;
    private String qualityStatus;
    private String beforeStatus;
    private String afterStatus;
    private Boolean ngRelated;
    private Long finishedStockId;
    private String innerUnitNo;
    private String refDocNo;
    private String operatorName;
    private String remark;
}
