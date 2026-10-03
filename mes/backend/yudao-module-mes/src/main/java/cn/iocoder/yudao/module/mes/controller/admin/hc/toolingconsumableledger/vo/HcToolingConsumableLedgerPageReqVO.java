package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 边库耗材领用台账分页 Request VO")
@Data
public class HcToolingConsumableLedgerPageReqVO extends PageParam {

    @Schema(description = "耗材种类")
    private String consumableType;

    @Schema(description = "工序")
    private String processCode;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "耗材批次号")
    private String batchNo;

    @Schema(description = "ERP料号")
    private String erpMaterialCode;

    @Schema(description = "领用人")
    private String receiverName;

    @Schema(description = "使用状态")
    private String usageStatus;

    @Schema(description = "领用开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTimeStart;

    @Schema(description = "领用结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTimeEnd;

    @Schema(description = "是否仅查询剩余量大于0")
    private Boolean onlyPositiveBalance;
}
