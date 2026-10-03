package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 边库耗材领用台账新增/修改 Request VO")
@Data
public class HcToolingConsumableLedgerSaveReqVO {

    private Long id;

    @NotBlank(message = "耗材种类不能为空")
    private String consumableType;

    private String consumableTypeName;

    @NotBlank(message = "工序不能为空")
    private String processCode;

    private String processName;

    private String model;

    @NotBlank(message = "耗材批次号不能为空")
    private String batchNo;

    private String erpMaterialCode;

    @NotNull(message = "领用量不能为空")
    private BigDecimal receiveQty;

    @NotNull(message = "计量单位不能为空")
    private Long uomId;

    private String uomCode;

    private String uomName;

    private String uom;

    @NotNull(message = "领用时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime receiveTime;

    private Long receiverId;

    @NotBlank(message = "领用人不能为空")
    private String receiverName;

    private String remark;
}
