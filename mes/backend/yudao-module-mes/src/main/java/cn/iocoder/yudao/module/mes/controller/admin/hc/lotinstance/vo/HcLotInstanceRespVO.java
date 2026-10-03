package cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 批次实例台账 Response VO")
@Data
public class HcLotInstanceRespVO {

    private Long id;
    private String lotNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String parentLotNo;
    private String batchLevel;
    private String instanceStatus;
    private String generateSource;
    private String batchStage;

    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private Integer ruleVersion;
    private String bizType;
    private String productCategoryCode;
    private String prodType;
    private String modelCode;
    private String ruleFormatSummary;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String materialCode;
    private String materialName;
    private String lineCode;
    private String lineName;
    private String batchLineCode;
    private String yearCode;
    private String monthCode;
    private Integer annualBatchSeq;
    private String generationTrigger;
    private String generationScope;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate bizDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "批号规则格式快照，已转换为可读对象")
    private Map<String, Object> ruleFormatSnapshot;

    @Schema(description = "批号各规则段实际取值")
    private Map<String, Object> segmentValues;

    @Schema(description = "生成上下文")
    private Map<String, Object> generationContext;

    @Schema(description = "生产属性快照")
    private Map<String, Object> productionAttributes;
}
