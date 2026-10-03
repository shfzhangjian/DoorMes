package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 良品率分析物化结果。
 *
 * <p>同一业务来源键始终保存业务表的最新状态，并按实际报工日期统计。</p>
 */
@TableName("mes_qms_yield_daily_fact")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsYieldDailyFactDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;
    private String processCode;
    private String processName;
    private String sourceTable;
    private Long sourceId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String motherRollNo;
    private String segmentNo;
    private String pieceNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private BigDecimal inputQty;
    private BigDecimal outputGoodQty;
    private BigDecimal outputNgQty;
    private String selfCheck;
    private String defectCode;
    private String visualResultJson;
    private String extraJson;
    private String submissionResult;
    private String reportStatus;
    private LocalDateTime confirmTime;
    private Long inspectionId;
    private String inspectionNo;
    private String inspectionSourceType;
    private String sourceContentHash;
    private String pivotKey;
    private String pivotKeyHash;
    private String pivotJson;
    private String syncBatchNo;
    private LocalDateTime syncTime;
    private Long tenantId;
}
