package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 压槽压辊清洗复位 Request VO")
@Data
public class HcPressSlotConsumableCleanReqVO {

    private Long planOperationId;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;

    @NotBlank(message = "耗材类型不能为空")
    private String consumableType;

    private String materialCode;
    private String materialName;
    private String batchNo;
    private Integer initialUseCount;
    private BigDecimal receiveQuantity;

    private String cleanRemark;
    private Long operatorId;
    private String operatorName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime cleanTime;
}
