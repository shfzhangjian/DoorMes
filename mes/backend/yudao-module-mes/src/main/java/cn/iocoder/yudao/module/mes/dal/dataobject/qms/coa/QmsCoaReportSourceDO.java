package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_report_source")
@KeySequence("mes_qms_coa_report_source_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaReportSourceDO extends BaseDO {

    @TableId
    private Long id;
    private Long reportId;
    private String coaNo;
    private Integer revisionNo;
    private String productionBatchNo;
    private String customerBatchNo;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private String sourceBatchNo;
    private Long processId;
    private String processCode;
    private String processName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long productModelId;
    private String productModelCode;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    private String standardSnapshotHash;
    private String sourceStatus;
    private String sourceResult;
    private String releaseResult;
    private Long qaInspectorId;
    private String qaInspectorName;
    private LocalDateTime qaTime;
    private Boolean historicalBackfill;
    private Boolean recheckFlag;
    private Long recheckGroupId;
    private Integer recheckRoundNo;
    private Boolean selectedFlag;
    private String effectiveReason;
    private String sourceSnapshotHash;
    private Long tenantId;
}
