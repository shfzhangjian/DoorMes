package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

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

@TableName("mes_sfc_grinding_middle_product_record")
@KeySequence("mes_sfc_grinding_middle_product_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingMiddleProductRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String formCode;
    private String formName;
    private String passType;
    private String passName;
    private String bizType;
    private Long bizId;
    private String segmentMark;
    private String segmentName;
    private BigDecimal segmentTotalLength;
    private Integer generatedLength;
    private LocalDate recordDate;
    private String docStatus;
    private String resultStatus;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String motherModelCode;
    private String motherModelName;
    private String materialCode;
    private String materialName;
    private String motherBatchNo;
    private String productionBatchNo;
    private BigDecimal widthMm;
    private Long recorderId;
    private String recorderName;
    private LocalDateTime recorderTime;
    private Long confirmerId;
    private String confirmerName;
    private LocalDateTime confirmerTime;
    private String headerDataJson;
    private String remark;
    private Long tenantId;
}
