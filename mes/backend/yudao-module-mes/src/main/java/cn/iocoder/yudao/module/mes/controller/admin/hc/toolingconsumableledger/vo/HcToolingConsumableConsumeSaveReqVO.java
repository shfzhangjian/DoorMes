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

@Schema(description = "管理后台 - 边库耗材消耗明细新增/修改 Request VO")
@Data
public class HcToolingConsumableConsumeSaveReqVO {

    private Long id;

    @NotNull(message = "领用台账不能为空")
    private Long ledgerId;

    @NotBlank(message = "耗材种类不能为空")
    private String consumableType;

    private String consumableTypeName;

    @NotBlank(message = "工序不能为空")
    private String processCode;

    private String processName;

    private String model;

    @NotBlank(message = "耗材批次号不能为空")
    private String batchNo;

    @NotNull(message = "消耗量不能为空")
    private BigDecimal consumeQty;

    @NotNull(message = "消耗时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime consumeTime;

    private String planNo;
    private String productionBatchNo;

    @Schema(description = "研发产品型号")
    private String productModelCode;

    @Schema(description = "研发产品料号")
    private String productMaterialCode;

    @Schema(description = "研发产品批号")
    private String productBatchNo;

    @Schema(description = "研发产品投入数量；粘胶1单位为m，粘胶2单位为pcs")
    private BigDecimal productInputQty;

    @Schema(description = "研发产品产出数量；粘胶1单位为m，粘胶2单位为pcs")
    private BigDecimal productOutputQty;

    private Long glueBoardUsageId;
    private String remark;
}
