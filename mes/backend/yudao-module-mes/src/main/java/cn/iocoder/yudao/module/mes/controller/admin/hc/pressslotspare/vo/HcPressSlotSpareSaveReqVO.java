package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

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

@Schema(description = "管理后台 - 压槽备件保存 Request VO")
@Data
public class HcPressSlotSpareSaveReqVO {

    private Long id;

    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;

    @NotBlank(message = "备件类型不能为空")
    private String spareType;

    private String materialCode;
    private String materialName;

    @NotBlank(message = "批号/编码不能为空")
    private String batchNo;

    private BigDecimal onlineQuantity;
    private BigDecimal availableQuantity;
    private Integer useCount;
    private Integer limitCount;
    private Integer limitDays;
    private String status;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime lastReplaceTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime lastCleanTime;

    private String lastCleanRemark;
    private String remark;
}
