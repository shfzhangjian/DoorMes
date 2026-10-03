package cn.iocoder.yudao.module.mes.service.hc.processparam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessParamRecordUpsertReq {

    private Long tenantId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String batchNo;
    private String processCode;
    private String processName;
    private String paramCode;
    private String paramName;
    private String paramValue;
    private BigDecimal paramValueNum;
    private String uom;
    private LocalDateTime recordTime;
    private Long recorderId;
    private String recorderName;
    private String sourceType;
    private String sourceTable;
    private Long sourceId;
    private Long sourceDetailId;
    private String sourceFormCode;
    private String sourceFormName;
    private String remark;

}
