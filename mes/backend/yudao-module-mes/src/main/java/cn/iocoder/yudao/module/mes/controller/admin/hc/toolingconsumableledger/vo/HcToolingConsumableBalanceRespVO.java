package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 边库耗材余额 Response VO")
@Data
public class HcToolingConsumableBalanceRespVO {

    private Long ledgerId;
    private String consumableType;
    private String consumableTypeName;
    private String processCode;
    private String processName;
    private String model;
    private String batchNo;
    private String erpMaterialCode;
    private BigDecimal receiveQty;
    private BigDecimal consumedQty;
    private BigDecimal balanceQty;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private String uom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receiveTime;

    private Long receiverId;
    private String receiverName;
    private String remark;
}
