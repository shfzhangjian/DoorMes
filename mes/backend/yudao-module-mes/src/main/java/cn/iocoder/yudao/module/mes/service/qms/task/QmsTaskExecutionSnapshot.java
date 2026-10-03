package cn.iocoder.yudao.module.mes.service.qms.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 原检验记录的只读统一快照，任务中心不反写原记录。
 */
@Data
@Builder
public class QmsTaskExecutionSnapshot {

    private String checkType;
    private String objectType;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long executionId;
    private String executionNo;
    private String executionRoute;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialSpec;
    private String productModel;
    private String operationCode;
    private String operationName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private String lotNo;
    private BigDecimal checkQty;
    private String unit;
    private Long standardId;
    private String standardNo;
    private String standardName;
    private String standardVersion;
    private String status;
    private String judgment;
    private String inspectorName;
    private LocalDateTime inspectionTime;
    private Integer itemCount;
    private Integer abnormalCount;
}
