package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 边库耗材领用台账用完标记 Request VO")
@Data
public class HcToolingConsumableLedgerMarkUsedUpReqVO {

    @NotNull(message = "领用台账不能为空")
    private Long id;

    @DecimalMin(value = "0.000", message = "用完余料量不能为负数")
    private BigDecimal usedUpRemainQty;

    @Schema(description = "粘胶胶板完成时填写的当前剩余量，直接更新关联库存；不新增台账存储字段")
    @DecimalMin(value = "0.000", message = "当前剩余量不能为负数")
    private BigDecimal balanceQty;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate usedUpActualDate;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String usedUpRemark;

    private Long usedUpAuthUserId;

    @NotBlank(message = "认证人不能为空")
    @Size(max = 64, message = "认证人不能超过64个字符")
    private String usedUpAuthUserName;
}
