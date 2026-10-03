package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 边库耗材消耗明细 Response VO")
@Data
public class HcToolingConsumableConsumeRespVO {

    private Long id;
    private Long ledgerId;
    private String consumableType;
    private String consumableTypeName;
    private String processCode;
    private String processName;
    private String model;
    private String batchNo;
    private BigDecimal consumeQty;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private String uom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime consumeTime;

    private String planNo;
    private String productionBatchNo;
    private String productModelCode;
    private String productMaterialCode;
    private String productBatchNo;
    private BigDecimal productInputQty;
    private BigDecimal productOutputQty;
    private Long glueBoardStockId;
    private Long glueBoardUsageId;
    private Long planOperationId;
    private String consumeSource;
    private String consumeType;
    private String remark;
    private String creator;
    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
