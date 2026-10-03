package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_report_item")
@KeySequence("mes_qms_coa_report_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaReportItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long reportId;
    private String coaNo;
    private Integer revisionNo;
    private String productionBatchNo;
    private String customerBatchNo;
    private String materialCode;
    private String productModelCode;
    private String metricCode;
    private String itemGroup;
    private String itemNameCn;
    private String itemNameEn;
    private String itemType;
    private String valueSourceType;
    private String sourceStandardApplyType;
    private String valueStrategy;
    private String specSource;
    private String specText;
    private String coaSpecText;
    private BigDecimal targetValue;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String rawValueJson;
    private String sourceValue;
    private String actualValue;
    private String displayValue;
    private String unit;
    private Integer decimalPlaces;
    private String inspectionMethod;
    private String result;
    private String attachmentUrls;
    private String sourceType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private Long sourceOrderItemId;
    private Boolean requiredFlag;
    private Boolean sourceDataCompleteFlag;
    private Boolean allowCorrectionFlag;
    private Boolean correctedFlag;
    private Integer correctionCount;
    private Long lastCorrectionLogId;
    private String lastCorrectionReason;
    private Long sourceProcessId;
    private String sourceProcessCode;
    private String sourceProcessName;
    private Long sourceFaiId;
    private String sourceFaiNo;
    private Long sourceFaiItemId;
    private Long sourceStandardId;
    private String sourceStandardNo;
    private String sourceStandardVersion;
    private String sourceStandardSnapshotHash;
    private Long sourceStandardItemId;
    private LocalDateTime sourceQaTime;
    private Integer sortNo;
    private Long tenantId;
}
